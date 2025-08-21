package com.wzz.hslspringboot.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import com.wzz.hslspringboot.apis.Function;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.AppointmentProcessorService;
import com.wzz.hslspringboot.utils.EncryptionUtil;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 预约处理器服务
 * 封装了完整的单次预约逻辑，设计为线程安全，可供多线程调用。
 */
@Service
public class AppointmentProcessorServiceImpl implements AppointmentProcessorService {


    private static final Logger log = LogManager.getLogger(AppointmentProcessorServiceImpl.class);
    @Autowired
    private Function function;

    // EncryptionUtil 是一个工具类，如果它内部没有状态，可以直接new。
    // 如果它也是一个Spring Bean，则应该使用 @Autowired 注入。这里假设它是一个无状态工具类。
    private final EncryptionUtil encryptionUtil = new EncryptionUtil();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     /**
     * 为单个用户执行完整的预约流程 (包含验证码处理)。
     * 此方法是线程安全的。
     *
     * @param user 用户信息对象
     * @return 提交预约后返回的JSONObject，如果过程中发生错误则返回null
     */
    @Override
    public JSONObject processAppointment(UserSmsWebSocket user) {
        if (user == null) {

            log.error("传入的用户信息为空，无法处理。");
            return null;
        }

        log.info("开始为用户手机号: {} 处理预约流程...", user.getUserPhone());

        // 1. 初始化
        RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(user);
        PostPointmentDTO postPointmentDTO = new PostPointmentDTO();

        // 2. 获取并设置用户信息
        if (!populateUserInfo(postPointmentDTO, user, requestHeaderUtil)) {
            return null;
        }

        // 3. 获取并设置车辆信息
        if (!populateVehicleInfo(postPointmentDTO, user, requestHeaderUtil)) {
            return null;
        }

        // 4. 处理验证码并获取短信
//        String smsCode = handleVerificationAndGetSms(user, requestHeaderUtil);

        String smsCode = "";

        if (StrUtil.isBlank(smsCode)) {
            log.error("获取短信验证码失败，中断用户 {} 的预约流程。", user.getUserPhone());
            return null;
        }
        log.info("成功获取到手机号 {} 的短信验证码。", user.getUserPhone());

        // 5. 搜索粮库信息并设置
        if (!populateDepotInfo(postPointmentDTO, user, requestHeaderUtil)) {
            return null;
        }

        // 6. 设置其他固定的和动态的参数
        setupDefaultParameters(postPointmentDTO, user, smsCode);

        // 7. 加密并提交数据
        try {
            String encryptedData = encryptionUtil.rsa(postPointmentDTO);
            postPointmentDTO.setSecretData(encryptedData);

            log.info("最终提交的DTO (手机号: {}): {}", user.getUserPhone(), objectMapper.writeValueAsString(postPointmentDTO));

            JSONObject result = function.postInfo(requestHeaderUtil, postPointmentDTO);
            log.info("手机号: {} 的预约提交结果: {}", user.getUserPhone(), result);
            return result;

        } catch (JsonProcessingException e) {
            log.error("DTO对象转JSON字符串失败，用户手机号: {}", user.getUserPhone(), e);
            return null;
        } catch (Exception e) {
            log.error("处理预约时发生未知异常，用户手机号: {}", user.getUserPhone(), e);
            return null;
        }
    }



    /**
     * [占位符方法] 识别图片验证码
     * 你需要在此处实现对接第三方打码平台API的逻辑
     *
     * @param imageBase64 图片的Base64编码字符串
     * @return 识别出的验证码字符串
     */
    private String solveCaptcha(String imageBase64) {
        // ====================== 真实逻辑替换区域 ======================
        // 1. 构建请求体，将 imageBase64 发送到打码平台API
        // 2. 轮询或等待打码平台返回识别结果
        // 3. 返回识别结果字符串
        // 示例: return DamatuApi.solve(imageBase64);
        // ===========================================================

        // 当前为模拟返回，用于流程测试
        log.warn("注意：当前正在使用模拟的图片验证码识别！");
        return "8888"; // 假设识别结果总是 "8888"
    }

    /**
     * [占位符方法] 获取短信验证码
     * 你需要在此处实现对接接码平台、Redis或数据库的逻辑
     *
     * @param phone 接收短信的手机号
     * @return 最新的短信验证码
     */
    private String retrieveSmsCode(String phone) {
        // ====================== 真实逻辑替换区域 ======================
        // 1. 调用接码平台API，根据手机号获取最新一条短信
        // 2. 从短信内容中用正则表达式提取出6位数字验证码
        // 3. 返回验证码
        // 示例: return SmsReceiverApi.getLatestCode(phone);

        // 或者从 Redis 中读取
        // 示例: return redisTemplate.opsForValue().get("sms_code:" + phone);
        // ===========================================================

        // 当前为模拟返回，用于流程测试
        log.warn("注意：当前正在使用模拟的短信验证码获取！");
        return "260287"; // 假设获取到的验证码总是 "260287"
    }

    /**
     * 填充用户信息到DTO
     */
    private boolean populateUserInfo(PostPointmentDTO dto, UserSmsWebSocket user, RequestHeaderUtil headers) {
        JSONObject response = function.checkCookieAndGetResponse(user, headers);
        if (response == null || !response.getBooleanValue("success")) {
            log.error("获取用户信息失败, 手机号: {}, 响应: {}", user.getUserPhone(), response);
            return false;
        }

        JSONObject userObject = response.getJSONObject("data").getJSONObject("user");
        if (userObject == null) {
            log.error("响应中未找到用户信息, 手机号: {}", user.getUserPhone());
            return false;
        }

        String userNm = userObject.getString("userNm");
        String userType = userObject.getString("userType");

        dto.setTjr(userNm);
        dto.setUserType(userType);
        dto.setCs(user.getFoodOfGrainNum());
        dto.setCyr(user.getUserName());
        dto.setJsr(user.getUserName());
        dto.setSfz(user.getUserIdCard());

        // 处理 mobileDeviceId
        String mobileDeviceIdStr = headers.getMobileDeviceId();
        if (!StrUtil.hasBlank(mobileDeviceIdStr)) {
            String mobileDeviceId = StrUtil.split(mobileDeviceIdStr, '=').get(1);
            dto.setMobileDeviceId(mobileDeviceId);
            dto.setOpenId(mobileDeviceId);
        }
        return true;
    }

    /**
     * 填充车辆信息到DTO
     */
    private boolean populateVehicleInfo(PostPointmentDTO dto, UserSmsWebSocket user, RequestHeaderUtil headers) {
        JSONObject response = function.getLicensePlateId(headers, user);


        JSONArray dataArray = response.getJSONArray("data");
        if (dataArray == null || dataArray.isEmpty()) {
            log.warn("用户 {} 的车牌号列表为空。", user.getUserPhone());
            return false;
        }

        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject vehicleInfo = dataArray.getJSONObject(i);
            if (user.getVehicleLicensePlateNumber().equals(vehicleInfo.getString("cph"))) {
                String cphlx = vehicleInfo.getString("cclx");
                dto.setCllxNm(cphlx);
                dto.setCphStr(user.getVehicleLicensePlateNumber());
                log.info("为用户 {} 找到匹配的车辆信息: {}", user.getUserPhone(), vehicleInfo);
                return true;
            }
        }

        log.error("未在列表中找到用户 {} 匹配的车牌号: {}", user.getUserPhone(), user.getVehicleLicensePlateNumber());
        return false;
    }

    /**
     * 填充粮库及预约信息到DTO
     */
    private boolean populateDepotInfo(PostPointmentDTO dto, UserSmsWebSocket user, RequestHeaderUtil headers) {
        JSONObject response = function.search(user, headers);
        if (response == null || !"200".equals(response.getString("code"))) {
            log.error("搜索粮库信息失败, 手机号: {}, 响应: {}", user.getUserPhone(), response);
            return false;
        }

        JSONArray dataArray = response.getJSONArray("data");
        if (dataArray == null || dataArray.isEmpty()) {
            log.error("搜索粮库信息结果为空, 手机号: {}", user.getUserPhone());
            return false;
        }

        JSONObject data = dataArray.getJSONObject(0); // 假设总是取第一个
        JSONArray yypzmxList = data.getJSONArray("yypzmxList");
        if (yypzmxList == null || yypzmxList.isEmpty()) {
            log.error("粮库预约配置列表为空, 手机号: {}", user.getUserPhone());
            return false;
        }
        JSONObject firstTimeSlot = yypzmxList.getJSONObject(0);

        dto.setZzmc(data.getString("zzmc"));
        dto.setZznm(data.getString("zznm"));
        dto.setLatitude(data.getString("latitude"));
        dto.setLongitude(data.getString("longitude"));
        dto.setLxfs(data.getString("lxfs"));
        dto.setRq(data.getString("rq"));
        dto.setPznm(data.getString("yypznm"));
        dto.setKssj(firstTimeSlot.getString("kssj"));
        dto.setJssj(firstTimeSlot.getString("jssj"));
        dto.setPzmxnm(firstTimeSlot.getString("yypzmxnm"));

        // 解析并匹配粮食品种
        JSONArray lspzArray = JSON.parseArray(data.getString("lspz"));
        boolean grainMatched = false;
        if (lspzArray != null) {
            for (int i = 0; i < lspzArray.size(); i++) {
                JSONObject grain = lspzArray.getJSONObject(i);
                if (user.getGrainVarieties().equals(grain.getString("name"))) {
                    dto.setLsmc(grain.getString("name"));
                    dto.setLsnm(grain.getString("nm"));
                    grainMatched = true;
                    break;
                }
            }
        }
        if (!grainMatched) {
            log.error("未找到匹配的粮食品种: {}, 手机号: {}", user.getGrainVarieties(), user.getUserPhone());
            return false;
        }

        return true;
    }

    /**
     * 设置其余默认或动态参数
     */
    private void setupDefaultParameters(PostPointmentDTO dto, UserSmsWebSocket user, String smsCode) {
        dto.setSl(user.getFoodOfGrainNum());
        dto.setPhone(user.getUserPhone());
        dto.setYyr(user.getUserName());
        dto.setYwlx("0");
        dto.setCyrnm("");
        dto.setTzdbh("");
        dto.setCyr("");
        dto.setCyrsfzh("");
        dto.setQymc("");
        dto.setXydm("");
        dto.setWxdlFlag("true");
        dto.setYyfsnm("1");
        dto.setYyfsmc("预约挂号");
        dto.setDevicetype("weixin");
        dto.setUuid("01a7baa260d04c8c85bd3b2488187767"); // 这个UUID可能是固定的，如果不是，也需要动态传入
        dto.setZldd("");
        dto.setCyrsjh("");
        dto.setDxyzm(smsCode); // 使用传入的短信验证码
    }
}