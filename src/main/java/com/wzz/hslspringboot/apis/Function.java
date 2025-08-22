package com.wzz.hslspringboot.apis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class Function {
    private static final Logger log = LogManager.getLogger(Function.class);
//    Modules api = new Modules();

    @Autowired
    private Modules api;

    public Boolean checkCookie(UserSmsWebSocket user, RequestHeaderUtil header) {
        JSONObject re = api.updateCookie(user, header);
        if (re != null && re.getInteger("status") == 200&&re.getJSONArray("data").getJSONObject(0).getString("loginCode").equals("1")) {
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
     *         {
     *           "success": boolean, // true表示校验通过
     *           "message": "描述信息",
     *           "data": JSONObject // 校验通过时，为用户信息；否则为 null
     *         }
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

    public JSONObject search(UserSmsWebSocket user, RequestHeaderUtil header){
        JSONObject re =  api.search(user, header);
        log.info("<search接口返回数据>"+re.toString());
        if (re != null && re.getInteger("status") == 200){
            return re;
        }
        return null;
    }
    public JSONObject getResvSjList(RequestHeaderUtil header,JSONObject data){
        JSONObject re =  api.getResvSjList(header,data);
        if (re != null && re.getInteger("status") == 200){
            return re;
        }
        return null;
    }
    /**
     * 提交预约
     */
    public JSONObject postInfo(RequestHeaderUtil header, PostPointmentDTO post){
        JSONObject re = api.postReserve(post, header);
        if (re != null && re.getInteger("status") == 200){
            return re;
        }
        return null;
    }
    /**
     *获取车牌id
     */
    public JSONObject getLicensePlateId(RequestHeaderUtil header,UserSmsWebSocket user){
        JSONObject re  = api.getVehicleInfoByUserId(user, header);
        if (re != null && re.getInteger("status") == 200){
            return re;
        }
        return null;
    }
    /**
     * 获取是否需要短信验证码
     */
    public JSONObject getSmsBooles(PostPointmentDTO p,RequestHeaderUtil header){
        JSONObject re = api.getResvMxList(p.getZznm(),p.getPzmxnm(),header);
        if (re != null && re.getInteger("status") == 200){
            return re;
        }
        return null;
    }
    /**
     * 获取图片验证码
     */
    public String getPhotoCatch(RequestHeaderUtil header){
        String t = String.valueOf(System.currentTimeMillis());
        byte[] re = api.getCaptchaImage(t,header);
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
    public String photoCodeOcr(RequestHeaderUtil requestHeaderUtil){
        for (int i = 0; i < 10; i++) {
            try {
                String imgBase =getPhotoCatch(requestHeaderUtil);
                JSONObject re = api.photeCodeOcr(imgBase);
                log.info("<UNK>, <UNK>: {}", re.toString());
                String as = String.valueOf(calculateManually(re.getString("data")));
                return as;
            }catch (Exception e){
                log.error("<UNK>", e);
            }
        }
        return null;
    }

    public String getUUID(RequestHeaderUtil requestHeaderUtil){
        for (int i = 0; i < 10; i++) {
            String as = photoCodeOcr(requestHeaderUtil);
            if(as==null){

            }
            log.info("获取图片验证码-{}",as);
            JSONObject g = checkPhoteCatch(as,requestHeaderUtil);
            log.info("<UNK>-{}",g);
            if (g != null && g.getJSONObject("data").getInteger("retCode")==1) {
                String uuid=g.getJSONObject("data").getString("uuid");
                return uuid;
            }
        }
        return null;
    }

    /**
     * 获取图片验证码
     *  发送手机验证码
     */
    public JSONObject checkData(PostPointmentDTO postPointmentDTO,RequestHeaderUtil requestHeaderUtil,UserSmsWebSocket u){
        JSONObject aaa = getSmsBooles(postPointmentDTO,requestHeaderUtil);
        JSONObject dataObject = aaa.getJSONObject("data");
        Boolean needsms = dataObject.getBoolean("needsms");
        JSONObject reJson=new JSONObject();
        // 打印结果
        log.info("成功解析到 'needsms' 的值为: {}", needsms);
        for (int i = 0; i < 10; i++) {
            String uuid= getUUID(requestHeaderUtil);
            postPointmentDTO.setUuid(uuid);
            if (uuid!=null){
                if(needsms){
                    JSONObject re = sendSmsCode(postPointmentDTO,requestHeaderUtil,u);
                    if (re!=null&&re.getJSONObject("data").getInteger("resultCode")==1){
                        reJson.put("needsms",needsms);
                        reJson.put("status",200);
                        return reJson;
                    }
                }
                reJson.put("needsms",needsms);
                reJson.put("status",200);
                return reJson;
            }
        }
        reJson.put("needsms",needsms);
        reJson.put("status",500);
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
    public JSONObject checkPhoteCatch(String code,RequestHeaderUtil header){
        JSONObject re = api.checkCaptchaCode(code,header);
        if (re != null && re.getInteger("status") == 200){
            return re;
        }
        return null;
    }
    /**
     * 发送手机短信验证码
     */
    public JSONObject sendSmsCode(PostPointmentDTO p,RequestHeaderUtil header,UserSmsWebSocket user){
        JSONObject re = api.sendSMSCode(p,user,header);
        if (re != null && re.getInteger("status") == 200){
            return re;
        }
        return null;
    }

    public JSONObject getRandomcode(UserSmsWebSocket user, RequestHeaderUtil header) {
        JSONObject re = api.getRandomcode(user, header);
        if (re != null && re.getInteger("status") == 200){
            return re;
        }
        return null;
    }
}
