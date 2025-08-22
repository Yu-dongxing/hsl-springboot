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

    private final EncryptionUtil encryptionUtil = new EncryptionUtil();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        if (!populateUserInfo(postPointmentDTO, user, requestHeaderUtil)) return null;

        // 3. 获取并设置车辆信息
        if (!populateVehicleInfo(postPointmentDTO, user, requestHeaderUtil)) return null;

        // 4. 搜索粮库信息并设置
        if (!populateDepotInfo(postPointmentDTO, user, requestHeaderUtil)) return null;

        // 5. 获取随机码并附加到联系方式
        if (!appendRandomCodeToContact(postPointmentDTO, user, requestHeaderUtil)) return null;

        // 6. 处理图片验证码 (总是执行)
        if (!handleImageCaptcha(requestHeaderUtil, user)) {
            log.error("图片验证码处理失败，中断用户 {} 的预约流程。", user.getUserPhone());
            return null;
        }
        log.info("手机号: {} 的图片验证码流程完成。", user.getUserPhone());

        // 7. 按需处理并获取短信验证码
        String smsCode = getSmsCodeIfNeeded(postPointmentDTO, user, requestHeaderUtil);
        if (smsCode == null) { // 返回 null 表示在需要短信的情况下，获取失败
            log.error("短信验证码处理流程失败，中断用户 {} 的预约流程。", user.getUserPhone());
            return null;
        }

        // 8. 设置其他固定的和动态的参数
        setupDefaultParameters(postPointmentDTO, user, smsCode);

        // 9. 加密并提交数据
        try {
            String encryptedData = encryptionUtil.rsa(postPointmentDTO);
            postPointmentDTO.setSecretData(encryptedData);

            log.info("最终提交的DTO (手机号: {}): {}", user.getUserPhone(), objectMapper.writeValueAsString(postPointmentDTO));

//            JSONObject result = function.postInfo(requestHeaderUtil, postPointmentDTO);
            JSONObject result = null;
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
     */
    private String solveCaptcha(String imageBase64) {
        log.warn("注意：当前正在使用模拟的图片验证码识别！");
        return "9"; // 假设识别结果总是 "9"
    }

    /**
     * [占位符方法] 获取短信验证码
     */
    private String retrieveSmsCode(String phone) {
        log.warn("注意：当前正在使用模拟的短信验证码获取！");
        return "260287"; // 假设获取到的验证码总是 "260287"
    }

    /**
     * 封装图片验证码的完整处理流程（获取、识别、验证）。
     * @return boolean true表示成功, false表示失败
     */
    private boolean handleImageCaptcha(RequestHeaderUtil headers, UserSmsWebSocket user) {
        // 1. 获取图片验证码
        String photoCatchResponse = function.getPhotoCatch(headers);
        // 假设图片base64在 "data" -> "imageBase64" 字段中，请根据实际情况修改
        String imageBase64 = photoCatchResponse;
        log.info("获取图片验证码成功, 手机号: {}", user.getUserPhone());

        // 2. 调用打码平台识别图片验证码
        String captchaCode = solveCaptcha(imageBase64);
        if (StrUtil.isBlank(captchaCode)) {
            log.error("图片验证码识别失败 (结果为空), 手机号: {}", user.getUserPhone());
            return false;
        }
        log.info("图片验证码识别结果: {}", captchaCode);

        // 3. 验证图片验证码
        JSONObject checkPhotoResponse = function.checkPhoteCatch(captchaCode, headers);
        // 假设成功的响应中包含 success:true
        if (checkPhotoResponse == null || !checkPhotoResponse.getBooleanValue("success")) {
            log.error("验证图片验证码失败, 手机号: {}, 响应: {}", user.getUserPhone(), checkPhotoResponse);
            return false;
        }
        log.info("图片验证码校验成功, 手机号: {}", user.getUserPhone());
        return true;
    }

    /**
     * 检查是否需要短信，如果需要则完成发送和获取的流程。
     * @return 返回短信验证码。如果不需要短信，则返回空字符串""。如果流程出错，则返回null。
     */
    private String getSmsCodeIfNeeded(PostPointmentDTO dto, UserSmsWebSocket user, RequestHeaderUtil headers) {
        // 1. 检查是否需要短信验证码
        JSONObject smsBoolesResponse = function.getSmsBooles(dto, headers);
        if (smsBoolesResponse == null || smsBoolesResponse.getJSONObject("data") == null) {
            log.error("检查是否需要短信验证码的请求失败, 手机号: {}, 响应: {}", user.getUserPhone(), smsBoolesResponse);
            return null;
        }
        boolean needsSms = smsBoolesResponse.getJSONObject("data").getBooleanValue("needsms");
        log.info("用户 {} 是否需要短信验证码: {}", user.getUserPhone(), needsSms);

        if (!needsSms) {
            return ""; // 不需要短信，直接返回空字符串
        }

        // --- 以下是需要短信验证的流程 ---

        // 2. 发送手机验证码
        JSONObject sendSmsResponse = function.sendSmsCode(dto, headers, user);
        if (sendSmsResponse == null || !sendSmsResponse.getBooleanValue("success")) {
            log.error("发送手机验证码失败, 手机号: {}, 响应: {}", user.getUserPhone(), sendSmsResponse);
            return null;
        }
        log.info("发送手机验证码请求成功, 手机号: {}", user.getUserPhone());

        // 3. 从接码平台或缓存中获取短信验证码
        String smsCode = retrieveSmsCode(user.getUserPhone());
        if (StrUtil.isBlank(smsCode)) {
            log.error("从外部平台获取短信验证码失败(结果为空), 手机号: {}", user.getUserPhone());
            return null;
        }
        log.info("成功获取到手机号 {} 的短信验证码。", user.getUserPhone());

        return smsCode;
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
        if (response == null || !response.containsKey("data")) {
            log.error("获取车辆信息失败, 手机号: {}, 响应: {}", user.getUserPhone(), response);
            return false;
        }

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
        if (response == null || !"1".equals(response.getString("retCode"))) {
            log.error("搜索粮库信息失败, 手机号: {}, 响应: {}", user.getUserPhone(), response);
            return false;
        }

        JSONArray dataArray = response.getJSONArray("data");
        if (dataArray == null || dataArray.isEmpty()) {
            log.error("搜索粮库信息结果为空, 手机号: {}", user.getUserPhone());
            return false;
        }

        JSONObject data = dataArray.getJSONObject(0);
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
     * 获取随机码并附加到联系方式字段
     */
    private boolean appendRandomCodeToContact(PostPointmentDTO dto, UserSmsWebSocket user, RequestHeaderUtil headers) {
        JSONObject response = function.getRandomcode(user, headers);
        if (response == null || response.getJSONObject("data") == null) {
            log.error("获取随机码失败, 手机号: {}, 响应: {}", user.getUserPhone(), response);
            return false;
        }
        String randomCode = response.getJSONObject("data").getString("randomCode");
        log.info("成功获取到 randomCode: {}", randomCode);

        dto.setLxfs(dto.getLxfs() + "_" + randomCode);
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
        dto.setUuid("01a7baa260d04c8c85bd3b2488187767");
        dto.setZldd("");
        dto.setCyrsjh("");
        dto.setDxyzm(smsCode);
    }
}