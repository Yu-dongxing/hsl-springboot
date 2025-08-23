package com.wzz.hslspringboot.task;

import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.AppointmentProcessorService;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import com.wzz.hslspringboot.utils.DataConverterUtil;
import com.wzz.hslspringboot.utils.DateTimeUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
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

    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    @Autowired
    private AppointmentProcessorService appointmentProcessorService;

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
        log.info("<每秒执行一次>");
        // 1. 从数据库查询所有状态为“待处理”的任务
        List<UserSmsWebSocket> pendingTasks = userSmsWebSocketService.getAll(STATUS_PENDING);

        for (UserSmsWebSocket user : pendingTasks) {
            // 2. 检查任务是否已经被调度，如果已在处理中，则跳过
            if (scheduledTasks.containsKey(user.getId())) {
                continue;
            }

            // 3. 解析预约时间
            LocalDateTime appointmentDateTime;
            try {
                appointmentDateTime = LocalDateTime.parse(user.getAppointmentTime(), APPOINTMENT_TIME_FORMATTER);
                 LocalDateTime w = DateTimeUtil.parseDateTime(user.getAppointmentTime());
            } catch (DateTimeParseException e) {
                log.error("用户ID: {} 的预约时间'{}'格式无效，请使用'yyyy-MM-dd HH:mm:ss'格式。", user.getId(), user.getAppointmentTime());
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_INVALID_TIME, "预约时间格式错误: " + e.getMessage());
                continue; // 处理下一个
            }

            // 4. 计算距离现在需要延迟多久执行
            long delayMillis = java.time.Duration.between(LocalDateTime.now(), appointmentDateTime).toMillis();
            if (delayMillis < 0) {
                delayMillis = 0; // 如果时间已过，立即执行
            }

            // 5. 提交任务到调度器
            try {
                Future<?> future = scheduler.schedule(() -> executeAppointmentWithRetries(user), delayMillis, TimeUnit.MILLISECONDS);

                // 6. 将任务放入跟踪Map，并更新数据库状态为“已调度”
                scheduledTasks.put(user.getId(), future);
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_SCHEDULED, "任务已进入调度队列，等待执行。");
                log.info("用户ID: {} 的任务已成功调度，将在 {} 毫秒后执行。", user.getId(), delayMillis);

            } catch (Exception e) {
                log.error("调度用户ID: {} 的任务时发生异常。", user.getId(), e);
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, "任务调度失败: " + e.getMessage());
            }
        }
    }

    /**
     * 封装了核心业务逻辑的执行和重试机制。
     * 此方法将在指定的预约时间被虚拟线程调用。
     *
     * @param user 用户及预约信息
     */
    private void executeAppointmentWithRetries(UserSmsWebSocket user) {
        // 使用一个无限循环来实现“失败后立即重试”
        while (true) {
            try {
                // 1. 更新状态为“执行中”
                log.info("开始执行用户ID: {} 的预约任务。", user.getId());
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_PROCESSING, "开始调用核心预约逻辑。");

                // 2. 调用核心业务逻辑
                appointmentProcessorService.processAppointment(user);

                // 3. 如果没有抛出异常，视为成功
                log.info("用户ID: {} 的预约任务执行成功。", user.getId());
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_SUCCESS, "预约流程执行成功。");

                // 4. 成功后，跳出重试循环
                break;

            } catch (Exception e) {
                // 5. 捕获任何异常，更新状态并准备重试
                log.error("用户ID: {} 的预约任务执行失败，将立即重试。错误: {}", user.getId(), e.getMessage(), e);
                userSmsWebSocketService.updateTaskStatus(user.getId(), STATUS_FAILED, "执行失败，即将重试: " + e.getMessage());

                // 为防止因外部服务瞬间不可用导致CPU空转，可以在重试前短暂休眠
                try {
                    Thread.sleep(2000); // 休眠2秒再重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // 恢复中断状态
                    log.error("重试等待时线程被中断，任务ID: {}", user.getId());
                    break; // 如果等待被中断，则停止重试
                }
            }
        }
        // 任务完成（无论是成功还是因中断退出），从跟踪Map中移除
        scheduledTasks.remove(user.getId());
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