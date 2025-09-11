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
import com.wzz.hslspringboot.pojo.NewSysConfig;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.SysConfigService;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import com.wzz.hslspringboot.utils.DateTimeUtil;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class Function {
    private static final Logger log = LogManager.getLogger(Function.class);
//    Modules api = new Modules();

    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;
    @Autowired
    private Modules api;

    @Autowired
    private SysConfigService sysConfigService;

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

        JSONObject errorInfo = dataArray.getJSONObject(0);
        String errorMessage = "未知的登录错误"; // 提供一个默认的错误信息

        if (errorInfo != null && errorInfo.getString("retMessage") != null) {
            errorMessage = errorInfo.getString("retMessage");
        }

        response.put("message", errorMessage);
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
        UserSmsWebSocket user= userSmsWebSocketService.getByPhone(requestHeaderUtil.getPhone());
        user.setNeedCaptcha("1");
        user.setUuid(null);
        userSmsWebSocketService.save(user);
        log.info("<向数据库发送图形验证码需求::::::>:{}",user.getUserName());
        for (int i = 0; i < 30; i++) {
            UserSmsWebSocket users= userSmsWebSocketService.getByPhone(requestHeaderUtil.getPhone());
            if (users.getUuid()!=null){
                return users.getUuid();
            }
            Thread.sleep(1000);
        }
        return null;
    }

    /**
     * 从配置Map中安全地解析整型值。
     *
     * @param configValue  配置Map
     * @param key          要查找的键
     * @param defaultValue 解析失败或键不存在时返回的默认值
     * @return 解析后的整型值或默认值
     */
    private int parseConfigInt(Map<String, Object> configValue, String key, int defaultValue) {
        if (configValue == null || !configValue.containsKey(key)) {
            return defaultValue;
        }
        try {
            Object value = configValue.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
        } catch (Exception e) {
            log.error("解析系统配置 '{}' 失败，将使用默认值 {}。错误: {}", key, defaultValue, e.getMessage());
        }
        return defaultValue;
    }
    /**
     * 从配置Map中安全地解析任何类型的值。
     *
     * @param configValue  配置Map
     * @param key          要查找的键
     * @param targetType   期望解析的目标类型 (e.g., String.class, Integer.class, Boolean.class)
     * @param defaultValue 解析失败或键不存在时返回的默认值
     * @param <T>          泛型类型
     * @return 解析后的值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T parseConfigValue(Map<String, Object> configValue, String key, Class<T> targetType, T defaultValue) {
        if (configValue == null || !configValue.containsKey(key) || configValue.get(key) == null) {
            return defaultValue;
        }

        Object value = configValue.get(key);

        // 1. 如果值的类型已经是目标类型，直接返回
        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }

        // 2. 尝试进行类型转换
        try {
            String stringValue = value.toString().trim();
            if (stringValue.isEmpty()) {
                return defaultValue;
            }

            // 根据目标类型进行转换
            if (targetType == String.class) {
                return (T) stringValue;
            }
            if (targetType == Integer.class || targetType == int.class) {
                return (T) Integer.valueOf(stringValue);
            }
            if (targetType == Long.class || targetType == long.class) {
                return (T) Long.valueOf(stringValue);
            }
            if (targetType == Double.class || targetType == double.class) {
                return (T) Double.valueOf(stringValue);
            }
            if (targetType == Float.class || targetType == float.class) {
                return (T) Float.valueOf(stringValue);
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                // "true" (不区分大小写) 被认为是 true, 其他所有值都是 false
                return (T) Boolean.valueOf(stringValue);
            }
            if (targetType == BigDecimal.class) {
                return (T) new BigDecimal(stringValue);
            }
            // 可以根据需要添加更多类型转换，例如 Date, List 等

            log.warn("不支持将类型 '{}' 转换为 '{}'，键: '{}'。将尝试强制转换。", value.getClass().getName(), targetType.getName(), key);
            // 3. 最后的尝试：强制转换
            return (T) value;

        } catch (Exception e) {
            log.error("解析系统配置 '{}' (期望类型: {}) 失败，将使用默认值 '{}'。原始值: '{}', 错误: {}",
                    key, targetType.getSimpleName(), defaultValue, value, e.getMessage());
            return defaultValue;
        }
    }


    /**
     * 获取图片验证码
     * 发送手机验证码
     */
    public JSONObject checkData(PostPointmentDTO postPointmentDTO, RequestHeaderUtil requestHeaderUtil, UserSmsWebSocket u) throws IOException, InterruptedException {
        NewSysConfig co = sysConfigService.getConfigByName("sys_config");
        Map<String, Object> configValue = (co != null) ? co.getConfigValue() : null;
        int tpyzm = parseConfigInt(configValue,"image_verification_code_retries",10);
        int dxyzm = parseConfigInt(configValue,"number_of_SMS_verification_code_retries",10);
        Boolean is_sms = parseConfigValue(configValue,"is_sms",Boolean.class ,true);
        JSONObject aaa = getSmsBooles(postPointmentDTO, requestHeaderUtil);
        JSONObject dataObject = aaa.getJSONObject("data");
        Boolean needsms = dataObject.getBoolean("needsms");
        JSONObject reJson = new JSONObject();

        //String dxyzmTimeStr  =  parseConfigValue(configValue,"sms_ code_time",String.class, "2025-01-01 00:00:00");
        String dxyzmTimeStr = u.getSmsCodeTime();

        log.info("成功解析到 'needsms' 的值为: {}", needsms);
        for (int i = 0; i < tpyzm; i++) {
            String uuid = getUUID(requestHeaderUtil);
            postPointmentDTO.setUuid(uuid);
            if (uuid != null) {
                if (needsms) {
                    log.info("发送短信验证码延时5秒发送");
                    //由于重试发送验证码时 必须更新uuid 所以把for删除了 替换为continue 重试的时候重新验证验证码 并且以为发送验证码的时候可能提示频繁所以加上了3秒延时（只是一个测试）
                        JSONObject re = sendSmsCode(postPointmentDTO, requestHeaderUtil, u);
                        log.info("发送短信验证码：{}",re);
                        if (re != null && re.getJSONObject("data").getInteger("resultCode") == 1) {
                            reJson.put("msg", re);
                            reJson.put("needsms", needsms);
                            reJson.put("status", 200);
                            return reJson;
                        }else {
                            reJson.put("msg", re);
                            reJson.put("needsms", needsms);
                            reJson.put("status", 500);
                            continue;
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


    /**
     * 获取图片验证码
     * 发送手机验证码
     */
    public JSONObject checkData(PostPointmentDTO postPointmentDTO, RequestHeaderUtil requestHeaderUtil, UserSmsWebSocket u,int g) throws IOException, InterruptedException {
        NewSysConfig co = sysConfigService.getConfigByName("sys_config");
        Map<String, Object> configValue = (co != null) ? co.getConfigValue() : null;
        int tpyzm = parseConfigInt(configValue,"image_verification_code_retries",10);
        Boolean is_sms = parseConfigValue(configValue,"is_sms",Boolean.class ,true);
        //TODO 调试结束后需要去除这一行  且前端修复一下两个开关
        JSONObject aaa = getSmsBooles(postPointmentDTO, requestHeaderUtil);
        JSONObject dataObject = aaa.getJSONObject("data");
        Boolean needsms = dataObject.getBoolean("needsms");
        JSONObject reJson = new JSONObject();

        log.info("成功解析到 'needsms' 的值为: {}", needsms);
        for (int i = 0; i < tpyzm; i++) {
            String uuid = getUUID(requestHeaderUtil);
            postPointmentDTO.setUuid(uuid);
            if (uuid != null) {
                if (needsms&&is_sms) {
                    log.info("发送短信验证码延时5秒发送");
                    //由于重试发送验证码时 必须更新uuid 所以把for删除了 替换为continue 重试的时候重新验证验证码 并且以为发送验证码的时候可能提示频繁所以加上了3秒延时（只是一个测试）
                    JSONObject re = sendSmsCode(postPointmentDTO, requestHeaderUtil, u);
                    log.info("发送短信验证码：{}",re);
                    if (re != null && re.getJSONObject("data").getInteger("resultCode") == 1) {
                        reJson.put("msg", re);
                        reJson.put("needsms", needsms);
                        reJson.put("status", 200);
                        return reJson;
                    }else {
                        reJson.put("msg", re);
                        reJson.put("needsms", needsms);
                        reJson.put("status", 500);
                        continue;
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
//        if (re != null && re.getInteger("status") == 200) {
            return re;
//        }
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
    /**
     * 上传车牌号！
     */
    public JSONObject postCPH(UserSmsWebSocket userSmsWebSocket,RequestHeaderUtil requestHeaderUtil){
        JSONObject re  = api.postCph(userSmsWebSocket,requestHeaderUtil);
        return re;
    }




}
