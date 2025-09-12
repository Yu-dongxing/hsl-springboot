package com.wzz.hslspringboot.apis;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import com.wzz.hslspringboot.DTO.SearchResvKdListDTO;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.utils.EncryptionUtil;
import com.wzz.hslspringboot.utils.HttpRequestUtil;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * API 模块，封装对 "三农" App 后端服务的 HTTP 请求。
 */
@Component
public class Modules {
    private static final Logger log = LogManager.getLogger(Modules.class);

    // 建议将 HttpRequestUtil 实例化为成员变量，避免在每个方法中都 new 一个实例
//    private final HttpRequestUtil util = new HttpRequestUtil();
    private final EncryptionUtil encryptionUtil = new EncryptionUtil();

    @Autowired
    private HttpRequestUtil util;
    /**
     * 搜索粮库
     *   POST /slyyServlet/service/nhyy/getResvKdList   /slyyServlet/service/nhyy/getResvKdListBySearch
     *   接口ID：337567046
     *   接口地址：https://app.apifox.com/link/project/6966761/apis/api-337567046
     */
    public JSONObject search(UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil){
        SearchResvKdListDTO se = new SearchResvKdListDTO(userSmsWebSocket);
        return util.postForm("/slyyServlet/service/nhyy/getResvKdListBySearch",requestHeaderUtil,se.get());
    }

    /**
     * 粮库
     *   POST /slyyServlet/service/nhyy/getResvKdList
     *   接口ID：337567046
     */
    public JSONObject getResvKdList(UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil){
        SearchResvKdListDTO se = new SearchResvKdListDTO(userSmsWebSocket);
        return util.postForm("/slyyServlet/service/nhyy/getResvKdList",requestHeaderUtil,se.get());
    }


    /**
     * 获取randomcode
     *   POST /slyyServlet/service/nhyy/getRandomCode
     *   接口ID：339617614
     *   接口地址：https://app.apifox.com/link/project/6988845/apis/api-339617614
     */
    public JSONObject getRandomcode(UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil){
        Map<String, Object> params = new HashMap<>();
        params.put("phone", userSmsWebSocket.getUserPhone());
        params.put("devicetype", "weixin");
        log.info("获取随机数提交数据：{}",params);
        return util.postForm("/slyyServlet/service/nhyy/getRandomCode",requestHeaderUtil,params);
    }

    /**
     * POST /slyyServlet/service/grxx/getGrxxStatus
     */
    public JSONObject getGrxxStatus (UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil,PostPointmentDTO postPointmentDTO){
        Map<String, Object> params = new HashMap<>();
        params.put("userId", postPointmentDTO.getPhone());
        params.put("zznm",postPointmentDTO.getZznm());
        params.put("deviceType", "weixin");
        return util.postForm("/slyyServlet/service/grxx/getGrxxStatus",requestHeaderUtil,params);
    }

    /**
     * POST /slyyServlet/service/nhyy/hdmCheck
     */
    public JSONObject hdmCheck (UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil){
        Map<String, Object> params = new HashMap<>();
        params.put("sfz",userSmsWebSocket.getUserIdCard());
        params.put("deviceType", "weixin");
        return util.postForm("/slyyServlet/service/nhyy/hdmCheck",requestHeaderUtil,params);
    }

    /**
     * POST /slyyServlet/service/nhyy/getDistanceByCurrentLocation
     */
    public JSONObject getDistanceByCurrentLocation(String lkdm,String lklongitude, String lklatitude,UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil){
        Map<String, Object> params = new HashMap<>();
        params.put("longitude",userSmsWebSocket.getUserLongitude());
        params.put("latitude",userSmsWebSocket.getUserLatitude());
        params.put("lklongitude", lklongitude);
        params.put("lklatitude", lklatitude);
        params.put("lkdm", lkdm);
        params.put("deviceType", "weixin");
        return util.postForm("/slyyServlet/service/nhyy/getDistanceByCurrentLocation",requestHeaderUtil,params);
    }

    /**
     * 获取车船号
     * POST /slyyServlet/service/nhyy/getClxxByUserId
     * Postman 接口名: "获取车船号"
     * @param userSmsWebSocket 包含用户手机号
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject getVehicleInfoByUserId(UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> params = new HashMap<>();
        // Postman 中 body 的 key 是 userId，而不是 userPhone
        params.put("userId", userSmsWebSocket.getUserPhone());
        params.put("devicetype", "weixin");
        return util.postForm("/slyyServlet/service/nhyy/getClxxByUserId", requestHeaderUtil, params);
    }

    /**
     * 获取图片验证码
     * GET /slyyServlet/service/nhyy/captchaMath
     * Postman 接口名: "获取图片验证码"
     *
     * @param timestamp         时间戳, 对应 Postman 中的 t
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public byte[] getCaptchaImage(String timestamp, RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> params = new HashMap<>();
        params.put("option", "image");
        params.put("t", timestamp);
        // 对于 GET 请求，参数应该由 util 内部处理拼接到 URL 后
        return util.getCaptchaImage("/slyyServlet/service/nhyy/captchaMath", requestHeaderUtil, params);
    }

    /**
     * 验证图片验证码
     * POST /slyyServlet/service/nhyy/captchaMath
     * Postman 接口名: "验证图片验证码"
     * @param code 验证码
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject checkCaptchaCode(String code, RequestHeaderUtil requestHeaderUtil) {
        String url = "/slyyServlet/service/nhyy/captchaMath?option=chkCode&code=" + code;
        Map<String, Object> bodyParams = new HashMap<>();
        bodyParams.put("devicetype", "weixin");
        return util.postForm(url, requestHeaderUtil, bodyParams);
    }

    /**
     * 发送手机短信验证码
     * POST /slyyServlet/service/smscheck/sendSMSCode
     * Postman 接口名: "发送手机号验证码"
     * @param userSmsWebSocket 包含用户手机号
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject sendSMSCode(PostPointmentDTO yy,UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> params = new HashMap<>();
        params.put("phoneNum", userSmsWebSocket.getUserPhone());
        params.put("zznm", yy.getZznm());
        params.put("uuid", yy.getUuid());
        params.put("pzmxnm",yy.getPzmxnm());
        params.put("devicetype", "weixin");
        return util.postForm("/slyyServlet/service/smscheck/sendSMSCode", requestHeaderUtil, params);
    }

    /**
     * 提交预约
     * POST /slyyServlet/service/nhyy/reserve
     * Postman 接口名: "提交预约"
     * @param postPointmentDTO 预约信息的 DTO 对象
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject postReserve(PostPointmentDTO postPointmentDTO, RequestHeaderUtil requestHeaderUtil) throws JsonProcessingException {
        Map<String, Object> params = new HashMap<>();
        params.put("params", encryptionUtil.secretParams(postPointmentDTO));
        log.info("<data>:{},<header>:{}",params,requestHeaderUtil);
        return util.postForm("/slyyServlet/service/nhyy/reserve", requestHeaderUtil, params);
    }

    /**
     * 查询此粮库是否需要验证码
     * POST /slyyServlet/service/nhyy/getResvMxList
     * Postman 接口名: "获取粮库可选表单"
     * @param zznm 组织id
     * @param yypzmxnm 预约配置明细id
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject getResvMxList(String zznm, String yypzmxnm, RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> params = new HashMap<>();
        params.put("devicetype", "weixin");
        params.put("zznm", zznm);
        params.put("yypzmxnm", yypzmxnm);
        params.put("ywlx", "0");
        params.put("yyfsnm", "1");
        return util.postForm("/slyyServlet/service/nhyy/getResvMxList", requestHeaderUtil, params);
    }

    /**
     *获取详情
     * @param requestHeaderUtil
     * @param json
     * @return
     */
    public JSONObject getResvSjList(RequestHeaderUtil requestHeaderUtil,JSONObject json) {
        return util.postJson("/slyyServlet/service/nhyy/getResvSjList", requestHeaderUtil, json.toString());
    }


    /**
     * 更新 Cookie (登录)
     * POST /slyyServlet/j_bsp_security_check/mobile
     * Postman 接口名: "更新cookie"
     * @param userSmsWebSocket 包含用户名和密码
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject updateCookie(UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil) {
        EncryptionUtil encryptionUtil = new EncryptionUtil();
        String p = encryptionUtil.password(userSmsWebSocket.getUserPassword());

        Map<String, Object> params = new HashMap<>();
        params.put("j_username", userSmsWebSocket.getUserPhone());
        params.put("j_password", p);
        params.put("devicetype", "weixin");
        return util.postForm("/slyyServlet/j_bsp_security_check/mobile", requestHeaderUtil, params);
    }

    /**
     * 更新 验证码cookie (登录前)
     * POST /slyyServlet/service/login/insertTpYzm
     * Postman 接口名: "更新cookie"
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject insertTpYzm(RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionid", "");
        params.put("devicetype", "weixin");
        return util.postForm("/slyyServlet/service/login/insertTpYzm", requestHeaderUtil, params);
    }


    /**
     * 检验密码 (登录校验)
     * POST /slyyServlet/j_bsp_security_check
     * Postman 接口名: "检验密码"
     * @param userSmsWebSocket 包含用户名和密码
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject checkPassword(UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil) {
        EncryptionUtil encryptionUtil = new EncryptionUtil();
        String p = encryptionUtil.password(userSmsWebSocket.getUserPassword());

        String url = String.format("/slyyServlet/j_bsp_security_check?j_username=%s&j_password=%s&newReqCode=false&forUpdateSqm=false",
                userSmsWebSocket.getUserPhone(), p);
        // Postman 中此请求的 body 为 null (Content-Length: 0)
        return util.postForm(url, requestHeaderUtil, null);
    }
    /**
     * 图片验证码识别
     * @param imgBase64 图片的Base64编码字符串
     * @return 包含识别结果或错误信息的JSONObject
     */
    public JSONObject photeCodeOcr(String imgBase64) {
        // 定义OCR服务的URL
        String ocrServiceUrl = "http://127.0.0.1:8000/ocr";

        try {
            // 使用Hutool的HttpRequest发起POST请求
            // 链式调用，设置请求URL、表单参数和超时时间
            HttpResponse response = HttpRequest.post(ocrServiceUrl)
                    .form("image", imgBase64) // 设置表单参数，"image"为字段名，imgBase64为对应的值
                    .timeout(1000) // 设置超时时间，单位为毫秒，这里设置为20秒. [3]
                    .execute(); // 执行请求

            // 判断请求是否成功
            if (response.isOk()) {
                // 获取响应体
                String resultBody = response.body();
                // 将响应的JSON字符串解析为JSONObject
                return JSON.parseObject(resultBody);
            } else {
                // 如果请求失败，构建一个包含错误信息的JSONObject
                JSONObject errorResult = new JSONObject();
                errorResult.put("error_code", response.getStatus());
                errorResult.put("error_msg", "OCR service request failed with status: " + response.getStatus());
                return errorResult;
            }
        } catch (JSONException e) {
            // JSON解析异常处理
            JSONObject errorResult = new JSONObject();
            errorResult.put("error_code", 500);
            errorResult.put("error_msg", "Failed to parse response from OCR service.");
            // 可以在这里添加日志记录 e.printStackTrace();
            return errorResult;
        } catch (Exception e) {
            // 其他异常处理，例如网络连接异常
            JSONObject errorResult = new JSONObject();
            errorResult.put("error_code", 500);
            errorResult.put("error_msg", "An unexpected error occurred: " + e.getMessage());
            // 可以在这里添加日志记录 e.printStackTrace();
            return errorResult;
        }
    }
    /**
     * 获取验证码(响应滑块)
     * POST /slyyServlet/service/captcha/getCaptcha
     * 接口ID：340218781
     * 接口地址：https://app.apifox.com/link/project/6997949/apis/api-340218781
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject getSliderCaptcha(RequestHeaderUtil requestHeaderUtil) {
        // 根据文档，请求体为空对象
        return util.postJson("/slyyServlet/service/captcha/getCaptcha?yypzmxnm="+requestHeaderUtil.getPzmxnm()+"&phone="+requestHeaderUtil.getPhone(), requestHeaderUtil, "{}");
    }

    /**
     * 验证验证码(滑块)
     * POST https://hsn.sinograin.com.cn/slyyServlet//service/captcha/getCaptcha
     * 接口ID：340218782
     * 接口地址：https://app.apifox.com/link/project/6997949/apis/api-340218782
     * @param requestHeaderUtil 请求头工具
     * @param data 包含轨迹等信息的验证数据
     * @return JSONObject
     */
    public JSONObject checkSliderCaptcha(RequestHeaderUtil requestHeaderUtil, String data) {
        log.info("data: " + data);
        return util.postJson("/slyyServlet//service/captcha/checkCaptcha", requestHeaderUtil, data);
    }
    public JSONObject checkCphYycs(RequestHeaderUtil requestHeaderUtil, Map<String,Object> data) {
        log.info("data: " + data);
        return util.postForm("/slyyServlet/service/nhyy/checkCphYycs", requestHeaderUtil, data) ;
    }

    /**
     * 获取验证码(响应点击文字)
     * POST /slyyServlet/service/captcha/getCaptcha
     * 接口ID：340218783
     * 接口地址：https://app.apifox.com/link/project/6997949/apis/api-340218783
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject getWordClickCaptcha(RequestHeaderUtil requestHeaderUtil) {
        // 根据文档，此接口与获取滑块验证码的请求完全相同
        JSONObject data = new JSONObject();
        data.put("bgImageWidth", 242);
        return util.postJson("/slyyServlet/service/captcha/getCaptcha", requestHeaderUtil, data.toString());
    }


    /**
     * 验证验证码(点击文字)
     * POST /slyyServlet/service/captcha/checkCaptcha
     * 接口ID：340218784
     * 接口地址：https://app.apifox.com/link/project/6997949/apis/api-340218784
     * @param requestHeaderUtil 请求头工具
     * @param captchaId 验证码的唯一ID
     * @param data 包含点击坐标和轨迹等信息的验证数据
     * @return JSONObject
     */
    public JSONObject checkWordClickCaptcha(RequestHeaderUtil requestHeaderUtil, String captchaId, Map<String, Object> data) {
        // 根据文档，此接口与验证滑块验证码的请求结构完全相同
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", captchaId);
        payload.put("data", data);
        // 将Map对象序列化为JSON字符串作为请求体
        return util.postJson("/slyyServlet/service/captcha/checkCaptcha", requestHeaderUtil, JSON.toJSONString(payload));
    }


    /**
     * 上传车牌号
     */
    public JSONObject postCph(UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> params = new HashMap<>();
        params.put("nm", "");
        params.put("cclx",1);
        params.put("cph",userSmsWebSocket.getVehicleLicensePlateNumber());
        params.put("clzz",userSmsWebSocket.getFoodOfGrainNum());
        params.put("xszurl","");
        params.put("clurl","");
        params.put("userId",userSmsWebSocket.getUserPhone());
        params.put("devicetype", "weixin");
        return util.postForm("/slyyServlet/service/clxxwh/save", requestHeaderUtil, params);
    }

    /**
     * 上传数据
     *
     * @param data 业务数据部分的JSON对象
     * @param hd   另一部分业务数据的JSON对象
     * @param url  请求的目标URL地址
     * @return 服务器响应的JSON对象，如果请求失败则返回null或一个包含错误信息的JSON对象
     */
    public JSONObject postData(JSONObject data, JSONObject hd, String url) {
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("data", data);
        requestPayload.put("hd", hd);
        String requestBody = requestPayload.toJSONString();
        try {
            HttpResponse response = HttpRequest.post(url)
                    .body(requestBody, "application/json;charset=UTF-8")
                    .timeout(2000)
                    .execute();
            if (response.isOk()) {
                String responseBody = response.body();
                return JSON.parseObject(responseBody);
            } else {
                System.err.println("请求失败，状态码: " + response.getStatus());
                System.err.println("响应内容: " + response.body());
                JSONObject errorResult = new JSONObject();
                errorResult.put("success", false);
                errorResult.put("statusCode", response.getStatus());
                errorResult.put("message", "请求服务器失败");
                errorResult.put("details", response.body());
                return errorResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject exceptionResult = new JSONObject();
            exceptionResult.put("success", false);
            exceptionResult.put("message", "请求过程中发生异常");
            exceptionResult.put("error", e.getMessage());
            return exceptionResult;
        }
    }



}