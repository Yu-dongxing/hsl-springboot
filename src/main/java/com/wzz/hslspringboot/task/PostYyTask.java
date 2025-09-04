package com.wzz.hslspringboot.task;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import com.wzz.hslspringboot.exception.BusinessException;
import com.wzz.hslspringboot.pojo.NewSysConfig;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.AppointmentProcessorService;
import com.wzz.hslspringboot.service.SysConfigService;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import com.wzz.hslspringboot.utils.DateTimeUtil;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 预约任务统一调度器
 *
 * @version 2.2 (修改：调整预检逻辑，增加日志详情)
 * @description 调整了任务执行逻辑，粮仓相关检查在预检时只做警告，在任务执行时做强校验。全面增强了日志记录的详细程度。
 */
@Component
public class PostYyTask {

    private static final Logger log = LogManager.getLogger(PostYyTask.class);

    // --- 任务状态常量 ---
    private static final String STATUS_PENDING = "待处理";
    private static final String STATUS_SCHEDULED = "已调度";
    private static final String STATUS_PROCESSING = "执行中";
    private static final String STATUS_SUCCESS = "成功";
    private static final String STATUS_FAILED = "异常";
    private static final String STATUS_INVALID_TIME = "时间格式无效";
    private static final String STATUS_PRECHECK_FAILED = "预检失败";
    private static final String STATUS_PRECHECK_WARN = "预检警告"; // 新增状态


    // --- 重试次数相关常量 ---
    private static final String MAX_RETRY_CONFIG_KEY = "maximum_number_of_retries";
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final String RETRY_INTERVAL_KEY = "Retry_interval";
    private static final int RETRY_INTERVAL_S = 2;


    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    @Autowired
    private AppointmentProcessorService appointmentProcessorService;

    @Autowired
    private SysConfigService sysConfigService;

    private ScheduledExecutorService scheduler;
    private final ConcurrentMap<Long, Future<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 使用虚拟线程池，适合处理大量I/O密集型任务
        scheduler = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory());
        log.info("虚拟线程预约任务调度器已初始化。");
    }

    /**
     * 每秒执行一次，扫描数据库中“待处理”的任务并进行调度
     */
    @Scheduled(fixedRate = 1000)
    public void scheduleNewAppointments() {
        List<UserSmsWebSocket> pendingTasks = userSmsWebSocketService.getAll(STATUS_PENDING);

        if (pendingTasks.isEmpty()) {
            return;
        }

        NewSysConfig co = sysConfigService.getConfigByName("sys_config");
        Map<String, Object> configValue = (co != null) ? co.getConfigValue() : null;

        int yzmTimeS = parseConfigInt(configValue, "yzm_time_s", 0);
        int maxRetries = parseConfigInt(configValue, MAX_RETRY_CONFIG_KEY, DEFAULT_MAX_RETRIES);
        int retriesTime = parseConfigInt(configValue, RETRY_INTERVAL_KEY, RETRY_INTERVAL_S);


        for (UserSmsWebSocket user : pendingTasks) {
            if (user.getAppointmentTime() == null || user.getAppointmentTime().isEmpty()) {
                continue;
            }
            if (scheduledTasks.containsKey(user.getId())) {
                log.trace("用户ID: {} 的任务已在调度队列中，本次跳过。", user.getId());
                continue;
            }

            LocalDateTime appointmentDateTime;
            try {
                appointmentDateTime = DateTimeUtil.parseDateTime(user.getAppointmentTime());
            } catch (DateTimeParseException e) {
                log.error("用户ID: {} 的预约时间'{}'格式无效。", user.getId(), user.getAppointmentTime());
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_INVALID_TIME, "预约时间格式错误: " + e.getMessage());
                continue;
            }
            try {
                log.info("对用户ID: {} 的任务进行预检...", user.getId());
                if (!appointmentProcessorService.preProcessCheck(user)) {
                    log.warn("用户ID: {} 的任务未能通过预检，将不会被调度。", user.getId());
                    continue; // 预检失败，跳过此任务
                }
                log.info("用户ID: {} 的任务通过预检（可能包含警告），准备调度。", user.getId());
            } catch (Exception e) {
                log.error("对用户ID: {} 的任务进行预检时发生严重异常，任务将不会被调度。", user.getId(), e);
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_PRECHECK_FAILED, "预检时发生未知异常: " + e.getMessage());
                continue;
            }

            long advanceMillis = yzmTimeS * 1000L;
            long initialDelayMillis = Duration.between(LocalDateTime.now(), appointmentDateTime).toMillis();
            long delayMillis = initialDelayMillis - advanceMillis;

            if (delayMillis < 0) {
                log.warn("用户ID: {} 的任务执行时间已过或过于接近，将立即执行。预约时间: {}", user.getId(), user.getAppointmentTime());
                delayMillis = 0;
            }
            try {
                final int effectiveMaxRetries = maxRetries;
                final int effectiveRetriesTime = retriesTime;
                Future<?> future = scheduler.schedule(() -> executeAppointmentWithRetries(user, effectiveMaxRetries, effectiveRetriesTime), delayMillis, TimeUnit.MILLISECONDS);
                scheduledTasks.put(user.getId(), future);
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_SCHEDULED, "任务已进入调度队列，等待执行。");
                log.info("用户ID: {} 的任务已成功调度，将在约 {} 秒后执行（已提前 {} 秒），最大重试次数: {}。", user.getId(),delayMillis/1000L, yzmTimeS, effectiveMaxRetries);
            } catch (Exception e) {
                log.error("调度用户ID: {} 的任务时发生异常。", user.getId(), e);
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, "任务调度失败: " + e.getMessage());
            }
        }
    }


    /**
     * 封装了核心业务逻辑的执行。
     * 新逻辑分为：0. 任务存在性校验 -> 1.数据准备 -> 2.精确时间等待 -> 3.提交与重试
     *
     * @param user                 用户及预约信息
     * @param maxRetries           最大重试次数
     * @param effectiveRetriesTime 重试间隔
     */
    private void executeAppointmentWithRetries(UserSmsWebSocket user, int maxRetries, int effectiveRetriesTime) {
        // ================== 0. 任务存在性校验 (新增逻辑) ==================
        if (userSmsWebSocketService.getById(user.getId()) == null) {
            log.warn("任务ID: {} 即将执行时发现其在数据库中已不存在，任务取消。", user.getId());
            scheduledTasks.remove(user.getId());
            return;
        }
        log.info("开始清空用户【{}】(ID:{}) 的历史执行日志...", user.getUserName(), user.getId());
        userSmsWebSocketService.clearUserLogInfoById(user.getId());
        log.info("用户【{}】(ID:{}) 的历史日志已清空。", user.getUserName(), user.getId());


        Map<String, Object> preparedData;
        try {
            // ================== 1. 数据准备阶段 (只执行一次) ==================
            log.info("【阶段1/3】开始为用户ID: {} 准备预约数据...", user.getId());
            userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_PROCESSING, "正在准备预约数据...");
            preparedData = appointmentProcessorService.prepareAppointmentData(user);

            PostPointmentDTO dto = (PostPointmentDTO) preparedData.get("dto");
            if (dto == null || StrUtil.isBlank(dto.getUuid())) {
                log.warn("用户ID: {} 数据准备阶段成功，但未能获取到随机码(uuid)，这可能导致需要短信验证时提交失败。", user.getId());
            }
            log.info("【阶段1/3】用户ID: {} 的预约数据准备成功。", user.getId());
        } catch (Exception e) {
            log.error("用户ID: {} 在[数据准备阶段]失败，任务终止。异常类型: {}", user.getId(), e.getClass().getSimpleName(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "未知错误";
            userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, "数据准备失败: " + errorMessage);
            scheduledTasks.remove(user.getId());
            return;
        }
        try {
            // ================== 2. 精确时间等待阶段 ==================
            LocalDateTime appointmentDateTime = DateTimeUtil.parseDateTime(user.getAppointmentTime());
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(appointmentDateTime)) {
                long millisToWait = Duration.between(now, appointmentDateTime).toMillis();
                if (millisToWait > 0) {
                    log.info("【阶段2/3】用户ID: {} 等待精准预约时间... 需等待 {} 秒。", user.getId(), millisToWait / 1000L);
                    userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_SCHEDULED, "准备就绪，等待精准提交时间...");
                    Thread.sleep(millisToWait);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【阶段2/3】用户ID: {} 在等待精准预约时间时被中断。", user.getId(), e);
            userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, "等待提交时间被中断: " + e.getMessage());
            scheduledTasks.remove(user.getId());
            return;
        } catch (Exception e) {
            log.error("【阶段2/3】用户ID: {} 在等待精准预约时间时发生未知错误。", user.getId(), e);
            userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, "等待提交时间异常: " + e.getMessage());
            scheduledTasks.remove(user.getId());
            return;
        }

        // ================== 3. 提交与重试阶段 ==================
        log.info("【阶段3/3】用户ID: {} 已到达指定时间，开始执行提交...", user.getId());
        PostPointmentDTO dtoToSubmit = (PostPointmentDTO) preparedData.get("dto");
        RequestHeaderUtil headersToSubmit = (RequestHeaderUtil) preparedData.get("headers");

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (userSmsWebSocketService.getById(user.getId()) == null) {
                    log.warn("任务ID: {} 在重试过程中发现其在数据库中已不存在，停止重试。", user.getId());
                    scheduledTasks.remove(user.getId());
                    return;
                }

                log.info("开始提交用户ID: {} 的预约任务，第 {}/{} 次尝试。", user.getId(), attempt, maxRetries);
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_PROCESSING, String.format("第 %d/%d 次尝试提交...", attempt, maxRetries));

                JSONObject o = appointmentProcessorService.submitAppointment(headersToSubmit, dtoToSubmit);

                if (o != null && o.getBooleanValue("success")) {
                    log.info("用户ID: {} 的预约任务提交成功。API响应: {}", user.getId(), o.toJSONString());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_SUCCESS, "预约流程执行成功。");
                    scheduledTasks.remove(user.getId());
                    return;
                }

                String errorMsg = (o != null) ? o.getString("msg") : "返回结果为null或业务失败";
                throw new BusinessException(0,errorMsg);

            } catch (Exception e) {
                log.error("用户ID: {} 的预约任务第 {}/{} 次提交失败。错误详情: {}", user.getId(), attempt, maxRetries, e.getMessage(), e);
                if (attempt < maxRetries) {
                    String retryMessage = String.format("第%d次提交失败，将在%d秒后重试: %s", attempt, effectiveRetriesTime, e.getMessage());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_PROCESSING, retryMessage); // 状态仍为执行中，但附加信息更新
                    try {
                        Thread.sleep(effectiveRetriesTime * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待时线程被中断，任务ID: {}，将停止重试。", user.getId());
                        userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, "重试等待被中断");
                        scheduledTasks.remove(user.getId());
                        return;
                    }
                } else {
                    log.error("用户ID: {} 的任务在尝试 {} 次提交后最终失败。", user.getId(), maxRetries);
                    String finalFailMessage = String.format("任务失败，已达最大提交次数(%d次): %s", maxRetries, e.getMessage());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, finalFailMessage);
                }
            }
        }
        scheduledTasks.remove(user.getId());
    }

    /**
     * 从配置Map中安全地解析整型值。
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


    @PreDestroy
    public void shutdown() {
        log.info("开始关闭预约任务调度器...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS))
                    log.error("调度器未能正常终止。");
            }
        } catch (InterruptedException ie) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("预约任务调度器已关闭。");
    }
}