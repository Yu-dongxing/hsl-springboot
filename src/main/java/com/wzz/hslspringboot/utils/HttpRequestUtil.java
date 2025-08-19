package com.wzz.hslspringboot.utils;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HttpRequestUtil {

    private static final Logger log = LogManager.getLogger(HttpRequestUtil.class);
    // JDK 8u111版本后，HTTPS的代理隧道认证默认被禁用，这会导致带用户名密码的HTTPS代理失败
    // 通过将 "https" 从禁用列表中移除来解决此问题
    // 另一种常见写法是 System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    static {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "false");
    }
    /**
     * 发送GET请求
     *
     * @param url       请求URL
     * @param headers   请求头对象
     * @param params    URL参数
     * @return 响应体字符串
     */
    public JSONObject get(String url, RequestHeaderUtil headers, Map<String, Object> params) {
        HttpRequest request = HttpRequest.of(url);
        request.method(Method.GET);
        if (MapUtil.isNotEmpty(params)) {
            request.form(params);
        }
        return executeRequest(request,headers);
    }
    /**
     * 发送POST表单请求
     *
     * @param url       请求URL
     * @param headers   请求头对象
     * @param formData  表单数据
     * @return 响应体字符串
     */
    public JSONObject postForm(String url, RequestHeaderUtil headers, Map<String, Object> formData) {
        HttpRequest request = HttpRequest.of(url);
        request.method(Method.POST);

        if (MapUtil.isNotEmpty(formData)) {
            request.form(formData);
        }

        return executeRequest(request, headers);
    }
    /**
     * 发送POST JSON请求
     *
     * @param url       请求URL
     * @param headers   请求头对象
     * @param jsonBody  JSON格式的请求体
     * @return 响应体字符串
     */
    public JSONObject postJson(String url, RequestHeaderUtil headers, String jsonBody) {
        HttpRequest request = HttpRequest.of(url);
        request.method(Method.POST);
        if (StrUtil.isNotBlank(jsonBody)) {
            request.body(jsonBody);
        }
        return executeRequest(request, headers);
    }
    /**
     * 执行请求并处理响应
     *
     * @param request HttpRequest对象
     * @return 响应体字符串, 失败返回null
     */
    private JSONObject executeRequest(HttpRequest request, RequestHeaderUtil headers) {
        HttpResponse response = null;
        JSONObject reJson=new JSONObject();
        try {
            response = request.execute();
            if (response.isOk()) {
                headers.setCookie(response);
                String body = response.body();
                log.info("Request to [{}] successful, response length: {}", request.getUrl(), body.length());
                reJson=reJson.parseObject(body);
                reJson.put("status", 200);
                return reJson;
            } else {
                reJson.put("status", 400);
                log.error("Request to [{}] failed, status: {}, body: {}", request.getUrl(), response.getStatus(), response.body());
                return reJson;
            }
        } catch (Exception e) {
            reJson.put("status", 500);
            log.error("Exception during request to [{}]: {}", request.getUrl(), e.getMessage(), e);
            return reJson;
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }
}
