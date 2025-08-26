package com.wzz.hslspringboot.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import com.wzz.hslspringboot.service.impl.UserSmsWebSocketServiceImpl;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * cookie
 */
@Data
public class RequestHeaderUtil {
    private static final Logger log = LogManager.getLogger(RequestHeaderUtil.class);
    private String JSESSIONID;
    private String ss_ctrl;
    private String xxx;
    private String Referer;
    private String cookie;
    private String mobileDeviceId;
    private String longJSESSIONID;
    /**
     * userSmsWebSocket.getUserCookie():
     * {
     * "JSESSIONID":"JSESSIONID=3CC6C5FF9ED7C953E350F79501246CD4",
     * "ss_ctrl":"ss_ctrl=80808198c2faa70198c615a8be0d76",
     * "xxx":" 09f3e6527a1a4b858b7c40a30a35746d=WyIzOTAwODUwMDI1Il0",
     * "Referer":"https://hsn.sinograin.com.cn/mobilexcx/html/main/main.html?code=0015jKFa1GPa9K0cK8Ia1rnqQL05jKFf&state=main",
     * "mobileDeviceId":"mobileDeviceId=os7mus9HY8oa5IQjlAevxA5YdUVM"
     * }
     * @param userSmsWebSocket
     */
    public RequestHeaderUtil(UserSmsWebSocket userSmsWebSocket) {
        // 1. 获取 JSON 字符串
        String cookieJsonString = userSmsWebSocket.getUserCookie();
        // 2. 解析 JSON 并赋值
        try {
            // 创建 JSONObject 对象
            JSONObject jsonObject = JSONObject.parseObject(cookieJsonString);
            // 3. 从 JSONObject 中提取数据并赋值给成员变量
            // 使用 optString 方法更安全，如果 key 不存在，会返回空字符串""而不是抛出异常
            this.JSESSIONID = jsonObject.getString("JSESSIONID");
            this.longJSESSIONID = jsonObject.getString("longJSESSIONID");
            this.ss_ctrl = jsonObject.getString("ss_ctrl");
            this.xxx = jsonObject.getString("xxx");
            this.Referer = jsonObject.getString("Referer");
            this.mobileDeviceId = jsonObject.getString("mobileDeviceId");
        } catch (Exception e) {
            // 记录日志或者处理异常，例如设置默认值
            System.err.println("解析用户Cookie JSON失败: " + e.getMessage());
            // 根据业务需求，可以选择在此处初始化变量为空字符串或null
            this.JSESSIONID = "";
            this.ss_ctrl = "";
            this.xxx = "";
            this.Referer = "";
            this.mobileDeviceId = "";
        }
        // 4. 调用方法，拼接最终的 Cookie 字符串
        this.setCookie();
    }

    public String getMobileDeviceId() {
        if (mobileDeviceId != null && mobileDeviceId.contains("=")) {
            return mobileDeviceId.substring(mobileDeviceId.indexOf("=") + 1);
        }
        return "";
    }

    public Map<String, String> getHeader() {
        Map<String, String> map = new HashMap<>();
        map.put("Sec-Fetch-Site", "same-origin");
        map.put("Accept-Language","zh-CN,zh-Hans;q=0.9");
        map.put("Accept","*/*");
        map.put("Sec-Fetch-Mode", "cors");
        map.put("Origin","https://hsn.sinograin.com.cn");
        map.put("Sec-Fetch-Dest","empty");
        map.put("User-Agent","Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.62(0x18003e2d) NetType/WIFI Language/zh_CN miniProgram/");
        map.put("Cookie",this.cookie);
        map.put("Referer",this.Referer);
        return map;
    }
    public void setCookie(HttpResponse response) {
        String[] cookies = new String[]{response.header("Set-Cookie")};
        log.info("<setCookie>{}",cookies);
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie==null||cookie.isEmpty()){
                    continue;
                }
                String[] parts = cookie.split(";");
                String mainPart = parts[0].trim();
                String[] nameValuePair = mainPart.split("=");
                if (nameValuePair.length == 2) {
                    String key = nameValuePair[0].trim();
                    String value = nameValuePair[1].trim();
                    System.out.println("Cookie Key: " + key + ", Cookie Value: " + value);
                    if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)&& key.contains("JSESSIONID")) {
                        this.setJSESSIONID(mainPart);
                    }
                    if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)&& key.contains("ss_ctrl")) {
                        this.setSs_ctrl(mainPart);
                    }
                    if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value) && key.length()==32) {
                        this.setXxx(mainPart);
                    }
                    setCookie();
                }
            }
        }
    }
    private void setCookie(){
        this.cookie=""+JSESSIONID+";"+ss_ctrl+";"+xxx;
    }
}
