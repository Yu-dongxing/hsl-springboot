package com.wzz.hslspringboot.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * cookie
 */
@Data
public class RequestHeaderUtil {
    private String JSESSIONID;
    private String ss_ctrl;
    private String xxx;
    private String Referer;
    private String cookie;

    public RequestHeaderUtil(String JSESSIONID, String ss_ctrl, String xxx, String Referer) {
        this.JSESSIONID = JSESSIONID;
        this.ss_ctrl = ss_ctrl;
        this.Referer = Referer;
        this.xxx = xxx;
        this.setCookie();
    }
    public Map<String, String> getHeader() {
        Map<String, String> map = new HashMap<>();
        map.put("Sec-Fetch-Site", "same-origin");
        map.put("Accept-Language","zh-CN,zh-Hans;q=0.9");
        map.put("Sec-Fetch-Mode", "cors");
        map.put("Content-Type", "application/x-www-form-urlencoded");
        map.put("Origin","https://hsn.sinograin.com.cn");
        map.put("Content-Length","0");
        map.put("Sec-Fetch-Dest","empty");
        map.put("Cookie",this.cookie);
        map.put("Referer",this.Referer);
        return map;
    }
    public void setCookie(HttpResponse response) {
        String[] cookies = new String[]{response.header("Set-Cookie")};
        if (cookies != null) {
            for (String cookie : cookies) {
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
        this.cookie=""+JSESSIONID+ss_ctrl+xxx;
    }
}
