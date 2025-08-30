package com.wzz.hslspringboot.task;

import com.alibaba.fastjson.JSONObject;
import com.wzz.hslspringboot.exception.BusinessException;
import com.wzz.hslspringboot.pojo.NewSysConfig;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.AppointmentProcessorService;
import com.wzz.hslspringboot.service.SysConfigService;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import com.wzz.hslspringboot.utils.DateTimeUtil;
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
    private static final String STATUS_PRECHECK_FAILED = "预检失败"; // 新增状态

    // --- 新增：重试次数相关常量 ---
    private static final String MAX_RETRY_CONFIG_KEY = "maximum_number_of_retries";
    private static final int DEFAULT_MAX_RETRIES = 3; // 默认最大重试次数
    private static final String RETRY_INTERVAL_KEY = "Retry_interval";
    private static final long RETRY_INTERVAL_MS = 2000; // 重试间隔时间（毫秒）
    private static final int RETRY_INTERVAL_S = 2; // 重试间隔时间（秒）


    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    @Autowired
    private AppointmentProcessorService appointmentProcessorService;

    @Autowired
    private SysConfigService sysConfigService;

    // 使用虚拟线程的 ScheduledExecutorService 来执行定时任务
    private ScheduledExecutorService scheduler;

    // 用于跟踪已调度但尚未完成的任务，防止重复调度
    // Key: UserSmsWebSocket的ID, Value: 任务的Future对象，可用于取消
    private final ConcurrentMap<Long, Future<?>> scheduledTasks = new ConcurrentHashMap<>();

    // 定义预约时间的格式
    private static final DateTimeFormatter APPOINTMENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void init() {
        // 使用虚拟线程工厂初始化调度器
        scheduler = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory());
        log.info("虚拟线程预约任务调度器已初始化。");
    }

    /**
     * 每秒执行一次，扫描数据库中“待处理”的任务并进行调度
     */
    @Scheduled(fixedRate = 1000)
    public void scheduleNewAppointments() {
//        log.info("扫描数据库中“待处理”的任务并进行调度");
        // 1. 从数据库查询所有状态为“待处理”的任务
        List<UserSmsWebSocket> pendingTasks = userSmsWebSocketService.getAll(STATUS_PENDING);
//        log.info("<<UNK>>::{}",pendingTasks);

        if (pendingTasks.isEmpty()) {
            return;
        }

        // 统一获取系统配置，避免在循环中重复查询数据库
        NewSysConfig co = sysConfigService.getConfigByName("sys_config");
        Map<String, Object> configValue = (co != null) ? co.getConfigValue() : null;

        // 解析需要提前的秒数 (yzm_time_s)
        int yzmTimeS = parseConfigInt(configValue, "yzm_time_s", 0);
//        log.info("系统配置：任务将提前 {} 秒执行。", yzmTimeS);

        // --- 新增：解析最大重试次数 ---
        int maxRetries = parseConfigInt(configValue, MAX_RETRY_CONFIG_KEY, DEFAULT_MAX_RETRIES);
//        log.info("系统配置：任务最大重试次数为 {}。", maxRetries);

        // --- 新增：解析重试间隔时间 ---
        int retriesTime = parseConfigInt(configValue, RETRY_INTERVAL_KEY, RETRY_INTERVAL_S);
//        log.info("系统配置：任务重试间隔时间为 {}秒。", retriesTime);


        for (UserSmsWebSocket user : pendingTasks) {
            if (user.getAppointmentTime()==null || user.getAppointmentTime().isEmpty()){

                continue;
            }
            // 2. 检查任务是否已经被调度，如果已在处理中，则跳过
            if (scheduledTasks.containsKey(user.getId())) {
                continue;
            }


            // 3. 解析预约时间
            LocalDateTime appointmentDateTime;
            try {
                appointmentDateTime =  DateTimeUtil.parseDateTime(user.getAppointmentTime());
//                appointmentDateTime = LocalDateTime.parse(user.getAppointmentTime(), APPOINTMENT_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                log.error("用户ID: {} 的预约时间'{}'格式无效，请使用'yyyy-MM-dd HH:mm:ss'格式。", user.getId(), user.getAppointmentTime());
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_INVALID_TIME, "预约时间格式错误: " + e.getMessage());
                continue; // 处理下一个
            }

            // ================= 新增预检逻辑开始 =================
            try {
                log.info("对用户ID: {} 的任务进行预检...", user.getId());
                boolean preCheckOk = appointmentProcessorService.preProcessCheck(user);
                if (!preCheckOk) {
                    log.warn("用户ID: {} 的任务未能通过预检，将不会被调度。", user.getId());
                    // 预检失败的状态已经在 preProcessCheck 方法内部更新了，这里直接跳过即可
                    continue; // 处理下一个任务
                }
                log.info("用户ID: {} 的任务通过预检。", user.getId());
            } catch (Exception e) {
                log.error("对用户ID: {} 的任务进行预检时发生严重异常，任务将不会被调度。", user.getId(), e);
                // 异常情况下，也需要更新任务状态，并跳过
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_PRECHECK_FAILED, "预检时发生未知异常: " + e.getMessage());
                continue;
            }

            // 4. 计算任务执行延迟时间
            long advanceMillis = yzmTimeS * 1000L;
            long initialDelayMillis = Duration.between(LocalDateTime.now(), appointmentDateTime).toMillis();
            long delayMillis = initialDelayMillis - advanceMillis;

            if (delayMillis < 0) {
                delayMillis = 0; // 如果计算后的时间已过或非常接近，立即执行
            }

            // 5. 提交任务到调度器
            try {
                // 使用 final 变量以便在 lambda 中使用
                final int effectiveMaxRetries = maxRetries;
                final int effectiveRetriesTime = retriesTime;
                Future<?> future = scheduler.schedule(() -> executeAppointmentWithRetries(user, effectiveMaxRetries,effectiveRetriesTime), delayMillis, TimeUnit.MILLISECONDS);

                // 6. 将任务放入跟踪Map，并更新数据库状态为“已调度”
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
     * 封装了核心业务逻辑的执行和有限重试机制。
     *
     * @param user                 用户及预约信息
     * @param maxRetries           最大重试次数
     * @param effectiveRetriesTime
     */
    private void executeAppointmentWithRetries(UserSmsWebSocket user, int maxRetries, int effectiveRetriesTime) {

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 1. 更新状态为“执行中”
                log.info("开始执行用户ID: {} 的预约任务，第 {}/{} 次尝试。", user.getId(), attempt, maxRetries);
                String processingMessage = (attempt == 1)
                        ? "开始调用核心预约逻辑。"
                        : String.format("开始第 %d/%d 次重试...", attempt, maxRetries);
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_PROCESSING, processingMessage);

                // 2. 调用核心业务逻辑
                JSONObject o = appointmentProcessorService.processAppointment(user);
                log.info("用户ID: {} 第 {} 次尝试的请求结果：{}", user.getId(), attempt, o);

                // 3. 检查业务返回码，判断是否成功
                JSONObject data = o.getJSONObject("data");
                if (data != null && data.getInteger("retCode") == 1) {
                    // 业务成功
                    log.info("用户ID: {} 的预约任务执行成功。", user.getId());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_SUCCESS, "预约流程执行成功。");
                    // 成功后，任务完成，直接返回
                    return;
                }

                // 业务失败，构造错误信息并抛出异常，以便进入重试逻辑
                String errorMsg = (data != null) ? data.getString("msg") : "返回结果中无data字段或业务失败";
                throw new BusinessException(0, errorMsg);

            } catch (Exception e) {
                log.error("用户ID: {} 的预约任务第 {}/{} 次尝试失败。错误: {}", user.getId(), attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    // 如果还未达到最大重试次数，更新状态并准备下一次重试
                    String retryMessage = String.format("执行失败，准备第 %d/%d 次重试: %s", attempt + 1, maxRetries, e.getMessage());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, retryMessage);
                    try {

                        // 在重试前短暂休眠，避免因服务瞬间不可用导致CPU空转
//                        Thread.sleep(RETRY_INTERVAL_MS);
                        Thread.sleep(effectiveRetriesTime * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // 恢复中断状态
                        log.error("重试等待时线程被中断，任务ID: {}，将停止重试。", user.getId());
                        // 如果等待被中断，则提前终止重试
                        return;
                    }
                } else {
                    // 达到最大重试次数，记录最终失败状态
                    log.error("用户ID: {} 的任务在尝试 {} 次后最终失败。", user.getId(), maxRetries);
                    String finalFailMessage = String.format("任务失败，已达最大重试次数(%d次): %s", maxRetries, e.getMessage());
                    userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, finalFailMessage);
                }
            }
        }
        // 当循环结束（无论是成功返回还是所有重试都失败），都将任务从跟踪Map中移除
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