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
 * @version 2.1 (修改：增加执行前任务存在性校验)
 * @description 调整了任务执行逻辑，先进行数据准备，再进行提交重试。在任务执行前，增加数据库校验，若任务已被删除则取消执行。
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
    private static final String STATUS_CANCELLED = "任务已取消";


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
    private static final DateTimeFormatter APPOINTMENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void init() {
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
//            try {
//                log.info("对用户ID: {} 的任务进行预检...", user.getId());
//                if (!appointmentProcessorService.preProcessCheck(user)) {
//                    log.warn("用户ID: {} 的任务未能通过预检，将不会被调度。", user.getId());
//                    continue;
//                }
//                log.info("用户ID: {} 的任务通过预检。", user.getId());
//            } catch (Exception e) {
//                log.error("对用户ID: {} 的任务进行预检时发生严重异常，任务将不会被调度。", user.getId(), e);
//                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_PRECHECK_FAILED, "预检时发生未知异常: " + e.getMessage());
//                continue;
//            }

            long advanceMillis = yzmTimeS * 1000L;
            long initialDelayMillis = Duration.between(LocalDateTime.now(), appointmentDateTime).toMillis();
            long delayMillis = initialDelayMillis - advanceMillis;

            if (delayMillis < 0) {
                delayMillis = 0;
            }
            try {
                final int effectiveMaxRetries = maxRetries;
                final int effectiveRetriesTime = retriesTime;
                Future<?> future = scheduler.schedule(() -> executeAppointmentWithRetries(user, effectiveMaxRetries, effectiveRetriesTime), delayMillis, TimeUnit.MILLISECONDS);
                scheduledTasks.put(user.getId(), future);
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_SCHEDULED, "任务已进入调度队列，等待执行。");
                log.info("用户ID: {} 的任务已成功调度，将在 {} 毫秒后执行（已提前 {} 秒），最大重试次数: {}。", user.getId(), delayMillis, yzmTimeS, effectiveMaxRetries);
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
        // 在执行任何操作前，先检查该任务是否还存在于数据库中
        if (userSmsWebSocketService.getById(user.getId()) == null) {
            log.warn("任务ID: {} 即将执行时发现其在数据库中已不存在，任务取消。", user.getId());
            scheduledTasks.remove(user.getId()); // 从调度Map中移除，防止内存泄漏
            return; // 终止执行
        }
        log.info("清空之前日志。。。。");
        userSmsWebSocketService.clearUserLogInfoById(user.getId());
        log.info("清空之前日志>>结束");

        Map<String, Object> preparedData;
        try {
            // ================== 1. 数据准备阶段 (只执行一次) ==================
            log.info("【阶段1/3】开始为用户ID: {} 准备预约数据...", user.getId());
            userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_PROCESSING, "正在准备预约数据...");
            preparedData = appointmentProcessorService.prepareAppointmentData(user);

            PostPointmentDTO dto = (PostPointmentDTO) preparedData.get("dto");
            // 核心校验：根据需求，如果准备阶段未能获取到uuid，则视为失败
            if (dto == null || StrUtil.isBlank(dto.getUuid())) {
                throw new BusinessException(0,"数据准备失败：未能获取到有效的短信验证码。");
            }
            log.info("【阶段1/3】用户ID: {} 的预约数据准备成功。", user.getId());
        } catch (Exception e) {
            log.error("用户ID: {} 在[数据准备阶段]失败，任务终止。", user.getId(), e);
            userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, "数据准备失败: " + e.getMessage());
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
                    log.info("【阶段2/3】用户ID: {} 等待精准预约时间... 需等待 {} 秒。", user.getId(), millisToWait / 1000.0);
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
        log.info("【阶段3/3】用户ID: {} 已到达指定时间，开始提交...", user.getId());
        PostPointmentDTO dtoToSubmit = (PostPointmentDTO) preparedData.get("dto");
        RequestHeaderUtil headersToSubmit = (RequestHeaderUtil) preparedData.get("headers");

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 在每次重试前，再次检查任务是否已被删除
                if (userSmsWebSocketService.getById(user.getId()) == null) {
                    log.warn("任务ID: {} 在重试过程中发现其在数据库中已不存在，停止重试。", user.getId());
                    scheduledTasks.remove(user.getId());
                    return; // 终止执行
                }

                log.info("开始提交用户ID: {} 的预约任务，第 {}/{} 次尝试。", user.getId(), attempt, maxRetries);
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_PROCESSING, String.format("第 %d/%d 次尝试提交...", attempt, maxRetries));

                // 调用新的、仅负责提交的方法
                JSONObject o = appointmentProcessorService.submitAppointment(headersToSubmit, dtoToSubmit);
                log.info("用户ID: {} 第 {} 次尝试的提交结果：{}", user.getId(), attempt, o);

                JSONObject data = o.getJSONObject("data");
                if (data != null && data.getInteger("retCode") == 1) {
                    log.info("用户ID: {} 的预约任务提交成功。", user.getId());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_SUCCESS, "预约流程执行成功。");
                    scheduledTasks.remove(user.getId()); // 成功后也要移除
                    return; // 成功，方法结束
                }

                String errorMsg = (data != null) ? data.getString("msg") : "返回结果中无data字段或业务失败";
                throw new BusinessException(0,errorMsg);

            } catch (Exception e) {
                log.error("用户ID: {} 的预约任务第 {}/{} 次提交失败。错误: {}", user.getId(), attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    String retryMessage = String.format("提交失败，准备第 %d/%d 次重试: %s", attempt + 1, maxRetries, e.getMessage());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, retryMessage);
                    try {
                        Thread.sleep(effectiveRetriesTime * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待时线程被中断，任务ID: {}，将停止重试。", user.getId());
                        scheduledTasks.remove(user.getId()); // 中断后移除
                        return;
                    }
                } else {
                    log.error("用户ID: {} 的任务在尝试 {} 次提交后最终失败。", user.getId(), maxRetries);
                    String finalFailMessage = String.format("任务失败，已达最大提交次数(%d次): %s", maxRetries, e.getMessage());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, finalFailMessage);
                }
            }
        }
        // 循环结束（所有重试都失败），从跟踪Map中移除
        scheduledTasks.remove(user.getId());
    }

    /**
     * 从配置Map中安全地解析整型值。
     *
     * @param configValue  配置Map
     * @param key          要查找的键
     * @param defaultValue 解析失败或键不存在时返回的默认值
     * @return 解析后的整型值或默认值
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