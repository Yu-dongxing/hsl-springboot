package com.wzz.hslspringboot.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import com.wzz.hslspringboot.apis.Function;
import com.wzz.hslspringboot.exception.BusinessException;
import com.wzz.hslspringboot.pojo.NewSysConfig;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.AppointmentProcessorService;
import com.wzz.hslspringboot.service.SysConfigService;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import com.wzz.hslspringboot.utils.EncryptionUtil;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * 预约处理器服务实现类 (重构版)
 * 封装了完整的单次预约业务逻辑，通过将各步骤拆分为独立的私有方法，提高了代码的可读性和可维护性。
 * 此服务设计为线程安全，可供多线程并发调用。
 */
@Service
public class AppointmentProcessorServiceImpl implements AppointmentProcessorService {

    private static final Logger log = LogManager.getLogger(AppointmentProcessorServiceImpl.class);

    @Autowired
    private Function function;
    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    @Autowired
    private SysConfigService sysConfigService;

    private final EncryptionUtil encryptionUtil = new EncryptionUtil();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter APPOINTMENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int TOTAL_STEPS = 10; // 总步骤数，用于日志输出

        /**
         * @param user 包含执行预约所需全部信息的用户对象
         * @return 包含准备好的 DTO 和 Headers 的 Map，若准备失败则抛出异常
         * @throws IOException
         * @throws InterruptedException
         */
        @Override
        public Map<String, Object> prepareAppointmentData(UserSmsWebSocket user) throws IOException, InterruptedException {

            log.info("开始为用户【{}】准备预约提交数据...", user.getUserName());
            try {
                RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(user);
                PostPointmentDTO dto = new PostPointmentDTO();

                if (!fetchAndSetUserInfo(user, requestHeaderUtil, dto))
                    throw new BusinessException(0,"获取并设置用户信息失败");
                if (!fetchAndSetVehicleInfo(user, requestHeaderUtil, dto))
                    throw new BusinessException(0,"获取并设置车辆信息失败");

                JSONObject depotData = searchAndSetDepotInfo(user, requestHeaderUtil, dto);
                if (depotData == null)
                    throw new BusinessException(0,"搜索并设置库点信息失败");

                if (!setGrainInfo(user, dto, depotData))
                    throw new BusinessException(0,"设置粮食品种信息失败");

                performPreChecks(user, requestHeaderUtil, dto, depotData);

                NewSysConfig co = sysConfigService.getConfigByName("sys_config");
                int page_sleep_time_s_value = 10;
                if (co != null && co.getConfigValue() != null) {
                    try {
                        Object page_sleep_time_value = co.getConfigValue().get("page_sleep_time_s");
                        if (page_sleep_time_value instanceof Number) {
                            page_sleep_time_s_value = ((Number) page_sleep_time_value).intValue();
                        }
                    } catch (Exception e) {
                        log.error("解析系统配置 'sys_config' 中的 page_sleep_time_s 失败，将使用默认值10。", e);
                    }
                }
                long startTime = System.currentTimeMillis();
                if (!handleSmsVerification(user, requestHeaderUtil, dto))
                    throw new BusinessException(0,"处理短信验证码失败");
                long endTime = System.currentTimeMillis();
                long sleepMillis = page_sleep_time_s_value * 1000L - (endTime - startTime);
                if (sleepMillis > 0) {
                    log.info("<计划总等待{}秒，业务处理耗时{}ms，实际休眠{}ms>：用户【{}】",
                            page_sleep_time_s_value,
                            (endTime - startTime),
                            sleepMillis,
                            user.getUserName());
                    Thread.sleep(sleepMillis);
                } else {
                    log.warn("<计划总等待{}秒，但业务处理耗时{}ms已超出>：用户【{}】",
                            page_sleep_time_s_value,
                            (endTime - startTime),
                            user.getUserName());
                }

                if (!fetchRandomCode(user, requestHeaderUtil, dto))
                    throw new BusinessException(0,"获取随机码(uuid)失败");

                populateRemainingDtoFields(user, dto);

                if (!encryptDtoData(dto))
                    throw new BusinessException(0,"加密预约数据失败");

                log.info("用户【{}】的预约数据准备完成。", user.getUserName());
                Map<String, Object> preparedData = new HashMap<>();
                preparedData.put("dto", dto);
                preparedData.put("headers", requestHeaderUtil);
                return preparedData;

            } catch (InterruptedException e) {
                log.warn("用户【{}】的数据准备任务在等待过程中被中断。", user.getUserName());
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                log.error("为用户【{}】准备预约数据时发生未预期的异常: ", user.getUserName(), e);
                throw e;
            }
        }

        /**
         * 执行最终的预约提交操作
         *
         * @param headers 请求头工具类
         * @param dto     已填充并加密好的数据传输对象
         * @return 提交后服务端返回的JSONObject
         */
        @Override
        public JSONObject submitAppointment(RequestHeaderUtil headers, PostPointmentDTO dto) throws JsonProcessingException {
            log.info("步骤 9/{}: 开始为用户【{}】执行最终提交操作...", TOTAL_STEPS, dto.getYyr());
            JSONObject submissionResponse = function.postInfo(headers, dto);
            log.info("用户【{}】预约提交执行完毕，提交结果: {}", dto.getYyr(), submissionResponse != null ? submissionResponse.toJSONString() : "null");
            return submissionResponse;
        }

    // ============================ 私有业务步骤方法 ============================

    /**
     * 步骤 1: 获取并设置用户信息
     */
    private boolean fetchAndSetUserInfo(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto) {
        log.info("步骤 1/{}: 获取用户信息...", TOTAL_STEPS);
        JSONObject response = function.checkCookieAndGetResponse(user, headers);
        if (response == null || !response.getBooleanValue("success")) {
//            userSmsWebSocketService.updateTaskStatus();
            log.error("用户【{}】获取用户信息失败或会话无效: {}", user.getUserName(), response);
            return false;
        }
        JSONObject userData = response.getJSONObject("data").getJSONObject("user");
        dto.setTjr(userData.getString("userNm"));
        dto.setUserType(userData.getString("userType"));
        dto.setCs(String.valueOf(user.getNumberOfVehiclesAndShips()));
        dto.setCyr(user.getUserName());
        dto.setJsr(user.getUserName());
        dto.setSfz(user.getUserIdCard());
        dto.setCyr(userData.getString("userNm"));
        dto.setCyrnm("1");
        dto.setCyrsfzh(user.getUserIdCard());
        dto.setCyrsjh(user.getUserPhone());
        if (!StrUtil.hasBlank(headers.getMobileDeviceId())) {
            dto.setMobileDeviceId(headers.getMobileDeviceId());
            dto.setOpenId(headers.getMobileDeviceId());
        }
        log.info("用户【{}】获取用户信息成功。", user.getUserName());
        return true;
    }

    /**
     * 步骤 2: 获取并设置车辆信息
     */
    private boolean fetchAndSetVehicleInfo(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto) {
        log.info("步骤 2/{}: 获取车辆信息...", TOTAL_STEPS);
        JSONObject response = function.getLicensePlateId(headers, user);
        JSONArray vehicleArray = response.getJSONArray("data");
        if (vehicleArray != null) {
            for (int i = 0; i < vehicleArray.size(); i++) {
                JSONObject vehicleInfo = vehicleArray.getJSONObject(i);
                if (user.getVehicleLicensePlateNumber().equals(vehicleInfo.getString("cph"))) {
                    dto.setCllxNm(vehicleInfo.getString("cclx"));
                    dto.setCphStr(user.getVehicleLicensePlateNumber() + ",");
                    log.info("用户【{}】匹配到车辆信息: 车牌号={}, 车辆类型={}", user.getUserName(), user.getVehicleLicensePlateNumber(), dto.getCllxNm());
                    return true;
                }
            }
        }
        // 2. 如果未找到，则执行添加逻辑
        log.info("用户【{}】的车辆列表未找到车牌号 {}，开始执行添加新车流程...", user.getUserName(), user.getVehicleLicensePlateNumber());
        JSONObject addVehicleResponse = function.postCPH(user, headers);

        if (addVehicleResponse != null) {
            dto.setCllxNm("1"); // 使用预定义的常量
            dto.setCphStr(user.getVehicleLicensePlateNumber() + ",");
            log.info("用户【{}】成功添加新车牌号 {}。API响应: {}", user.getUserName(), user.getVehicleLicensePlateNumber(),addVehicleResponse);
            return true; // 添加并设置成功，返回true
        } else {
            log.error("用户【{}】添加新车牌号 {} 失败，API返回null。",  user.getUserName(), user.getVehicleLicensePlateNumber());
            return false;
        }
    }

    /**
     * 步骤 3: 搜索预约库点和时间信息
     * @return 库点数据JSONObject，失败则返回null
     */
    private JSONObject searchAndSetDepotInfo(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto) {
        log.info("步骤 3/{}: 搜索预约库点信息...", TOTAL_STEPS);
        JSONObject response = function.search(user, headers);
        if (response == null || !response.containsKey("data")) {
            log.error("用户【{}】搜索库点信息失败，API返回的响应为空或不包含 'data' 键。", user.getUserName());
            return null;
        }
        JSONArray dataArray = response.getJSONArray("data");
        if (dataArray == null || dataArray.isEmpty()) {
            log.error("用户【{}】搜索库点信息未返回有效数据，可能原因：没有符合条件的库点或当前没有可预约的时间段。", user.getUserName());
        }
        JSONObject depotData = response.getJSONArray("data").getJSONObject(0);
        dto.setZzmc(depotData.getString("zzmc"));
        dto.setZznm(depotData.getString("zznm"));
        dto.setLatitude(depotData.getString("latitude"));
        dto.setLongitude(depotData.getString("longitude"));
        dto.setLxfs(depotData.getString("lxfs"));
        dto.setRq(depotData.getString("rq"));
        dto.setPznm(depotData.getString("yypznm"));
        JSONArray timeSlotArray = depotData.getJSONArray("yypzmxList");
        if (timeSlotArray.isEmpty()) {
            log.error("用户【{}】的预约库点 {} 没有可用的时间段", user.getUserName(), depotData.getString("zzmc"));
            return null;
        }
        JSONObject firstTimeSlot = timeSlotArray.getJSONObject(0);
        dto.setKssj(firstTimeSlot.getString("kssj"));
        dto.setJssj(firstTimeSlot.getString("jssj"));
        dto.setPzmxnm(firstTimeSlot.getString("yypzmxnm"));
        log.info("用户【{}】获取到预约库点: {}, 日期: {}, 时间: {}-{}", user.getUserName(), dto.getZzmc(), dto.getRq(), dto.getKssj(), dto.getJssj());
        return depotData;
    }

    /**
     * 步骤 4: 解析并设置粮食品种
     */
    private boolean setGrainInfo(UserSmsWebSocket user, PostPointmentDTO dto, JSONObject depotData) {
        log.info("步骤 4/{}: 设置粮食品种...", TOTAL_STEPS);
        JSONArray grainArray = JSON.parseArray(depotData.getString("lspz"));
        if (grainArray != null && !grainArray.isEmpty()) {
            for (int i = 0; i < grainArray.size(); i++) {
                JSONObject grain = grainArray.getJSONObject(i);
                if (user.getGrainVarieties().equals(grain.getString("name"))) {
                    dto.setLsmc(grain.getString("name"));
                    dto.setLsnm(grain.getString("nm"));
                    log.info("用户【{}】匹配到粮食品种: {}", user.getUserName(), dto.getLsmc());
                    return true;
                }
            }
        }
        log.error("用户【{}】未在库点粮食品种列表中找到匹配项: {}", user.getUserName(), user.getGrainVarieties());
        return false;
    }

    /**
     * 步骤 5: 执行一系列业务前置检查
     */
    private void performPreChecks(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto, JSONObject depotData) {
        log.info("步骤 5/{}: 执行业务前置检查...", TOTAL_STEPS);
        function.getGrxxStatus(dto, headers, user);
        function.hdmCheck(headers, user);
        function.getDistanceByCurrentLocation(dto.getZznm(), dto.getLongitude(), dto.getLatitude(), dto, headers, user);
        function.getSmsBooles(dto, headers);
        function.getResvSjList(headers, depotData);
        log.info("用户【{}】业务前置检查调用完成。", user.getUserName());
    }

    /**
     * 步骤 6: 获取随机码
     */
    private boolean fetchRandomCode(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto) {
        log.info("步骤 6/{}: 获取随机码...", TOTAL_STEPS);
        function.getRandomcode(dto, headers, user);
        return true;
    }

    /**
     * 步骤 7: 处理短信验证码
     */
    private boolean handleSmsVerification(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto) throws IOException, InterruptedException {
        log.info("步骤 7/{}: 处理短信验证码...", TOTAL_STEPS);
        JSONObject checkRe = function.checkData(dto, headers, user);

        if (!checkRe.getBoolean("needsms")) {
            dto.setDxyzm("");
            log.info("用户【{}】此次操作无需短信验证。", user.getUserName());
            return true;
        }

        log.info("用户【{}】此次操作需要短信验证，正在检查本地验证码...", user.getUserName());
        for (int i = 0; i < 30; i++) {
            UserSmsWebSocket latestUser = userSmsWebSocketService.ByUserPhoneSelect(user.getUserPhone());
            if (latestUser != null && latestUser.getUpSmsTime() != null && !StrUtil.isBlank(latestUser.getUserSmsMessage())) {
                Duration duration = Duration.between(latestUser.getUpSmsTime(), LocalDateTime.now());
                if (duration.getSeconds() <= 30) {
                    dto.setDxyzm(latestUser.getUserSmsMessage());
                    userSmsWebSocketService.updateTaskStatus(user.getId(),"执行中","用户【"+ user.getUserName() +"】成功获取到有效期内的验证码: "+latestUser.getUserSmsMessage()+"");
                    log.info("用户【{}】成功获取到有效期内的验证码: {}", user.getUserName(), latestUser.getUserSmsMessage());
                    break;
                } else {
                    dto.setDxyzm("");
//                    log.warn("用户【{}】的验证码已超过30秒有效期，将不使用该验证码。", user.getUserName());
                }
            } else {
                dto.setDxyzm("");
                userSmsWebSocketService.updateTaskStatus(user.getId(),"执行中","用户【"+ user.getUserName() +"】需要验证码，但数据库中未找到有效验证码或时间戳。");

                log.warn("用户【{}】需要验证码，但数据库中未找到有效验证码或时间戳。", user.getUserName());
            }
            Thread.sleep(1000);
        }
        return true;
    }

    /**
     * 步骤 8: 填充其余固定的DTO字段
     */
    private void populateRemainingDtoFields(UserSmsWebSocket user, PostPointmentDTO dto) {
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
        dto.setZldd("");
        dto.setCyrsjh("");
    }

    /**
     * 步骤 9: 对预约数据进行加密
     */
    private boolean encryptDtoData(PostPointmentDTO dto) {
        log.info("步骤 8/{}: 对预约数据进行加密...", TOTAL_STEPS);
        try {
            String secretData = encryptionUtil.rsa(dto);
            dto.setSecretData(secretData);
            log.debug("用户【{}】最终提交的DTO（加密前）: {}", dto.getYyr(), objectMapper.writeValueAsString(dto));
            return true;
        } catch (JsonProcessingException e) {
            log.warn("用户【{}】序列化DTO用于调试日志时失败", dto.getYyr(), e);
            return true; // 即使日志失败，也不中断主流程
        } catch (Exception e) {
            log.error("用户【{}】加密数据时发生异常: ", dto.getYyr(), e);
            return false;
        }
    }

    /**
     * 步骤 10: 定时提交最终预约请求
     * @return 最终提交结果，失败则返回null
     * @throws InterruptedException 如果线程在休眠时被中断
     */
    private JSONObject scheduleAndSubmit(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto) throws InterruptedException, JsonProcessingException {
        log.info("步骤 9/{}: 准备定时提交最终预约请求...", TOTAL_STEPS);
        LocalDateTime appointmentDateTime;
        try {
            appointmentDateTime = LocalDateTime.parse(user.getAppointmentTime(), APPOINTMENT_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.error("用户【{}】解析预约时间 '{}' 失败，请检查时间格式是否为 'yyyy-MM-dd HH:mm:ss'", user.getUserName(), user.getAppointmentTime(), e);
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(appointmentDateTime)) {
            long millisToWait = Duration.between(now, appointmentDateTime).toMillis();
            if (millisToWait > 0) {
                log.info("用户【{}】的预约时间为 {}，当前时间为 {}，需等待 {} 秒后执行提交。",
                        user.getUserName(),
                        appointmentDateTime.format(APPOINTMENT_TIME_FORMATTER),
                        now.format(APPOINTMENT_TIME_FORMATTER),
                        millisToWait / 1000.0);
                Thread.sleep(millisToWait);
            }
        }

        log.info("步骤 10/{}: 已到达指定预约时间，开始执行最终提交操作...", TOTAL_STEPS);
        JSONObject submissionResponse = function.postInfo(headers, dto);
        log.info("用户【{}】预约流程执行完毕，提交结果: {}", user.getUserName(), submissionResponse != null ? submissionResponse.toJSONString() : "null");

        return submissionResponse;
    }
    @Override
    public boolean preProcessCheck(UserSmsWebSocket user) {
        log.info("开始为用户【{}】执行预约任务预检...", user.getUserName());
        try {
            // 初始化必要的数据结构
            RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(user);
            PostPointmentDTO dto = new PostPointmentDTO();

            // 预检1: 检查用户信息
            if (!fetchAndSetUserInfo(user, requestHeaderUtil, dto)) {
                log.error("预检失败：用户【{}】无法获取有效的用户信息。", user.getUserName());
                userSmsWebSocketService.updateTaskStatus(user.getId(), "预检失败", "获取用户信息失败或会话无效");
                return false;
            }
            // 预检2: 检查车辆信息
            if (!fetchAndSetVehicleInfo(user, requestHeaderUtil, dto)) {
                log.error("预检失败：用户【{}】无法匹配到车牌号,并且无法添加车牌号 {}", user.getUserName(), user.getVehicleLicensePlateNumber());
                userSmsWebSocketService.updateTaskStatus(user.getId(), "预检失败", "未找到匹配的车牌号，并且无法添加车牌号: " + user.getVehicleLicensePlateNumber());
                return false;
            }

            // 预检3: 检查粮库信息
            JSONObject depotData = searchAndSetDepotInfo(user, requestHeaderUtil, dto);
            if (depotData == null ) {
                log.error("预检失败：用户【{}】无法获取有效的粮库信息或可用时间段。", user.getUserName());
                userSmsWebSocketService.updateTaskStatus(user.getId(), "预检失败", "搜索粮库信息失败或粮库无可用时间");
                return false;
            }

            // 预检4: 检查粮食品种
            if (!setGrainInfo(user, dto, depotData)) {
                log.error("预检失败：用户【{}】无法匹配到粮食品种 {}", user.getUserName(), user.getGrainVarieties());
                userSmsWebSocketService.updateTaskStatus(user.getId(), "预检失败", "未找到匹配的粮食品种: " + user.getGrainVarieties());
                return false;
            }

            log.info("用户【{}】的预约任务预检通过。", user.getUserName());
            return true;

        } catch (Exception e) {
            log.error("为用户【{}】执行预检时发生未预期的异常: ", user.getUserName(), e);
            userSmsWebSocketService.updateTaskStatus(user.getId(), "预检失败", "预检过程中发生异常: " + e.getMessage());
            return false;
        }
    }


    @Override
    public boolean preProcessCheckByReport(UserSmsWebSocket user) {
        log.info("开始为用户【{}】执行重新预检...", user.getUserName());
        try {
            // 初始化必要的数据结构
            RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(user);
            PostPointmentDTO dto = new PostPointmentDTO();

            // 预检1: 检查用户信息
            if (!fetchAndSetUserInfo(user, requestHeaderUtil, dto)) {
                log.error("重新预检失败：用户【{}】无法获取有效的用户信息。", user.getUserName());
                userSmsWebSocketService.updateTaskStatus(user.getId(), "重新预检失败", "获取用户信息失败或会话无效");
                return false;
            }

            // 预检3: 检查粮库信息
//            JSONObject depotData = searchAndSetDepotInfo(user, requestHeaderUtil, dto);
//            if (depotData == null) {
//                log.error("重新预检失败：用户【{}】无法获取有效的粮库信息或可用时间段。", user.getUserName());
//                userSmsWebSocketService.updateTaskStatus(user.getId(), "重新预检失败", "搜索粮库信息失败或粮库无可用时间");
//                return false;
//            }
//
//            // 预检4: 检查粮食品种
//            if (!setGrainInfo(user, dto, depotData)) {
//                log.error("重新预检失败：用户【{}】无法匹配到粮食品种 {}", user.getUserName(), user.getGrainVarieties());
//                userSmsWebSocketService.updateTaskStatus(user.getId(), "重新预检失败", "未找到匹配的粮食品种: " + user.getGrainVarieties());
//                return false;
//            }

            log.info("用户【{}】的预约任务重新预检通过。", user.getUserName());
            user.setTaskStatus("待处理");
            userSmsWebSocketService.save(user);
            return true;

        } catch (Exception e) {
            log.error("为用户【{}】执行重新预检时发生未预期的异常: ", user.getUserName(), e);
            userSmsWebSocketService.updateTaskStatus(user.getId(), "重新预检失败", "预检过程中发生异常: " + e.getMessage());
            return false;
        }
    }
}