package com.wzz.hslspringboot.apis;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.wzz.hslspringboot.Captcha.Captcha;
import com.wzz.hslspringboot.Captcha.CaptchaData;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class Function {
    private static final Logger log = LogManager.getLogger(Function.class);
//    Modules api = new Modules();

    @Autowired
    private Modules api;

    public Boolean checkCookie(UserSmsWebSocket user, RequestHeaderUtil header) {
        JSONObject re = api.updateCookie(user, header);
        if (re != null && re.getInteger("status") == 200 && re.getJSONArray("data").getJSONObject(0).getString("loginCode").equals("1")) {
            for (int i = 0; i < re.getJSONArray("data").size(); i++) {
                if (re.getJSONArray("data").getJSONObject(i).getString("loginCode").equals("1")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查用户Cookie状态，并返回一个包含处理结果的封装JSON对象。
     * 该方法始终返回一个非空的 Fastjson JSONObject。
     *
     * @param user   WebSocket用户对象
     * @param header 请求头工具类
     * @return 一个包含校验结果的JSONObject，结构如下：
     * {
     * "success": boolean, // true表示校验通过
     * "message": "描述信息",
     * "data": JSONObject // 校验通过时，为用户信息；否则为 null
     * }
     */
    public JSONObject checkCookieAndGetResponse(UserSmsWebSocket user, RequestHeaderUtil header) {
        // 1. 初始化返回结果，默认为失败状态
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("data", null);

        // 2. 调用API获取最新的状态
        JSONObject apiResponse = api.updateCookie(user, header);

        // 3. 校验API响应
        // Fastjson 的 getIntValue 如果键不存在或值不是数字，会返回0，正好符合我们的需求
        if (apiResponse == null || apiResponse.getIntValue("status") != 200) {
            String message = (apiResponse == null) ? "API请求失败，返回为空" : "API状态码异常: " + apiResponse.getIntValue("status");
            response.put("message", message);
            return response;
        }

        // Fastjson 的 getJSONArray 如果键不存在或类型不匹配，会返回 null
        JSONArray dataArray = apiResponse.getJSONArray("data");
        if (dataArray == null || dataArray.isEmpty()) {
            response.put("message", "API响应格式错误或data数组为空");
            return response;
        }

        // 4. 遍历data数组，查找有效的登录信息
        for (int i = 0; i < dataArray.size(); i++) { // 注意：Fastjson 的 JSONArray 用 .size()
            JSONObject userInfo = dataArray.getJSONObject(i);

            // 防御性编程：确保userInfo不为null，且loginCode为"1"
            // Fastjson 的 getString 在键不存在时返回 null，所以 "1".equals() 写法很关键
            if (userInfo != null && "1".equals(userInfo.getString("loginCode"))) {

                // 5. 找到有效信息，更新返回结果为成功状态
                response.put("success", true);
                response.put("message", "Cookie校验成功");
                response.put("data", userInfo);

                // 找到后立即返回，无需继续遍历
                return response;
            }
        }

        // 6. 如果循环结束仍未找到，说明Cookie有效但无登录用户
        response.put("message", "未找到有效的登录会话");
        return response;
    }

    public JSONObject search(UserSmsWebSocket user, RequestHeaderUtil header) {
        JSONObject re = api.search(user, header);
        log.info("<search接口返回数据>" + re.toString());
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    public JSONObject getResvSjList(RequestHeaderUtil header, JSONObject data) {
        JSONObject re = api.getResvSjList(header, data);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    /**
     * 提交预约
     */
    public JSONObject postInfo(RequestHeaderUtil header, PostPointmentDTO post) throws JsonProcessingException {
        JSONObject re = api.postReserve(post, header);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    /**
     * 获取车牌id
     */
    public JSONObject getLicensePlateId(RequestHeaderUtil header, UserSmsWebSocket user) {
        JSONObject re = api.getVehicleInfoByUserId(user, header);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    /**
     * 获取是否需要短信验证码
     */
    public JSONObject getSmsBooles(PostPointmentDTO p, RequestHeaderUtil header) {
        JSONObject re = api.getResvMxList(p.getZznm(), p.getPzmxnm(), header);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    /**
     * 获取图片验证码
     */
    public String getPhotoCatch(RequestHeaderUtil header) {
        String t = String.valueOf(System.currentTimeMillis());
        byte[] re = api.getCaptchaImage(t, header);
        if (re != null && re.length > 0) {
            String base64Image = Base64.getEncoder().encodeToString(re);
            log.info("成功获取并编码图片验证码, Base64长度: {}", base64Image.length());

            // 3. (推荐) 拼接成Data URI Scheme，方便前端直接在img标签中使用
            return "data:image/png;base64," + base64Image;
        } else {
            log.error("调用getCaptchaImage方法失败，返回为空。");
            return null;
        }
    }

    /**
     * 识别图片验证码
     */
    public String photoCodeOcr(RequestHeaderUtil requestHeaderUtil) {
        for (int i = 0; i < 10; i++) {
            try {
                String imgBase = getPhotoCatch(requestHeaderUtil);
                JSONObject re = api.photeCodeOcr(imgBase);
                log.info("<UNK>, <UNK>: {}", re.toString());
                String as = String.valueOf(calculateManually(re.getString("data")));
                return as;
            } catch (Exception e) {
                log.error("<UNK>", e);
            }
        }
        return null;
    }

    public String getUUID(RequestHeaderUtil requestHeaderUtil) throws IOException, InterruptedException {
        JSONObject rrrr = getCaptcha(requestHeaderUtil);
        log.info("<获取图片或滑动验证码::::::>:{}", rrrr);
        CaptchaData ew = new CaptchaData(rrrr);
        new Captcha(ew);
        if (ew.getStatus() == 200) {
            //Thread.sleep(5000);
            JSONObject qqqq = ew.getJson();
            log.info("<获取图片或滑动验证码>:{}", qqqq.toString());
            JSONObject sa = checkCaptcha(requestHeaderUtil, qqqq.toString());
            log.info("<<UNK>::::::>:{}", sa.toString());
            return sa.getJSONObject("data").getString("uuid");
        }
        return null;
    }

    /**
     * 获取图片验证码
     * 发送手机验证码
     */
    public JSONObject checkData(PostPointmentDTO postPointmentDTO, RequestHeaderUtil requestHeaderUtil, UserSmsWebSocket u) throws IOException, InterruptedException {
        JSONObject aaa = getSmsBooles(postPointmentDTO, requestHeaderUtil);
        JSONObject dataObject = aaa.getJSONObject("data");
        Boolean needsms = dataObject.getBoolean("needsms");
        JSONObject reJson = new JSONObject();
        // 打印结果
        log.info("成功解析到 'needsms' 的值为: {}", needsms);
        for (int i = 0; i < 10; i++) {
            String uuid = getUUID(requestHeaderUtil);
            postPointmentDTO.setUuid(uuid);
            if (uuid != null) {
                if (needsms) {
                    JSONObject re = sendSmsCode(postPointmentDTO, requestHeaderUtil, u);
                    if (re != null && re.getJSONObject("data").getInteger("resultCode") == 1) {
                        reJson.put("needsms", needsms);
                        reJson.put("status", 200);
                        return reJson;
                    }
                }
                reJson.put("needsms", needsms);
                reJson.put("status", 200);
                return reJson;
            }
        }
        reJson.put("needsms", needsms);
        reJson.put("status", 500);
        return reJson;
    }

    public int calculateManually(String expression) {

        String cleanExpr = expression.replaceAll("[=?\\s]", ""); // 得到 "5+4"

        String operator = "";
        if (cleanExpr.contains("+")) {
            operator = "\\+";
        } else if (cleanExpr.contains("-")) {
            operator = "-";
        } else if (cleanExpr.contains("x")) {
            operator = "x";
        } else {
            throw new IllegalArgumentException("不支持的运算符: " + cleanExpr);
        }

        String[] parts = cleanExpr.split(operator);
        if (parts.length != 2) {
            throw new IllegalArgumentException("无效的表达式格式: " + cleanExpr);
        }

        int num1 = Integer.parseInt(parts[0]);
        int num2 = Integer.parseInt(parts[1]);

        switch (operator) {
            case "\\+":
                return num1 + num2;
            case "-":
                return num1 - num2;
            case "x":
                return num1 * num2;
            default:
                // 不会执行到这里
                throw new IllegalStateException("代码逻辑错误");
        }
    }


    /**
     * 验证图片验证码
     */
    public JSONObject checkPhoteCatch(String code, RequestHeaderUtil header) {
        JSONObject re = api.checkCaptchaCode(code, header);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    /**
     * 发送手机短信验证码
     */
    public JSONObject sendSmsCode(PostPointmentDTO p, RequestHeaderUtil header, UserSmsWebSocket user) {
        JSONObject re = api.sendSMSCode(p, user, header);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    public JSONObject getRandomcode(UserSmsWebSocket user, RequestHeaderUtil header) {
        JSONObject re = api.getRandomcode(user, header);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    /**
     * 获取验证码(响应滑块)
     * POST https://hsn.sinograin.com.cn/slyyServlet//service/captcha/getCaptcha
     * 接口ID：340218781
     * 接口地址：https://app.apifox.com/link/project/6997949/apis/api-340218781
     */
    public JSONObject getCaptcha(RequestHeaderUtil requestHeaderUtil) {
        JSONObject re = api.getSliderCaptcha(requestHeaderUtil);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    public JSONObject checkCphYycs(PostPointmentDTO user, RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> data = new HashMap<>();
        data.put("zznm", user.getZznm());
        data.put("cph", user.getCphStr());
        data.put("devicetype", "weixin");
        JSONObject re = api.checkCphYycs(requestHeaderUtil, data);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    /**
     * 验证验证码(滑块)
     * POST https://hsn.sinograin.com.cn/slyyServlet/service/captcha/checkCaptcha
     * 接口ID：340218782
     * 接口地址：https://app.apifox.com/link/project/6997949/apis/api-340218782
     */
    public JSONObject checkCaptcha(RequestHeaderUtil requestHeaderUtil, String data) {
        JSONObject re = api.checkSliderCaptcha(requestHeaderUtil, data);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
//        Map<String,String> headers=requestHeaderUtil.getHeader();
//        HttpURLConnection connection = null;
//        try {
//            // 创建连接
//            URL url = new URL("https://hsn.sinograin.com.cn/slyyServlet/service/captcha/checkCaptcha");
//            connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("POST");
//            connection.setDoOutput(true);
//            connection.setDoInput(true);
//            connection.setUseCaches(false);
//            connection.setInstanceFollowRedirects(true);
//            connection.setConnectTimeout(10000); // 连接超时时间
//            connection.setReadTimeout(10000);    // 读取超时时间
//
//            // 设置请求头
//            if (headers != null && !headers.isEmpty()) {
//                for (Map.Entry<String, String> entry : headers.entrySet()) {
//                    connection.setRequestProperty(entry.getKey(), entry.getValue());
//                }
//            }
//            String jsonBody=data;
//            // 写入请求体
//            if (jsonBody != null && !jsonBody.isEmpty()) {
//                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
//                try (OutputStream os = connection.getOutputStream();
//                     Writer writer = new OutputStreamWriter(os, "UTF-8")) {
//                    writer.write(jsonBody);
//                    writer.flush();
//                }
//            }
//
//            // 获取响应
//            int responseCode = connection.getResponseCode();
//            BufferedReader reader;
//            if (responseCode >= 200 && responseCode < 300) {
//                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
//            } else {
//                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
//            }
//
//            StringBuilder response = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                response.append(line);
//            }
//            reader.close();
//
//            // 将响应转换为 JSONObject
//            JSONObject jsonResponse = JSONObject.parseObject(response.toString());
//            return jsonResponse;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            return (JSONObject) new JSONObject().put("error", "Request failed: " + e.getMessage());
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
    }

    /**
     * 获取验证码(响应点击文字)
     * POST https://hsn.sinograin.com.cn/slyyServlet//service/captcha/getCaptcha
     * 接口ID：340218783
     * 接口地址：https://app.apifox.com/link/project/6997949/apis/api-340218783
     */
    public JSONObject getCaptchaText(RequestHeaderUtil requestHeaderUti) {
        JSONObject re = api.getWordClickCaptcha(requestHeaderUti);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    /**
     * 验证验证码(点击文字)
     * POST https://hsn.sinograin.com.cn/slyyServlet//service/captcha/checkCaptcha
     * 接口ID：340218784
     * 接口地址：https://app.apifox.com/link/project/6997949/apis/api-340218784
     */
    public JSONObject checkCaptchaText(RequestHeaderUtil requestHeaderUtil, String captchaId, Map<String, Object> data) {
        JSONObject re = api.checkWordClickCaptcha(requestHeaderUtil, captchaId, data);
        if (re != null && re.getInteger("status") == 200) {
            return re;
        }
        return null;
    }

    /**
     *
     */
    public void getRandomcode(PostPointmentDTO p, RequestHeaderUtil requestHeaderUtil, UserSmsWebSocket u) {
        JSONObject o = getRandomcode(u, requestHeaderUtil);
        log.info("获取随机数 :{}", o);
        JSONObject dataJson = o.getJSONObject("data");
        String randomCode = dataJson.getString("randomCode");
        log.info("成功解析到 randomCode: {}", randomCode);
        p.setLxfs(u.getUserPhone() + "_" + randomCode);
    }

    /**
     * POST /slyyServlet/service/grxx/getGrxxStatus
     */
    public JSONObject getGrxxStatus(PostPointmentDTO p, RequestHeaderUtil requestHeaderUtil, UserSmsWebSocket u) {
        JSONObject re = api.getGrxxStatus(u, requestHeaderUtil, p);
        return re;
    }
    /**
     * POST /slyyServlet/service/nhyy/hdmCheck
     */
    public JSONObject hdmCheck(RequestHeaderUtil requestHeaderUtil, UserSmsWebSocket u){
        JSONObject re  =api.hdmCheck(u, requestHeaderUtil);
        return re;
    }
    /**
     * POST /slyyServlet/service/nhyy/getDistanceByCurrentLocation
     */
    public JSONObject getDistanceByCurrentLocation(String lkdm,String lklongitude,String lklatitude,PostPointmentDTO p, RequestHeaderUtil requestHeaderUtil, UserSmsWebSocket u){
        JSONObject re = api.getDistanceByCurrentLocation(lkdm,lklongitude,lklatitude,u,requestHeaderUtil);
        return re;
    }



}
