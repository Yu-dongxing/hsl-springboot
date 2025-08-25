package com.wzz.hslspringboot.Captcha;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;

public class api {
   private final String baiDuAccess_token= "24.e82241dc0fa52ab6181f4367b4bd222b.2592000.1758524883.282335-44901639";
    private final String ymToken= "oM69qj4ZGAJKBd1l12jyicfWmGjG8ZGL0w9tFtcsVVc";
    private final String geePassSdk="37f43982ef45430ca8cf734056926788h6ehpw1puxnxdca5";

   public void baiDuSdk(CaptchaData captchaData) throws IOException {
       String remark="输出计算结果";
       String resultString = Jsoup.connect("https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic?access_token="+baiDuAccess_token)
               .data("image", captchaData.getTemplateImage())
               .header("Content-Type", "application/x-www-form-urlencoded")
               .ignoreContentType(true).timeout(120000).post().text();
       JSONObject jsonObject = JSONObject.parseObject(resultString);
       System.out.println(jsonObject);
       if (!jsonObject.containsKey("error_code")) {
           String result=jsonObject.getJSONArray("words_result").getJSONObject(0).getString("words");
           System.out.println("识别成功结果为:"+result);
           captchaData.setExtra(result);
           captchaData.setStatus(200);
       } else {
           System.out.println("失败:"+jsonObject.containsKey("error_msg"));
           captchaData.setMsg(jsonObject.getString("error_msg"));
           captchaData.setStatus(500);
       }
   }
    public void YMSdk(CaptchaData captchaData) throws IOException {
        String typeid= "300010";
        Map< String, String> data = new HashMap<>();
        data.put("token",ymToken);
        data.put("type", typeid);
        data.put("extra", captchaData.getExtra());
        data.put("image", removeImageBase64Prefix(captchaData.getBackgroundImage()));
        String resultString = Jsoup.connect("http://api.jfbym.com/api/YmServer/customApi")
                .requestBody(JSON.toJSONString(data))
                .header("Content-Type", "application/json")
                .ignoreContentType(true).timeout(120000).post().text();
        JSONObject jsonObject = JSONObject.parseObject(resultString);
        Integer code = jsonObject.getInteger("code");
        if (code==10000) {
            String result=jsonObject.getJSONObject("data").getString("data");
            System.out.println("识别成功结果为:"+result);
            captchaData.setExtra(result);
            captchaData.setStatus(200);
        }else {
            System.out.println("识别失败原因为:"+jsonObject.getString("msg"));
            captchaData.setMsg(jsonObject.getString("msg"));
            captchaData.setStatus(500);
        }
    }
    public void geePassSdk(CaptchaData captchaData) throws IOException {
        Map< String, Object> data = new HashMap<>();
        data.put("token",geePassSdk);
        data.put("type", 20110);
        data.put("image", removeImageBase64Prefix(captchaData.getBackgroundImage()));
        String resultString = Jsoup.connect("https://api.geepass.cn/api/recognize/captcha")
                .requestBody(JSON.toJSONString(data))
                .header("Content-Type", "application/json")
                .ignoreContentType(true).timeout(120000).post().text();
        JSONObject jsonObject = JSONObject.parseObject(resultString);
        Integer code = jsonObject.getInteger("code");
        if (code==10000) {
            int result=jsonObject.getJSONObject("data").getJSONObject("data").getJSONArray("target").getInteger(0);
            System.out.println("识别成功结果为:"+(result/2-6));
            captchaData.setResult(result/2-6);
            captchaData.setStatus(200);
        }else {
            System.out.println("识别失败原因为:"+jsonObject.getString("msg"));
            captchaData.setMsg(jsonObject.getString("msg"));
            captchaData.setStatus(500);
        }
    }

    public static String removeImageBase64Prefix(String base64Str) {
        if (base64Str == null) {
            return null;
        }
        // 正则匹配去除 data:image/...;base64, 前缀
        return base64Str.replaceFirst("^data:image/\\w+;base64,", "");
    }

}
