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
import com.wzz.hslspringboot.handler.UserLog;
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
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 预约处理器服务实现类 (重构版)
 * 封装了完整的单次预约业务逻辑，通过将各步骤拆分为独立的私有方法，提高了代码的可读性和可维护性。
 * 此服务设计为线程安全，可供多线程并发调用。
 */
@Service
public class AppointmentProcessorServiceImpl implements AppointmentProcessorService {

    private static final Logger logs = LogManager.getLogger(AppointmentProcessorServiceImpl.class);

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
    @Autowired
    private UserLog log;
    /**
     * @param user 包含执行预约所需全部信息的用户对象
     * @return 包含准备好的 DTO 和 Headers 的 Map，若准备失败则抛出异常
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public Map<String, Object> prepareAppointmentData(UserSmsWebSocket user) throws IOException, InterruptedException {

        log.info(logs, user.getId(),"开始为用户【{}】准备预约提交数据...", user.getUserName());
        try {
            RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(user);
            PostPointmentDTO dto = new PostPointmentDTO();

            if (!fetchAndSetUserInfo(user, requestHeaderUtil, dto))
                throw new BusinessException(0,"获取并设置用户信息失败");
            if (!fetchAndSetVehicleInfo(user, requestHeaderUtil, dto))
                throw new BusinessException(0,"获取并设置车辆信息失败");

            JSONObject depotData = searchAndSetDepotInfo(user, requestHeaderUtil, dto);
            // 此处 depotData 不会为 null，因为 searchAndSetDepotInfo 在失败时会抛出异常

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
                    log.err(logs, user.getId(),"解析系统配置 'sys_config' 中的 page_sleep_time_s 失败，将使用默认值10。", e);
                }
            }
            long startTime = System.currentTimeMillis();
            if (!handleSmsVerification(user, requestHeaderUtil, dto,true))
                throw new BusinessException(0,"处理短信验证码失败");
            long endTime = System.currentTimeMillis();
            long sleepMillis = page_sleep_time_s_value * 1000L - (endTime - startTime);
            if (sleepMillis > 0) {
                log.info(logs, user.getId(),"<计划总等待{}秒，业务处理耗时{}ms，实际休眠{}ms>：用户【{}】",
                        page_sleep_time_s_value,
                        (endTime - startTime),
                        sleepMillis,
                        user.getUserName());
                Thread.sleep(sleepMillis);
            } else {
                log.info(logs, user.getId(),"<计划总等待{}秒，但业务处理耗时{}ms已超出>：用户【{}】",
                        page_sleep_time_s_value,
                        (endTime - startTime),
                        user.getUserName());
            }

            if (!fetchRandomCode(user, requestHeaderUtil, dto))
                throw new BusinessException(0,"获取随机码(uuid)失败");

            populateRemainingDtoFields(user, dto);

            if (!encryptDtoData(dto))
                throw new BusinessException(0,"加密预约数据失败");

            log.info(logs, user.getId(),"用户【{}】的预约数据准备完成。", user.getUserName());
            Map<String, Object> preparedData = new HashMap<>();
            preparedData.put("dto", dto);
            preparedData.put("headers", requestHeaderUtil);
            return preparedData;

        } catch (InterruptedException e) {
            log.err(logs, user.getId(),"用户【{}】的数据准备任务在等待过程中被中断。", user.getUserName());
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            log.err(logs, user.getId(),"为用户【{}】准备预约数据时发生未预期的异常: ", user.getUserName(), e);
            throw e;
        }
    }

    /**
     * 执行最终的预约提交操作
     *
     * @param headers 请求头工具类
     * @param dto     已填充并加密好的数据传输对象
     * @return 提交后服务端返回的JSONObject
     *             if (!handleSmsVerification(user, requestHeaderUtil, dto))
     *                 throw new BusinessException(0,"处理短信验证码失败");
     *             long endTime = System.currentTimeMillis();
     */
    @Override
    public JSONObject submitAppointment(RequestHeaderUtil headers, PostPointmentDTO dto,UserSmsWebSocket user) throws JsonProcessingException {
        log.info(logs, user.getId(),"步骤 9/{}: 开始为用户【{}】执行最终提交操作...", TOTAL_STEPS, dto.getYyr());
        try {
            log.info(logs, user.getId(),"最终提交的用户【{}】的数据（加密前部分关键信息）: 预约人={}, 库点={}, 日期={}, 时间={}-{}",
                    dto.getYyr(), dto.getYyr(), dto.getZzmc(), dto.getRq(), dto.getKssj(), dto.getJssj());
        } catch (Exception e) {
            log.err(logs, user.getId(),"记录提交前日志时发生异常", e);
        }
        JSONObject submissionResponse = function.postInfo(headers, dto);
        log.info(logs, user.getId(),"用户【{}】预约提交执行完毕，提交结果: {}", dto.getYyr(), submissionResponse != null ? submissionResponse.toJSONString() : "null");
        return submissionResponse;
    }
    @Override
    public JSONObject submitAppointment(RequestHeaderUtil headers, PostPointmentDTO dto, UserSmsWebSocket user, int f) throws IOException, InterruptedException {
        NewSysConfig co = sysConfigService.getConfigByName("sys_config");
        Map<String, Object> configValue = (co != null) ? co.getConfigValue() : null;
        Boolean is_sms = parseConfigValue(configValue,"is_sms",Boolean.class ,true);
            if (!handleSmsVerification(user, headers, dto,is_sms)){
                throw new BusinessException(0,"<处理重试的短信验证码失败>");
            }


        log.info(logs, user.getId(),"步骤 9/{}: 开始为用户【{}】执行最终提交操作...", TOTAL_STEPS, dto.getYyr());
        try {
            log.info(logs, user.getId(),"最终提交的用户【{}】的数据（加密前部分关键信息）: 预约人={}, 库点={}, 日期={}, 时间={}-{}",
                    dto.getYyr(), dto.getYyr(), dto.getZzmc(), dto.getRq(), dto.getKssj(), dto.getJssj());
        } catch (Exception e) {
            log.err(logs, user.getId(),"记录提交前日志时发生异常", e);
        }
        JSONObject submissionResponse = function.postInfo(headers, dto);
        log.info(logs, user.getId(),"用户【{}】预约提交执行完毕，提交结果: {}", dto.getYyr(), submissionResponse != null ? submissionResponse.toJSONString() : "null");
        return submissionResponse;
    }
    // ============================ 私有业务步骤方法 ============================

    /**
     * 步骤 1: 获取并设置用户信息
     */
    private boolean fetchAndSetUserInfo(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto) {
        log.info(logs, user.getId(),"步骤 1/{}: 获取用户信息...", TOTAL_STEPS);
        JSONObject response = function.checkCookieAndGetResponse(user, headers);
        if (response == null || !response.getBooleanValue("success")) {
            log.err(logs, user.getId(),"用户【{}】获取用户信息失败或会话无效，响应: {}", user.getUserName(), response != null ? response.toJSONString() : "null");
            userSmsWebSocketService.updateTaskStatus(user.getId(),"异常","获取用户信息失败："+response.toJSONString());
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
        log.info(logs, user.getId(),"用户【{}】获取用户信息成功: tjr={}, userType={}", user.getUserName(), dto.getTjr(), dto.getUserType());
        return true;
    }

    /**
     * 步骤 2: 获取并设置车辆信息
     */
    private boolean fetchAndSetVehicleInfo(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto) {
        log.info(logs, user.getId(),"步骤 2/{}: 获取车辆信息...", TOTAL_STEPS);
        JSONObject response = function.getLicensePlateId(headers, user);
        JSONArray vehicleArray = response.getJSONArray("data");
        if (vehicleArray != null) {
            for (int i = 0; i < vehicleArray.size(); i++) {
                JSONObject vehicleInfo = vehicleArray.getJSONObject(i);
                if (user.getVehicleLicensePlateNumber().equals(vehicleInfo.getString("cph"))) {
                    dto.setCllxNm(vehicleInfo.getString("cclx"));
                    dto.setCphStr(user.getVehicleLicensePlateNumber() + ",");
                    log.info(logs, user.getId(),"用户【{}】匹配到车辆信息: 车牌号={}, 车辆类型={}", user.getUserName(), user.getVehicleLicensePlateNumber(), dto.getCllxNm());
                    return true;
                }
            }
        }
        // 2. 如果未找到，则执行添加逻辑
        log.err(logs, user.getId(),"用户【{}】的车辆列表未找到车牌号 {}，开始执行添加新车流程...", user.getUserName(), user.getVehicleLicensePlateNumber());
        JSONObject addVehicleResponse = function.postCPH(user, headers);
        log.info(logs, user.getId(),"上传车牌号请求返回数据：{}",addVehicleResponse);
        if (addVehicleResponse != null && "1".equals(addVehicleResponse.getString("retCode"))) {
            dto.setCllxNm("1"); // 使用预定义的常量
            dto.setCphStr(user.getVehicleLicensePlateNumber() + ",");
            log.info(logs, user.getId(),"用户【{}】成功添加新车牌号 {}。API响应: {}", user.getUserName(), user.getVehicleLicensePlateNumber(), addVehicleResponse.toJSONString());
            return true;
        } else {
            log.err(logs, user.getId(),"用户【{}】添加新车牌号 {} 失败，API响应: {}", user.getUserName(), user.getVehicleLicensePlateNumber(), addVehicleResponse != null ? addVehicleResponse.toJSONString() : "null");
            return false;
        }
    }

    /**
     * 步骤 3: 搜索预约库点和时间信息
     * @return 库点数据JSONObject，失败则抛出异常
     */
    private JSONObject searchAndSetDepotInfo(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto) {
        log.info(logs, user.getId(),"步骤 3/{}: 搜索预约库点信息...", TOTAL_STEPS);
        JSONObject response = function.search(user, headers);
        log.info(logs, user.getId(),"用户【{}】搜索库点API原始响应: {}", user.getUserName(), response != null ? response.toJSONString() : "null");

        // 检查1: API响应是否有效
        if (response == null || !response.containsKey("data")) {
            log.err(logs, user.getId(),"用户【{}】搜索库点信息失败，API返回的响应为空或不包含 'data' 键。", user.getUserName());
            throw new BusinessException(0,"搜索库点信息失败，中心库系统未返回有效响应。");
        }

        JSONArray dataArray = response.getJSONArray("data");

        // 检查2: 是否返回了任何库点数据
        if (dataArray == null || dataArray.isEmpty()) {
            log.err(logs, user.getId(),"用户【{}】搜索库点信息未返回有效数据。可能原因：没有符合条件的库点或当前没有可预约的时间段。", user.getUserName());
            throw new BusinessException(0,"未搜索到可用的预约库点，请检查预约设置或稍后再试。");
        }

        JSONObject depotData = dataArray.getJSONObject(0);
        dto.setZzmc(depotData.getString("zzmc"));
        dto.setZznm(depotData.getString("zznm"));
        dto.setLatitude(depotData.getString("latitude"));
        dto.setLongitude(depotData.getString("longitude"));
        dto.setLxfs(depotData.getString("lxfs"));
        dto.setRq(depotData.getString("rq"));
        dto.setPznm(depotData.getString("yypznm"));

        JSONArray timeSlotArray = depotData.getJSONArray("yypzmxList");

        // 检查3: 库点是否返回了可用的时间段
        if (timeSlotArray == null || timeSlotArray.isEmpty()) {
            log.err(logs, user.getId(),"用户【{}】的预约库点 {} 没有可用的时间段", user.getUserName(), depotData.getString("zzmc"));
            throw new BusinessException(0,"库点【" + depotData.getString("zzmc") + "】当前没有可预约的时间段。");
        }

        JSONObject firstTimeSlot = timeSlotArray.getJSONObject(0);
        dto.setKssj(firstTimeSlot.getString("kssj"));
        dto.setJssj(firstTimeSlot.getString("jssj"));
        dto.setPzmxnm(firstTimeSlot.getString("yypzmxnm"));

        log.info(logs, user.getId(),"用户【{}】获取到预约库点: {}, 日期: {}, 时间: {}-{}", user.getUserName(), dto.getZzmc(), dto.getRq(), dto.getKssj(), dto.getJssj());
        return depotData;
    }

    /**
     * 步骤 4: 解析并设置粮食品种
     */
    private boolean setGrainInfo(UserSmsWebSocket user, PostPointmentDTO dto, JSONObject depotData) {
        log.info(logs, user.getId(),"步骤 4/{}: 设置粮食品种...", TOTAL_STEPS);
        JSONArray grainArray = JSON.parseArray(depotData.getString("lspz"));
        if (grainArray != null && !grainArray.isEmpty()) {
            for (int i = 0; i < grainArray.size(); i++) {
                JSONObject grain = grainArray.getJSONObject(i);
                if (user.getGrainVarieties().equals(grain.getString("name"))) {
                    dto.setLsmc(grain.getString("name"));
                    dto.setLsnm(grain.getString("nm"));
                    log.info(logs, user.getId(),"用户【{}】匹配到粮食品种: {}", user.getUserName(), dto.getLsmc());
                    return true;
                }
            }
        }
         log.err(logs, user.getId(),"用户【{}】未在库点粮食品种列表中找到匹配项: {}。可用品种: {}", user.getUserName(), user.getGrainVarieties(), grainArray != null ? grainArray.toJSONString() : "[]");
        return false;
    }

    /**
     * 步骤 5: 执行一系列业务前置检查
     */
    private void performPreChecks(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto, JSONObject depotData) {
        log.info(logs, user.getId(),"步骤 5/{}: 执行业务前置检查...", TOTAL_STEPS);
        log.info(logs, user.getId()," -> 5.1/5: 执行 getGrxxStatus 检查...");
        function.getGrxxStatus(dto, headers, user);
        log.info(logs, user.getId()," -> 5.2/5: 执行 hdmCheck 检查...");
        function.hdmCheck(headers, user);
        log.info(logs, user.getId()," -> 5.3/5: 执行 getDistanceByCurrentLocation 检查...");
        function.getDistanceByCurrentLocation(dto.getZznm(), dto.getLongitude(), dto.getLatitude(), dto, headers, user);
        log.info(logs, user.getId()," -> 5.4/5: 执行 getSmsBooles 检查...");
        function.getSmsBooles(dto, headers);
        log.info(logs, user.getId()," -> 5.5/5: 执行 getResvSjList 检查...");
        function.getResvSjList(headers, depotData);
        log.info(logs, user.getId(),"用户【{}】业务前置检查调用完成。", user.getUserName());
    }

    /**
     * 步骤 6: 获取随机码
     */
    private boolean fetchRandomCode(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto) {
        log.info(logs, user.getId(),"步骤 6/{}: 获取随机码...", TOTAL_STEPS);
        function.getRandomcode(dto, headers, user);
        log.info(logs, user.getId(),"用户【{}】获取随机码调用完成。", user.getUserName());
        return true;
    }

    /**
     * 步骤 7: 处理短信验证码
     */
    private boolean handleSmsVerification(UserSmsWebSocket user, RequestHeaderUtil headers, PostPointmentDTO dto,boolean is_sms) throws IOException, InterruptedException {
        log.info(logs, user.getId(),"步骤 7/{}: 处理短信验证码...", TOTAL_STEPS);


        JSONObject checkRe = null;
        try {
            //is_sms是此方法新增的
            if (is_sms){
                //重试的抽象方法（原来的）
                checkRe = function.checkData(dto, headers, user);
            }else {
                //重试的抽象方法
                checkRe = function.checkData(dto, headers, user,0);
            }
            log.err(logs, user.getId(),"验证码结果：",checkRe.toString());
        } catch (Exception e) {
            // 捕获在 function.checkData 内部可能发生的任何异常，包括我们刚刚分析的NPE
            userSmsWebSocketService.updateTaskStatus(user.getId(),"执行中","用户【"+ user.getUserName() +"】调用 checkData 接口失败，无法继续短信验证流程。");
             log.err(logs, user.getId(),"用户【{}】调用 checkData 接口失败，无法继续短信验证流程。", user.getUserName(), e);
            // 根据业务决定是抛出异常还是返回false
            throw new BusinessException(0, "检查短信验证需求时发生内部错误");
        }
        //新增的 检查验证码发送状态 原来没有做status判断 导致后面的提交接口返回参数异常
        if (checkRe.getInteger("status")!=200){
            log.err(logs, user.getId(),"用户【{}】checkData 接口返回错误", checkRe.getString("msg"));
            throw new BusinessException(0, checkRe.getString("msg"));
        }
        // 增加对 checkRe 本身以及关键字段的校验
        if (checkRe == null || !checkRe.containsKey("needsms")) {
              log.err(logs, user.getId(),"用户【{}】checkData 接口返回无效，错误", user.getUserName());
            throw new BusinessException(0, "checkData执行时发生内部错误");
            // 这里可以根据业务定义一个默认行为，或者直接报错
        }
        if (checkRe != null && !checkRe.getBooleanValue("needsms")) {
            dto.setDxyzm("");
            log.info(logs, user.getId(),"用户【{}】此次操作无需短信验证。", user.getUserName());
            return true;
        }


        log.info(logs, user.getId(),"用户【{}】此次操作需要短信验证，启动本地验证码轮询...", user.getUserName());
        for (int i = 0; i < 30; i++) {
            UserSmsWebSocket latestUser = userSmsWebSocketService.ByUserPhoneSelect(user.getUserPhone());
            if (latestUser != null && latestUser.getUpSmsTime() != null && !StrUtil.isBlank(latestUser.getUserSmsMessage())&& !latestUser.getUserSmsMessage().isEmpty()) {
                Duration duration = Duration.between(latestUser.getUpSmsTime(), LocalDateTime.now());
                if (duration.getSeconds() <= 30) {
                    dto.setDxyzm(latestUser.getUserSmsMessage());
                    userSmsWebSocketService.updateTaskStatus(user.getId(),"执行中","用户【"+ user.getUserName() +"】成功获取到有效期内的验证码: "+latestUser.getUserSmsMessage()+"");
                    log.info(logs, user.getId(),"用户【{}】成功获取到有效期内的验证码: {}", user.getUserName(), latestUser.getUserSmsMessage());
                    return true; // 获取到验证码后直接返回
                } else {
                      log.err(logs, user.getId(),"用户【{}】找到一条验证码，但已超过30秒有效期。", user.getUserName());
                }
            }else {
                log.err(logs, user.getId(),"用户【{}】暂时未获取到验证码", user.getUserName());
            }
            Thread.sleep(1000);
        }

        // 循环结束仍未找到
        dto.setDxyzm("");
        userSmsWebSocketService.updateTaskStatus(user.getId(),"执行中","用户【"+ user.getUserName() +"】在30秒内未获取到有效验证码。");
          log.err(logs, user.getId(),"用户【{}】在等待30秒后仍未获取到有效的短信验证码，将尝试无验证码提交。", user.getUserName());
        return true; // 即使没有验证码，也认为此步骤处理完成，让后续逻辑决定是否能提交
    }

    /**
     * 步骤 8: 填充其余固定的DTO字段
     */
    private void populateRemainingDtoFields(UserSmsWebSocket user, PostPointmentDTO dto) {
        log.info(logs, user.getId(),"步骤 8/{}: 填充DTO中其余字段...", TOTAL_STEPS);
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
        dto.setWxdlFlag(true);
        dto.setYyfsnm("1");
        dto.setYyfsmc("预约挂号");
        dto.setDevicetype("weixin");
        dto.setZldd("");
        dto.setCyrsjh("");
        log.info(logs, user.getId(),"用户【{}】DTO其余字段填充完毕。", user.getUserName());
    }

    /**
     * 步骤 9: 对预约数据进行加密
     */
    private boolean encryptDtoData(PostPointmentDTO dto) {
        logs.info("步骤 9/{}: 对预约数据进行加密...", TOTAL_STEPS);
        try {
            String secretData = encryptionUtil.rsa(dto);
            dto.setSecretData(secretData);
            logs.info("用户【{}】最终提交的DTO（加密前）: {}", dto.getYyr(), objectMapper.writeValueAsString(dto));
            return true;
        } catch (JsonProcessingException e) {
              logs.error("用户【{}】序列化DTO用于调试日志时失败", dto.getYyr(), e);
            return true; // 即使日志失败，也不中断主流程
        } catch (Exception e) {
             logs.error("用户【{}】加密数据时发生异常: ", dto.getYyr(), e);
            return false;
        }
    }

    @Override
    public boolean preProcessCheck(UserSmsWebSocket user) {
        log.info(logs, user.getId(),"开始为用户【{}】执行预约任务预检...", user.getUserName());
        try {
            RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(user);
            requestHeaderUtil.setPhone(user.getUserPhone());
            PostPointmentDTO dto = new PostPointmentDTO();

            // 预检1 (关键检查): 检查用户信息
            if (!fetchAndSetUserInfo(user, requestHeaderUtil, dto)) {
                 log.err(logs, user.getId(),"预检失败：用户【{}】无法获取有效的用户信息。", user.getUserName());
                userSmsWebSocketService.updateTaskStatus(user.getId(), "预检失败", "获取用户信息失败或用户账号密码错误");
                return false;
            }
            // 预检2 (关键检查): 检查车辆信息
            if (!fetchAndSetVehicleInfo(user, requestHeaderUtil, dto)) {
                 log.err(logs, user.getId(),"预检失败：用户【{}】无法匹配或添加车牌号 {}", user.getUserName(), user.getVehicleLicensePlateNumber());
                userSmsWebSocketService.updateTaskStatus(user.getId(), "预检失败", "未找到或无法添加车牌号: " + user.getVehicleLicensePlateNumber());
                return false;
            }

            // 预检3 & 4 (非关键检查，改为警告): 检查粮库和粮食品种
            try {
                JSONObject depotData = searchAndSetDepotInfo(user, requestHeaderUtil, dto);
                if (!setGrainInfo(user, dto, depotData)) {
                      log.err(logs, user.getId(),"预检警告：用户【{}】无法匹配到粮食品种 {}。此问题将在任务正式执行时再次检查。", user.getUserName(), user.getGrainVarieties());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), "预检警告", "预检时未找到匹配的粮食品种: " + user.getGrainVarieties());
                }
            } catch (Exception e) {
                  log.err(logs, user.getId(),"预检警告：用户【{}】无法获取有效的粮库信息。此问题将在任务正式执行时再次检查。原因: {}", user.getUserName(), e.getMessage());
                userSmsWebSocketService.updateTaskStatus(user.getId(), "预检警告", "预检时搜索粮库信息失败: " + e.getMessage());
            }

            log.info(logs, user.getId(),"用户【{}】的预约任务预检通过（部分检查项可能为警告状态）。", user.getUserName());
            return true;

        } catch (Exception e) {
             log.err(logs, user.getId(),"为用户【{}】执行预检时发生未预期的异常: ", user.getUserName(), e);
            userSmsWebSocketService.updateTaskStatus(user.getId(), "预检失败", "预检过程中发生异常: " + e.getMessage());
            return false;
        }
    }


    @Override
    public boolean preProcessCheckByReport(UserSmsWebSocket user) {
        log.info(logs, user.getId(),"开始为用户【{}】执行重新预检...", user.getUserName());
        try {
            RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(user);
            PostPointmentDTO dto = new PostPointmentDTO();
            requestHeaderUtil.setPhone(user.getUserPhone());
            // 预检1 (关键检查): 检查用户信息
            if (!fetchAndSetUserInfo(user, requestHeaderUtil, dto)) {
                 log.err(logs, user.getId(),"重新预检失败：用户【{}】无法获取有效的用户信息。", user.getUserName());
                userSmsWebSocketService.updateTaskStatus(user.getId(), "重新预检失败", "获取用户信息失败或会话无效");
                return false;
            }

            // 预检3 & 4 (非关键检查，改为警告): 检查粮库和粮食品种
            try {
                JSONObject depotData = searchAndSetDepotInfo(user, requestHeaderUtil, dto);
                if (!setGrainInfo(user, dto, depotData)) {
                      log.err(logs, user.getId(),"重新预检警告：用户【{}】无法匹配到粮食品种 {}。此问题将在任务正式执行时再次检查。", user.getUserName(), user.getGrainVarieties());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), "重新预检警告", "重新预检时未找到匹配的粮食品种: " + user.getGrainVarieties());
                }
            } catch (Exception e) {
                  log.err(logs, user.getId(),"重新预检警告：用户【{}】无法获取有效的粮库信息。此问题将在任务正式执行时再次检查。原因: {}", user.getUserName(), e.getMessage());
                userSmsWebSocketService.updateTaskStatus(user.getId(), "重新预检警告", "重新预检时搜索粮库信息失败: " + e.getMessage());
            }

            log.info(logs, user.getId(),"用户【{}】的预约任务重新预检通过。", user.getUserName());
            user.setTaskStatus("待处理");
            userSmsWebSocketService.save(user);
            return true;

        } catch (Exception e) {
             log.err(logs, user.getId(),"为用户【{}】执行重新预检时发生未预期的异常: ", user.getUserName(), e);
            userSmsWebSocketService.updateTaskStatus(user.getId(), "重新预检失败", "重新预检过程中发生异常: " + e.getMessage());
            return false;
        }

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

            logs.warn("不支持将类型 '{}' 转换为 '{}'，键: '{}'。将尝试强制转换。", value.getClass().getName(), targetType.getName(), key);
            // 3. 最后的尝试：强制转换
            return (T) value;

        } catch (Exception e) {
            logs.error("解析系统配置 '{}' (期望类型: {}) 失败，将使用默认值 '{}'。原始值: '{}', 错误: {}",
                    key, targetType.getSimpleName(), defaultValue, value, e.getMessage());
            return defaultValue;
        }
    }
}