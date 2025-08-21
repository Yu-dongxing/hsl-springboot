package com.wzz.hslspringboot.apis;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import com.wzz.hslspringboot.DTO.SearchResvKdListDTO;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.utils.EncryptionUtil;
import com.wzz.hslspringboot.utils.HttpRequestUtil;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * API 模块，封装对 "三农" App 后端服务的 HTTP 请求。
 */
@Component
public class Modules {

    // 建议将 HttpRequestUtil 实例化为成员变量，避免在每个方法中都 new 一个实例
//    private final HttpRequestUtil util = new HttpRequestUtil();


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
     * 获取randomcode
     *   POST /slyyServlet/service/nhyy/getRandomCode
     *   接口ID：339617614
     *   接口地址：https://app.apifox.com/link/project/6988845/apis/api-339617614
     */
    public JSONObject getRandomcode(UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil){
        Map<String, Object> params = new HashMap<>();
        params.put("phone", userSmsWebSocket.getUserPhone());
        params.put("devicetype", "weixin");
        return util.postForm("/slyyServlet/service/nhyy/getRandomCode",requestHeaderUtil,params);
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
     * @param timestamp 时间戳, 对应 Postman 中的 t
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject getCaptchaImage(String timestamp, RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> params = new HashMap<>();
        params.put("option", "image");
        params.put("t", timestamp);
        // 对于 GET 请求，参数应该由 util 内部处理拼接到 URL 后
        return util.get("/slyyServlet/service/nhyy/captchaMath", requestHeaderUtil, params);
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
     * @param zznm 组织内码 (根据接口推断)
     * @param uuid UUID
     * @param userSmsWebSocket 包含用户手机号
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject sendSMSCode(String zznm, String uuid, UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> params = new HashMap<>();
        params.put("phoneNum", userSmsWebSocket.getUserPhone());
        params.put("zznm", zznm);
        params.put("uuid", uuid);
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
    public JSONObject postReserve(PostPointmentDTO postPointmentDTO, RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> params = BeanUtil.beanToMap(postPointmentDTO);
        return util.postForm("/slyyServlet/service/nhyy/reserve", requestHeaderUtil, params);
    }

    /**
     * 获取粮库可选表单 查询此粮库是否需要验证码
     * POST /slyyServlet/service/nhyy/getResvMxList
     * Postman 接口名: "获取粮库可选表单"
     * @param zznm 组织内码
     * @param yypzmxnm 预约配置明细内码
     * @param ywlx 业务类型
     * @param yyfsnm 预约方式内码
     * @param requestHeaderUtil 请求头工具
     * @return JSONObject
     */
    public JSONObject getResvMxList(String zznm, String yypzmxnm, String ywlx, String yyfsnm, RequestHeaderUtil requestHeaderUtil) {
        Map<String, Object> params = new HashMap<>();
        params.put("devicetype", "weixin");
        params.put("zznm", zznm);
        params.put("yypzmxnm", yypzmxnm);
        params.put("ywlx", ywlx);
        params.put("yyfsnm", yyfsnm);
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
}