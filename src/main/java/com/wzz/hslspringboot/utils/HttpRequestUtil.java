package com.wzz.hslspringboot.utils;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONException;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HttpRequestUtil {

    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    private static final Logger log = LogManager.getLogger(HttpRequestUtil.class);

    private String baseUrl = "https://hsn.sinograin.com.cn";

    // JDK 8u111版本后，HTTPS的代理隧道认证默认被禁用
    static {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "false");
    }


    /**
     * 发送GET请求以获取图片验证码等二进制数据
     * 此方法不使用 executeRequest，因为它专门设计用于直接返回响应体的原始字节数组，
     * 适用于图片、文件下载等场景。
     *
     * @param urlPath   请求的相对路径 (e.g., /slyyServlet/service/...)
     * @param headers   请求头对象
     * @param params    URL参数
     * @return 响应体的字节数组 (byte[])，如果请求失败或响应为空则返回 null
     */
    public byte[] getCaptchaImage(String urlPath, RequestHeaderUtil headers, Map<String, Object> params) {
        String fullUrl = baseUrl + urlPath;
        HttpRequest request = HttpRequest.get(fullUrl);

        if (MapUtil.isNotEmpty(params)) {
            request.form(params);
        }

        // 应用请求头
        if (headers != null && headers.getHeader() != null) {
            request.addHeaders(headers.getHeader());
        }

        // 设置超时
        request.setConnectionTimeout(3000); // 连接超时
        request.setReadTimeout(5000);       // 读取超时

        HttpResponse response = null;
        try {
            log.info("请求图片验证码 to URL: [{}], Method: [{}]", request.getUrl(), request.getMethod());
            response = request.execute();

            if (response.isOk()) {
                // 关键：验证码请求通常会设置session，所以必须处理Cookie
                headers.setCookie(response);
                set(headers);

                // 核心区别：使用 bodyBytes() 直接获取原始字节数据
                byte[] imageBytes = response.bodyBytes();
                log.info("成功获取图片 to [{}], HTTP 状态码: {}, Response Body Length: {}", request.getUrl(), response.getStatus(), imageBytes.length);
                return imageBytes;
            } else {
                log.error("获取图片失败 to [{}], HTTP Status: {}, Body: {}", request.getUrl(), response.getStatus(), response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("获取图片网络异常 to [{}]: {}", request.getUrl(), e.getMessage(), e);
            return null;
        } finally {
            log.info("图片请求结束.");
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * 发送GET请求
     * @param urlPath   请求的相对路径 (e.g., /slyyServlet/service/...)
     * @param headers   请求头对象
     * @param params    URL参数
     * @return 包含状态和数据的JSONObject
     */
    public JSONObject get(String urlPath, RequestHeaderUtil headers, Map<String, Object> params) {
        String fullUrl = baseUrl + urlPath;
        HttpRequest request = HttpRequest.get(fullUrl);
        if (MapUtil.isNotEmpty(params)) {
            request.form(params);
        }
        return executeRequest(request, headers);
    }

    /**
     * 发送POST表单请求
     * @param urlPath   请求的相对路径
     * @param headers   请求头对象
     * @param formData  表单数据
     * @return 包含状态和数据的JSONObject
     */
    public JSONObject postForm(String urlPath, RequestHeaderUtil headers, Map<String, Object> formData) {
        String fullUrl = baseUrl + urlPath;
        HttpRequest request = HttpRequest.post(fullUrl);
        if (MapUtil.isNotEmpty(formData)) {
            request.form(formData);
        } else {
            // 确保即使 formData 为 null，也发送 Content-Length: 0 的请求，以匹配 Postman 的行为
            request.header("Content-Length", "0");
        }
        log.info("Response formData: [{}]", formData);
        return executeRequest(request, headers);
    }
    public JSONObject postForm2(String urlPath, RequestHeaderUtil headers, Map<String, Object> formData) {
        String fullUrl = baseUrl + urlPath;
        HttpRequest request = HttpRequest.post(fullUrl);
        String body=toFormString(formData);

        JSONObject logJson = new JSONObject();
        logJson.put("data", formData);
        log.info("请求体: {}", logJson.toJSONString());
        log.info("请求头[{}]", headers.getHeader());
        System.out.println("请求体"+body);
        if (MapUtil.isNotEmpty(formData)) {
            request.body(body);
            request.header("Content-Type", "application/x-www-form-urlencoded");
        } else {
            // 确保即使 formData 为 null，也发送 Content-Length: 0 的请求，以匹配 Postman 的行为
            request.header("Content-Length", "0");
            request.header("Content-Type", "application/x-www-form-urlencoded");
        }
        return executeRequest(request, headers);
    }

    /**
     * 发送POST JSON请求
     * @param urlPath   请求的相对路径
     * @param headers   请求头对象
     * @param jsonBody  JSON格式的请求体
     * @return 包含状态和数据的JSONObject
     */
    public JSONObject postJson(String urlPath, RequestHeaderUtil headers, String jsonBody) {
        String fullUrl = baseUrl + urlPath;
        HttpRequest request = HttpUtil.createPost(fullUrl);
        if (StrUtil.isNotBlank(jsonBody)) {
            request.body(jsonBody);
            request.header("Content-Type", "application/json;charset=UTF-8");
            request.addHeaders(headers.getHeader());
            // Hutool 会自动设置 Content-Type 为 application/json
        }
        log.info("发送POST JSON请求: [{}]", request.toString());
        return executeRequest(request, headers);
    }

    /**
     *
     */
    public void set(RequestHeaderUtil headers) {
        UserSmsWebSocket u =  userSmsWebSocketService.selectByDeviceId(headers.getMobileDeviceId());
        if(u!=null){
            JSONObject j =JSONObject.parseObject(u.getUserCookie());
            j.put("JSESSIONID",headers.getJSESSIONID());
            j.put("ss_ctrl",headers.getSs_ctrl());
            j.put("xxx",headers.getXxx());
            u.setUserCookie(j.toJSONString());
            userSmsWebSocketService.save(u);
        }
    }

    public static String toFormString(Map<String, Object> formData) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey());
            sb.append("=");
            if (entry.getValue()!=null) {
                sb.append(URLUtil.encode(entry.getValue().toString()));
            }else {
                sb.append("");
            }

        }
        return sb.toString();
    }

    /**
     * 核心请求执行逻辑
     * @param request HttpRequest对象
     * @param headers 请求头对象
     * @return 一个封装好的JSONObject，结构如下：
     *         - 成功: { "status": 200, "httpStatus": 2xx, ... (原始响应的键值对) }
     *         - 成功但响应非JSON: { "status": 200, "httpStatus": 2xx, "rawBody": "原始响应字符串" }
     *         - 业务失败: { "status": 4xx/5xx, "httpStatus": 4xx/5xx, "errorBody": "错误响应体" }
     *         - 请求异常: { "status": 500, "httpStatus": -1, "errorMessage": "异常信息" }
     */


    private JSONObject executeRequest(HttpRequest request, RequestHeaderUtil headers) {
        // 应用请求头
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        request.removeHeader("User-Agent");
        if (headers != null && headers.getHeader() != null) {
            request.addHeaders(headers.getHeader());
        }
        log.info("<请求：：：：：：>: 【{}】", request.toString());

        // --- 修改：在执行前设置超时 ---
        request.setConnectionTimeout(2000); // 设置连接超时
        request.setReadTimeout(2000);       // 设置读取超

        HttpResponse response = null;
        try {
            log.info("请求网络 to URL: [{}], Method: [{}]", request.getUrl(), request.getMethod());
            response = request.execute();

            // 请求成功 (HTTP状态码 2xx)
            if (response.isOk()) {
                headers.setCookie(response);
                set(headers);
                String body = response.body();

                log.info("请求成功 to [{}], HTTP 状态码: {}, Response Body Length: {} ,Body: {}", request.getUrl(), response.getStatus(), body.length(),StrUtil.brief(body, 10000));
                JSONObject resultJson;
                try {
                    // 原代码: reJson=reJson.parseObject(body); 是错误的用法。
                    // 正确用法是调用静态方法 JSONObject.parseObject()。
                    resultJson = JSONObject.parseObject(body);
                    // 如果响应体是 "null" 或空字符串，parseObject 可能返回 null
                    if (resultJson == null) {
                        resultJson = new JSONObject();
                    }
                    resultJson.put("status", 200); // 业务成功状态码（我们自己定义）
                } catch (JSONException e) {
                    // 如果响应体不是一个有效的JSON，则封装成一个新对象
                    log.warn("响应体 body from [{}] 不是一个有效的JSON. Body: {}", request.getUrl(), StrUtil.brief(body, 100));
                    resultJson = new JSONObject();
                    resultJson.put("rawBody", body); // 将原始字符串放入特定字段
                    resultJson.put("status", 401); // 业务成功状态码（我们自己定义）
                }
                resultJson.put("httpStatus", response.getStatus()); // 保留真实的HTTP状态码
                return resultJson;

            } else {
                // 请求失败 (HTTP状态码 4xx, 5xx等)
                log.error("请求失败 to [{}], HTTP Status: {}, Body: {}", request.getUrl(), response.getStatus(), response.body().toString());
                JSONObject errorJson = new JSONObject();
                errorJson.put("status",400); // 业务失败状态码直接用HTTP状态码
                errorJson.put("httpStatus", response.getStatus());
                errorJson.put("errorBody", response.body()); // 保留错误信息体
                return errorJson;
            }
        } catch (Exception e) {
            // 网络异常、超时等
            log.error("网络异常 to [{}]: {}", request.getUrl(), e.getMessage(), e);
            JSONObject exceptionJson = new JSONObject();
            exceptionJson.put("status", 500); // 内部异常状态码
            exceptionJson.put("httpStatus", 500); // 标记为无法获取HTTP状态码
            exceptionJson.put("errorMessage", e.getMessage());
            return exceptionJson;
        } finally {
            log.info("请求结束.");
            if (response != null) {
                response.close();
            }
        }
    }
    // =================================================================
    // ====================== 新增的原生HTTP请求方法 =====================
    // =================================================================

    /**
     * 使用 Java 原生 HttpURLConnection 发送 POST 表单请求。
     * 此方法不依赖 Hutool 的 HTTP 客户端，作为备选方案。
     *
     * @param urlPath   请求的相对路径
     * @param headers   请求头对象
     * @param formData  表单数据
     * @return 包含状态和数据的JSONObject，结构与 executeRequest 类似
     */
    public JSONObject postFormNative(String urlPath, RequestHeaderUtil headers, Map<String, Object> formData) {
        String fullUrl = baseUrl + urlPath;
        HttpURLConnection conn = null;
        try {
            log.info("原生HTTP请求 to URL: [{}], Method: [POST]", fullUrl);

            // 1. 准备表单数据
            String formString = toUrlEncodedString(formData);
            byte[] formBytes = formString.getBytes(StandardCharsets.UTF_8);

            // 2. 创建连接
            URL url = new URL(fullUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000); // 连接超时
            conn.setReadTimeout(5000);    // 读取超时
            conn.setDoOutput(true);       // 允许写入请求体

            // 3. 设置请求头
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(formBytes.length));
            if (headers != null && headers.getHeader() != null) {
                headers.getHeader().forEach(conn::setRequestProperty);
            }
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");


            // 4. 发送请求体
            try (OutputStream os = conn.getOutputStream()) {
                os.write(formBytes);
            }

            // 5. 获取响应
            int httpStatus = conn.getResponseCode();
            JSONObject resultJson;

            if (httpStatus >= 200 && httpStatus < 300) { // 请求成功
                // 处理Cookie
                Map<String, List<String>> headerFields = conn.getHeaderFields();
                List<String> cookies = headerFields.get("Set-Cookie");
                if (cookies != null && !cookies.isEmpty()) {
                    headers.setCookie((HttpResponse) cookies);
                    set(headers);
                }

                String body = readStream(conn.getInputStream());
                log.info("原生请求成功 to [{}], HTTP 状态码: {}, Response Body Length: {} ,Body: {}", fullUrl, httpStatus, body.length(), StrUtil.brief(body, 10000));

                try {
                    resultJson = JSONObject.parseObject(body);
                    if (resultJson == null) {
                        resultJson = new JSONObject();
                    }
                    resultJson.put("status", 200);
                } catch (JSONException e) {
                    log.warn("原生请求响应体 body from [{}] 不是一个有效的JSON. Body: {}", fullUrl, StrUtil.brief(body, 100));
                    resultJson = new JSONObject();
                    resultJson.put("rawBody", body);
                    resultJson.put("status", 401);
                }
                resultJson.put("httpStatus", httpStatus);
                return resultJson;
            } else { // 请求失败
                String errorBody = readStream(conn.getErrorStream());
                log.error("原生请求失败 to [{}], HTTP Status: {}, Body: {}", fullUrl, httpStatus, errorBody);
                JSONObject errorJson = new JSONObject();
                errorJson.put("status", 400);
                errorJson.put("httpStatus", httpStatus);
                errorJson.put("errorBody", errorBody);
                return errorJson;
            }

        } catch (Exception e) {
            log.error("原生请求网络异常 to [{}]: {}", fullUrl, e.getMessage(), e);
            JSONObject exceptionJson = new JSONObject();
            exceptionJson.put("status", 500);
            exceptionJson.put("httpStatus", 500);
            exceptionJson.put("errorMessage", e.getMessage());
            return exceptionJson;
        } finally {
            log.info("原生请求结束.");
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 读取输入流并转换为字符串
     */
    private String readStream(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {
            log.error("读取输入流时出错: {}", e.getMessage());
            return "";
        }
    }


    /**
     * 将Map格式的表单数据转换为 x-www-form-urlencoded 格式的字符串
     *
     * @param formData 表单数据
     * @return URL编码后的字符串
     */
    private String toUrlEncodedString(Map<String, Object> formData) {
        if (MapUtil.isEmpty(formData)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, Object> entry : formData.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()));
                sb.append("=");
                Object value = entry.getValue();
                if (value != null) {
                    sb.append(URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8.name()));
                }
            }
        } catch (UnsupportedEncodingException e) {
            // 这在现代JVM中基本不可能发生，因为UTF-8是标准字符集
            log.error("严重错误：系统不支持UTF-8编码", e);
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
        return sb.toString();
    }

}