package com.wzz.hslspringboot.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil; // 假设这个DTO已在之前的步骤中创建
import com.wzz.hslspringboot.DTO.DepotDetailRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 中储粮 "三农" 预约系统 API 客户端
 * 使用 Hutool HTTP 封装了 Postman 集合中的所有接口请求。
 * 核心要求：所有业务接口的调用都必须传递一个有效的 Cookie。
 */
@Component
@Slf4j
public class SinoGrainApiClient {

    // 从 application.yml 注入基础 URL
    @Value("${sino-grain.api.base-url}")
    private String baseUrl;

    private static final String SINO_GRAIN_ORIGIN = "https://hsn.sinograin.com.cn";
    private static final String SINO_GRAIN_REFERER = "https://hsn.sinograin.com.cn/mobilexcx/html/main/main.html";

    /**
     * 构建所有请求通用的 Header
     * @return 包含通用请求头的 Map
     */
    private Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Origin", SINO_GRAIN_ORIGIN);
        headers.put("Referer", SINO_GRAIN_REFERER); // 根据Postman文件，Referer是固定的
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("Accept-Language", "zh-CN,zh-Hans;q=0.9");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Dest", "empty");
        return headers;
    }

    // =====================================================================================
    // 1. 认证与会话 (Authentication & Session)
    // =====================================================================================

    /**
     * 【更新cookie】接口: 使用用户名和加密密码登录，获取新的会话Cookie。
     * @param username 用户名 (手机号)
     * @param encryptedPassword MD5加密后的密码
     * @param initialCookie 可选的初始Cookie（例如 `09f3e6527a1a4b858b7c40a30a35746d=...`）
     * @return 完整的 HttpResponse 对象，调用方需从中提取 'Set-Cookie' 响应头来建立会话。
     */
    public HttpResponse login(String username, String encryptedPassword, String initialCookie) {
        String url = SINO_GRAIN_ORIGIN + "/slyyServlet/j_bsp_security_check/mobile";
        log.info("Attempting login for user: {}", username);

        HttpRequest request = HttpRequest.post(url)
                .headerMap(getDefaultHeaders(), true)
                .form("j_username", username)
                .form("j_password", encryptedPassword)
                .form("devicetype", "weixin");

        if (StringUtils.hasText(initialCookie)) {
            request.cookie(initialCookie);
        }

        return request.execute();
    }

    /**
     * 【检验密码】接口: 另一种形式的认证检查。
     * @param cookie 必须的会话 Cookie
     * @param username 用户名
     * @param encryptedPassword MD5加密后的密码
     * @return 响应体字符串
     */
    public String checkPassword(String cookie, String username, String encryptedPassword) {
        String url = SINO_GRAIN_ORIGIN + "/slyyServlet/j_bsp_security_check";
        log.info("Checking password for user: {}", username);

        return HttpRequest.post(url)
                .headerMap(getDefaultHeaders(), true)
                .cookie(cookie)
                .contentType("application/x-www-form-urlencoded")
                .body("j_username=" + username + "&j_password=" + encryptedPassword + "&newReqCode=false&forUpdateSqm=false")
                .execute()
                .body();
    }


    // =====================================================================================
    // 2. 粮库信息查询 (Depot Information)
    // =====================================================================================

    /**
     * 【搜索粮库】接口: 根据经纬度、日期等信息搜索附近的粮库。
     * @param cookie 必须的会话 Cookie
     * @param params 包含所有表单参数的 Map (longitude, latitude, phone, sfz, rq, min, max, 等)
     * @return API响应的 JSON 字符串
     */
    public String searchDepots(String cookie, Map<String, Object> params) {
        String url = baseUrl + "/nhyy/getResvKdList";
        log.info("Searching depots with params: {}", params);
        return HttpRequest.post(url)
                .headerMap(getDefaultHeaders(), true)
                .cookie(cookie)
                .form(params)
                .execute()
                .body();
    }

    /**
     * 【获取粮库详情】接口: 获取指定粮库的详细信息，如可预约时段等。
     * @param cookie 必须的会话 Cookie
     * @param requestDto 包含请求参数的 DTO 对象 (请求体为 JSON)
     * @return API响应的 JSON 字符串
     */
    public String getDepotDetails(String cookie, DepotDetailRequestDto requestDto) {
        String url = baseUrl + "/nhyy/getResvSjList";
        String jsonBody = JSONUtil.toJsonStr(requestDto);
        log.info("Getting depot details with JSON body: {}", jsonBody);

        return HttpRequest.post(url)
                .headerMap(getDefaultHeaders(), true)
                .cookie(cookie)
                .body(jsonBody)
                .contentType("application/json;charset=UTF-8")
                .execute()
                .body();
    }

    /**
     * 【获取粮库可选表单 / 查询此粮库是否需要验证码】接口: 获取预约所需的详细配置信息。
     * 该接口也用于判断是否需要图片验证码。
     * @param cookie 必须的会话 Cookie
     * @param params 包含 'zznm', 'yypzmxnm', 'ywlx', 'yyfsnm' 等参数的 Map
     * @return API响应的 JSON 字符串
     */
    public String getAppointmentOptions(String cookie, Map<String, Object> params) {
        String url = baseUrl + "/nhyy/getResvMxList";
        log.info("Getting appointment options with params: {}", params);
        return HttpRequest.post(url)
                .headerMap(getDefaultHeaders(), true)
                .cookie(cookie)
                .form(params)
                .execute()
                .body();
    }

    /**
     * 【获取车船号】接口: 根据用户ID获取已绑定的车辆/船舶信息。
     * @param cookie 必须的会话 Cookie
     * @param userId 用户ID (通常是手机号)
     * @return API响应的 JSON 字符串
     */
    public String getVehicleInfoByUserId(String cookie, String userId) {
        String url = baseUrl + "/nhyy/getClxxByUserId";
        log.info("Getting vehicle info for user ID: {}", userId);
        return HttpRequest.post(url)
                .headerMap(getDefaultHeaders(), true)
                .cookie(cookie)
                .form("userId", userId)
                .form("devicetype", "weixin")
                .execute()
                .body();
    }

    // =====================================================================================
    // 3. 验证码服务 (Captcha Service)
    // =====================================================================================

    /**
     * 【获取图片验证码】接口: 获取一个用于计算的图片验证码。
     * @param cookie 必须的会话 Cookie
     * @return 验证码图片的字节数组 (byte[])
     */
    public byte[] getImageCaptcha(String cookie) {
        String url = baseUrl + "/nhyy/captchaMath?option=image&t=" + System.currentTimeMillis();
        log.info("Fetching image captcha.");
        return HttpRequest.get(url)
                .headerMap(getDefaultHeaders(), true)
                .cookie(cookie)
                .execute()
                .bodyBytes();
    }

    /**
     * 【验证图片验证码】接口: 提交计算结果以验证图片验证码是否正确。
     * @param cookie 必须的会话 Cookie
     * @param code 用户计算出的验证码结果
     * @return API响应的 JSON 字符串 (通常包含成功或失败的标志)
     */
    public String verifyImageCaptcha(String cookie, String code) {
        // 注意：URL中包含了查询参数
        String url = baseUrl + "/nhyy/captchaMath?option=chkCode&code=" + code;
        log.info("Verifying image captcha with code: {}", code);
        return HttpRequest.post(url)
                .headerMap(getDefaultHeaders(), true)
                .cookie(cookie)
                .form("devicetype", "weixin") // 请求体中只有一个参数
                .execute()
                .body();
    }

    /**
     * 【发送手机号验证码】接口: 向指定手机号发送短信验证码。
     * @param cookie 必须的会话 Cookie
     * @param params 包含 'phoneNum', 'zznm', 'uuid' 的参数 Map
     * @return API响应的 JSON 字符串
     */
    public String sendSmsCode(String cookie, Map<String, Object> params) {
        String url = baseUrl + "/smscheck/sendSMSCode";
        log.info("Sending SMS code to phone: {}", params.get("phoneNum"));
        return HttpRequest.post(url)
                .headerMap(getDefaultHeaders(), true)
                .cookie(cookie)
                .form(params)
                .execute()
                .body();
    }

    // =====================================================================================
    // 4. 核心业务 - 提交预约 (Core Business - Appointment Submission)
    // =====================================================================================

    /**
     * 【提交预约】接口: 提交所有信息，完成预约操作。
     * @param cookie 必须的会话 Cookie
     * @param params 包含所有预约信息的 Map (yyr, sfz, phone, pznm, zznm, rq, sl, cphStr 等)
     * @return API响应的 JSON 字符串
     */
    public String submitAppointment(String cookie, Map<String, Object> params) {
        String url = baseUrl + "/nhyy/reserve";
        log.info("Submitting appointment for user: {}", params.get("yyr"));
        return HttpRequest.post(url)
                .headerMap(getDefaultHeaders(), true)
                .cookie(cookie)
                .form(params)
                .execute()
                .body();
    }
}