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

}
