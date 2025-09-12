package com.wzz.hslspringboot.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
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
    private String slyyServletJSESSIONID;
    private String phone;
    private String pzmxnm;

     /**
     * userSmsWebSocket.getUserCookie():
     * {
     * "JSESSIONID":"JSESSIONID=3CC6C5FF9ED7C953E350F79501246CD4",
     * "ss_ctrl":"ss_ctrl=80808198c2faa70198c615a8be0d76",
     * "xxx":" 09f3e6527a1a4b858b7c40a30a35746d=WyIzOTAwODUwMDI1Il0",
     * "Referer":"https://hsn.sinograin.com.cn/mobilexcx/html/main/main.html?code=0015jKFa1GPa9K0cK8Ia1rnqQL05jKFf&state=main",
     * "mobileDeviceId":"mobileDeviceId=os7mus9HY8oa5IQjlAevxA5YdUVM"
     * }
     *
     * @param userSmsWebSocket
     */
    public RequestHeaderUtil(UserSmsWebSocket userSmsWebSocket) {
        // 1. 获取 JSON 字符串
        String cookieJsonString = userSmsWebSocket.getUserCookie();
        boolean useDefaultValues = false;

        // 2. 判断是否为空，如果为空则标记使用默认值
        if (StrUtil.isBlank(cookieJsonString)) {
            useDefaultValues = true;
            log.warn("用户Cookie JSON字符串为空，将使用随机默认值。");
        } else {
            // 3. 解析 JSON 并赋值
            try {
                // 创建 JSONObject 对象
                JSONObject jsonObject = JSONObject.parseObject(cookieJsonString);

                // 如果解析出的jsonObject为空或者没有内容，也使用默认值
                if (jsonObject == null || jsonObject.isEmpty()) {
                    useDefaultValues = true;
                    log.warn("解析后的用户Cookie JSON对象为空，将使用随机默认值。");
                } else {
                    // 从 JSONObject 中提取数据并赋值给成员变量
                    this.JSESSIONID = jsonObject.getString("JSESSIONID");
                    this.slyyServletJSESSIONID = jsonObject.getString("getSlyyServletJSESSIONID");
                    this.ss_ctrl = jsonObject.getString("ss_ctrl");
                    this.xxx = jsonObject.getString("xxx");
                    this.Referer = jsonObject.getString("Referer");
                    this.mobileDeviceId = jsonObject.getString("mobileDeviceId");
                }
            } catch (Exception e) {
                // 解析异常，标记使用默认值
                log.error("解析用户Cookie JSON失败: {}, 将使用随机默认值。", e.getMessage());
                useDefaultValues = true;
            }
        }

        // 4. 如果需要，则设置随机默认值
        if (useDefaultValues) {
            this.setDefaultRandomValues();
        }

        // 5. 调用方法，拼接最终的 Cookie 字符串
        this.setCookie();
    }


    /**
     * 当传入的cookie json为空或解析失败时，设置随机的默认值
     */
    private void setDefaultRandomValues() {
        log.info("正在设置随机默认Cookie值...");
        String randomUUID32Upper = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String randomUUID32Lower = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        // 模拟一个微信的 openid (28位)
        String randomOpenId = "os7mus" + RandomUtil.randomString(22);

        this.JSESSIONID = "JSESSIONID=" + randomUUID32Upper;
        this.slyyServletJSESSIONID = this.JSESSIONID; // 默认情况下可以设为一样
        this.ss_ctrl = "ss_ctrl=80808198" + RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER + "abcdef", 22);
        this.xxx = randomUUID32Lower + "=WyIz" + RandomUtil.randomNumbers(10) + "Il0";
        this.mobileDeviceId = "mobileDeviceId=" + randomOpenId;
        this.Referer = "https://hsn.sinograin.com.cn/mobilexcx/html/main/main.html?code=" + RandomUtil.randomString(32) + "&state=main";
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
                String mainPath =parts[1].trim();
                String[] nameValuePath = mainPath.split("=");
                String[] nameValuePair = mainPart.split("=");
                String Path =null;
                String PathValue = null;
                if (nameValuePath.length==2){
                    Path = nameValuePath[0].trim();
                    PathValue = nameValuePath[1].trim();
                }
                if (nameValuePair.length == 2) {
                    String key = nameValuePair[0].trim();
                    String value = nameValuePair[1].trim();
                    System.out.println("Cookie Key: " + key + ", Cookie Value: " + value);
                    if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)&& key.contains("JSESSIONID")) {
                        if (Path!=null&&PathValue.contains("slyyServlet")) {
                            this.setSlyyServletJSESSIONID(mainPart);
                        }else {
                            this.setJSESSIONID(mainPart);
                        }
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
        this.cookie=slyyServletJSESSIONID+";"+xxx+";"+JSESSIONID+";"+ss_ctrl;
        //this.cookie="JSESSIONID=2658AFD959D27A897F1E2A8C36FE66A2; 09f3e6527a1a4b858b7c40a30a35746d=WyI1NDAwNjY1MTMiXQ; JSESSIONID=E7165F70B061F03E9FF2D77F7BB3D8B6; ss_ctrl=8080819934335101993713a63d1386";
    }
}
