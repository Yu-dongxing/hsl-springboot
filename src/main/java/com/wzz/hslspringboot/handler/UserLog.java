package com.wzz.hslspringboot.handler;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;


import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserLog {

    private static final ParameterizedMessageFactory MF = ParameterizedMessageFactory.INSTANCE;
    // 可按需限制推送长度，避免消息过长撑爆前端/WS
    private static final int MAX_PUSH_LEN = 4000;
    private static final String STATUS_PENDING = "待处理";
    private static final String STATUS_SCHEDULED = "已调度";
    private static final String STATUS_PROCESSING = "执行中";
    private static final String STATUS_SUCCESS = "成功";
    private static final String STATUS_FAILED = "异常";
    private static final String STATUS_INVALID_TIME = "时间格式无效";
    private static final String STATUS_PRECHECK_FAILED = "预检失败";
    private static final String STATUS_PRECHECK_WARN = "预检警告";

    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    public UserLog(UserSmsWebSocketService userSmsWebSocketService) {
        this.userSmsWebSocketService = userSmsWebSocketService;
    }

    /* ============ info ============ */
    public void info(Logger logger, long userId, String pattern, Object... args) {
        String msg = MF.newMessage("[uid={}] " + pattern, merge(userId, args)).getFormattedMessage();
        logger.info(msg);
        push(userId, STATUS_PROCESSING, msg);
    }

    /* ============ trace ============ */
    public void trace(Logger logger, long userId, Object... args) {
        String msg = MF.newMessage("[uid={}] 调试" , merge(userId, args)).getFormattedMessage();
        logger.trace(msg);
        push(userId, STATUS_PROCESSING, msg);
    }

    /* ============ err / error（无异常）=========== */
    public void err(Logger logger, long userId,  Object... args) {
        String msg = MF.newMessage("[uid={}] 错误" , merge(userId, args)).getFormattedMessage();
        logger.error(msg);
        push(userId, STATUS_FAILED, msg);
    }

    /* ============ err / error（带异常）=========== */
    public void err(Logger logger, long userId, Throwable t, Object... args) {
        String msg = MF.newMessage("[uid={}] 错误", merge(userId, args)).getFormattedMessage();
        logger.error(msg, t);
        String brief = (t == null) ? "" : " | ex=" + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
        push(userId, STATUS_FAILED, msg + brief);
    }

    /* ============ helpers ============ */
    private void push(long userId, String status, String content) {
        String payload = (content == null) ? "" : content;
        if (payload.length() > MAX_PUSH_LEN) {
            payload = payload.substring(0, MAX_PUSH_LEN) + " ...[truncated]";
        }
        try {
            // 你的签名：updateTaskStatus(userid, 状态（info/err/trace）, 日志内容)
            userSmsWebSocketService.updateTaskStatus(userId, status, payload);
        } catch (Exception e) {
            // 失败不影响业务
            // 这里不要再递归用辅助组件打日志，直接用 logger 由调用处负责
        }
    }

    private static Object[] merge(Object first, Object... rest) {
        if (rest == null || rest.length == 0) return new Object[]{ first };
        Object[] arr = new Object[rest.length + 1];
        arr[0] = first;
        System.arraycopy(rest, 0, arr, 1, rest.length);
        return arr;
    }
}
