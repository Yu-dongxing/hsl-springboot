package com.wzz.hslspringboot.utils;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONException;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

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

    /**
     * 发送POST JSON请求
     * @param urlPath   请求的相对路径
     * @param headers   请求头对象
     * @param jsonBody  JSON格式的请求体
     * @return 包含状态和数据的JSONObject
     */
    public JSONObject postJson(String urlPath, RequestHeaderUtil headers, String jsonBody) {
        String fullUrl = baseUrl + urlPath;
        HttpRequest request = HttpRequest.post(fullUrl);
        if (StrUtil.isNotBlank(jsonBody)) {
            request.body(jsonBody);
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
        if (headers != null && headers.getHeader() != null) {
            request.addHeaders(headers.getHeader());
        }
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
}