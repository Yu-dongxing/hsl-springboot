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
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import com.wzz.hslspringboot.utils.EncryptionUtil;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 预约处理器服务
 * 封装了完整的单次预约逻辑，设计为线程安全，可供多线程调用。
 */
@Service
public class AppointmentProcessorServiceImpl implements AppointmentProcessorService {


    private static final Logger log = LogManager.getLogger(AppointmentProcessorServiceImpl.class);

    @Autowired
    private Function function;


    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    // 使用类成员变量，避免在方法内重复创建对象
    private final EncryptionUtil encryptionUtil = new EncryptionUtil();
    private final ObjectMapper objectMapper = new ObjectMapper();


    // 定义预约时间的格式
    private static final DateTimeFormatter APPOINTMENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 为单个用户执行完整的预约流程 (包含验证码处理)。
     * 此方法是线程安全的。
     *
     * @param user 用户信息对象, 包含了执行预约所需的所有信息，包括最新的短信验证码和时间
     * @return 提交预约后返回的JSONObject，如果过程中发生错误则返回null
     */
    @Override
    public JSONObject processAppointment(UserSmsWebSocket user) {
        log.info("开始为用户【{}】执行完整的预约流程...", user.getUserName());

        try {
            // 1. 初始化请求头和数据传输对象(DTO)
            RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(user);
            PostPointmentDTO postPointmentDTO = new PostPointmentDTO();

            // 2. 获取并设置用户信息
            log.info("步骤 1/8: 获取用户信息...");
            JSONObject userInfoResponse = function.checkCookieAndGetResponse(user, requestHeaderUtil);
            if (userInfoResponse == null || !userInfoResponse.getBooleanValue("success")) {
                log.error("用户【{}】获取用户信息失败或会话无效: {}", user.getUserName(), userInfoResponse);
                return null;
            }
            JSONObject userData = userInfoResponse.getJSONObject("data").getJSONObject("user");
            String userNm = userData.getString("userNm");
            String userType = userData.getString("userType");
            postPointmentDTO.setTjr(userNm);
            postPointmentDTO.setUserType(userType);
            postPointmentDTO.setCs(String.valueOf(user.getNumberOfVehiclesAndShips()));
            postPointmentDTO.setCyr(user.getUserName());
            postPointmentDTO.setJsr(user.getUserName());
            postPointmentDTO.setSfz(user.getUserIdCard());

            String mobileDeviceIdStr = requestHeaderUtil.getMobileDeviceId();
            if (!StrUtil.hasBlank(mobileDeviceIdStr)) {
                String mobileDeviceId = mobileDeviceIdStr.split("=").length > 1 ? mobileDeviceIdStr.split("=")[1] : "";
                postPointmentDTO.setMobileDeviceId(mobileDeviceId);
                postPointmentDTO.setOpenId(mobileDeviceId);
            }
            log.info("用户【{}】获取用户信息成功: userNm={}, userType={}", user.getUserName(), userNm, userType);

            // 3. 获取并设置车辆信息
            log.info("步骤 2/8: 获取车辆信息...");
            JSONObject licensePlateResponse = function.getLicensePlateId(requestHeaderUtil, user);
            if (licensePlateResponse == null ) {
                log.error("用户【{}】获取车牌信息失败: {}", user.getUserName(), licensePlateResponse);
                return null;
            }
            JSONArray vehicleArray = licensePlateResponse.getJSONArray("data");
            boolean vehicleFound = false;
            for (int i = 0; i < vehicleArray.size(); i++) {
                JSONObject vehicleInfo = vehicleArray.getJSONObject(i);
                if (user.getVehicleLicensePlateNumber().equals(vehicleInfo.getString("cph"))) {
                    String cclx = vehicleInfo.getString("cclx");
                    postPointmentDTO.setCllxNm(cclx);
                    postPointmentDTO.setCphStr(user.getVehicleLicensePlateNumber() + ",");
                    vehicleFound = true;
                    log.info("用户【{}】匹配到车辆信息: 车牌号={}, 车辆类型={}", user.getUserName(), user.getVehicleLicensePlateNumber(), cclx);
                    break;
                }
            }
            if (!vehicleFound) {
                log.error("用户【{}】未在用户车辆列表中找到匹配的车牌号: {}", user.getUserName(), user.getVehicleLicensePlateNumber());
                return null;
            }

            // 4. 搜索预约库点和时间信息
            log.info("步骤 3/8: 搜索预约库点信息...");
            JSONObject searchResponse = function.search(user, requestHeaderUtil);
            if (searchResponse == null  || searchResponse.getJSONArray("data").isEmpty()) {
                log.error("用户【{}】搜索预约库点信息失败或未找到可用库点: {}", user.getUserName(), searchResponse);
                return null;
            }
            JSONObject depotData = searchResponse.getJSONArray("data").getJSONObject(0);
            postPointmentDTO.setZzmc(depotData.getString("zzmc"));
            postPointmentDTO.setZznm(depotData.getString("zznm"));
            postPointmentDTO.setLatitude(depotData.getString("latitude"));
            postPointmentDTO.setLongitude(depotData.getString("longitude"));
            postPointmentDTO.setLxfs(depotData.getString("lxfs"));
            String rq = depotData.getString("rq");
            postPointmentDTO.setRq(rq);
            postPointmentDTO.setPznm(depotData.getString("yypznm")); // 注意这里的字段名映射

            // 解析具体时间段
            JSONArray timeSlotArray = depotData.getJSONArray("yypzmxList");
            if (timeSlotArray.isEmpty()) {
                log.error("用户【{}】的预约库点 {} 没有可用的时间段", user.getUserName(), depotData.getString("zzmc"));
                return null;
            }
            JSONObject firstTimeSlot = timeSlotArray.getJSONObject(0);
            postPointmentDTO.setKssj(firstTimeSlot.getString("kssj"));
            postPointmentDTO.setJssj(firstTimeSlot.getString("jssj"));
            postPointmentDTO.setPzmxnm(firstTimeSlot.getString("yypzmxnm"));
            log.info("用户【{}】获取到预约库点: {}, 日期: {}, 时间: {}-{}", user.getUserName(), depotData.getString("zzmc"), rq, firstTimeSlot.getString("kssj"), firstTimeSlot.getString("jssj"));


            // 5. 解析并设置粮食品种
            log.info("步骤 4/8: 设置粮食品种...");
            JSONArray grainArray = JSON.parseArray(depotData.getString("lspz"));
            boolean grainFound = false;
            if (grainArray != null && !grainArray.isEmpty()) {
                for (int i = 0; i < grainArray.size(); i++) {
                    JSONObject grain = grainArray.getJSONObject(i);
                    if (user.getGrainVarieties().equals(grain.getString("name"))) {
                        postPointmentDTO.setLsmc(grain.getString("name"));
                        postPointmentDTO.setLsnm(grain.getString("nm"));
                        grainFound = true;
                        log.info("用户【{}】匹配到粮食品种: {}", user.getUserName(), grain.getString("name"));
                        break;
                    }
                }
            }
            if (!grainFound) {
                log.error("用户【{}】未在库点粮食品种列表中找到匹配项: {}", user.getUserName(), user.getGrainVarieties());
                return null;
            }

            // 6. 获取随机数并更新联系方式
            log.info("步骤 5/8: 获取随机码...");
            JSONObject randomCodeResponse = function.getRandomcode(user, requestHeaderUtil);
            if (randomCodeResponse == null ) {
                log.error("用户【{}】获取随机码失败: {}", user.getUserName(), randomCodeResponse);
                return null;
            }
            String randomCode = randomCodeResponse.getJSONObject("data").getString("randomCode");
            postPointmentDTO.setLxfs(user.getUserPhone() + "_" + randomCode);
            log.info("用户【{}】获取随机码成功: {}", user.getUserName(), randomCode);

            // 7. 处理短信验证码
            log.info("步骤 6/8: 处理短信验证码...");
            JSONObject checkRe = function.checkData(postPointmentDTO, requestHeaderUtil, user);
            if (!checkRe.getBoolean("needsms")) {
                postPointmentDTO.setDxyzm("");
            } else {
                // 需要短信验证码的逻辑
                UserSmsWebSocket ua = userSmsWebSocketService.ByUserPhoneSelect(postPointmentDTO.getPhone());
                if (ua != null && ua.getUpSmsTime() != null) {
                    LocalDateTime upSmsTime = ua.getUpSmsTime();
                    LocalDateTime now = LocalDateTime.now();
                    Duration duration = Duration.between(upSmsTime, now);
                    long secondsDifference = duration.getSeconds();

                    if (secondsDifference > 30) {
                        log.warn("用户【{}】的验证码已超过30秒有效期，将清空验证码。", user.getUserName());
                        postPointmentDTO.setDxyzm("");
                    } else {
                        postPointmentDTO.setDxyzm(ua.getUserSmsMessage());
                        log.info("用户【{}】成功获取到有效期内的验证码。", user.getUserName());
                    }
                } else {
                    log.warn("用户【{}】需要验证码，但数据库中未找到或时间为空。", user.getUserName());
                    postPointmentDTO.setDxyzm("");
                }
            }


            // 8. 填充其余固定或默认的DTO字段
            postPointmentDTO.setSl(user.getFoodOfGrainNum());
            postPointmentDTO.setPhone(user.getUserPhone());
            postPointmentDTO.setYyr(user.getUserName());
            postPointmentDTO.setYwlx("0");
            postPointmentDTO.setCyrnm("");
            postPointmentDTO.setTzdbh("");
            postPointmentDTO.setCyr("");
            postPointmentDTO.setCyrsfzh("");
            postPointmentDTO.setQymc("");
            postPointmentDTO.setXydm("");
            postPointmentDTO.setWxdlFlag("true");
            postPointmentDTO.setYyfsnm("1");
            postPointmentDTO.setYyfsmc("预约挂号");
            postPointmentDTO.setDevicetype("weixin");
            postPointmentDTO.setZldd("");
            postPointmentDTO.setCyrsjh("");

            // 9. 加密最终数据
            log.info("步骤 7/8: 对预约数据进行加密...");
            String secretData = encryptionUtil.rsa(postPointmentDTO);
            postPointmentDTO.setSecretData(secretData);

            try {
                log.debug("用户【{}】最终提交的DTO（加密前）: {}", user.getUserName(), objectMapper.writeValueAsString(postPointmentDTO));
            } catch (JsonProcessingException e) {
                log.warn("用户【{}】序列化DTO用于调试日志时失败", user.getUserName(), e);
            }

            // ==================== 10. 定时提交预约 (核心修改部分) ====================
            log.info("用户【{}】进入步骤 8/8: 准备定时提交最终预约请求...", user.getUserName());

            // 10.1 解析预约时间字符串
            LocalDateTime appointmentDateTime;
            try {
                appointmentDateTime = LocalDateTime.parse(user.getAppointmentTime(), APPOINTMENT_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                log.error("用户【{}】解析预约时间 '{}' 失败，请检查时间格式是否为 'yyyy-MM-dd HH:mm:ss'", user.getUserName(), user.getAppointmentTime(), e);
                return null; // 时间格式错误，直接结束任务
            }

            // 10.2 计算等待时间并休眠
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(appointmentDateTime)) {
                Duration duration = Duration.between(now, appointmentDateTime);
                long millisToWait = duration.toMillis();
                if (millisToWait > 0) {
                    log.info("用户【{}】的预约时间为 {}，当前时间为 {}，需等待 {} 秒后执行提交。",
                            user.getUserName(),
                            appointmentDateTime.format(APPOINTMENT_TIME_FORMATTER),
                            now.format(APPOINTMENT_TIME_FORMATTER),
                            millisToWait / 1000.0);

                    // 休眠直到指定时间
                    Thread.sleep(millisToWait);
                }
            }

            // 10.3 执行最终的提交请求
            log.info("用户【{}】已到达指定预约时间，开始执行提交操作...", user.getUserName());
            JSONObject submissionResponse = function.postInfo(requestHeaderUtil, postPointmentDTO);
            log.info("用户【{}】预约流程执行完毕，提交结果: {}", user.getUserName(), submissionResponse != null ? submissionResponse.toJSONString() : "null");

            return submissionResponse;

        } catch (InterruptedException e) {
            // 当Thread.sleep被中断时，捕获此异常
            log.warn("用户【{}】的预约任务在等待过程中被中断。", user.getUserName());
            // 恢复中断状态，这是良好的多线程编程实践
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.error("为用户【{}】执行预约流程时发生未预期的异常: ", user.getUserName(), e);
            return null; // 发生任何其他异常都返回null，保证方法的健壮性
        }
    }
}