package com.wzz.hslspringboot.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceExecutor {

    private static final Logger log = LogManager.getLogger(ServiceExecutor.class);

    /**
     * 统一的静态调度方法
     * @param serviceName 服务名称字符串 (对应枚举的常量名)
     * @param methodName 方法名称字符串
     * @param args 方法参数
     * @return 方法执行的返回值
     */
    public static Object dispatch(String serviceName, String methodName, Object... args) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            System.err.println("错误：服务名称不能为空。");
            return null;
        }
        try {
            String serviceEnumName = serviceName.toUpperCase();
            ServiceDispatcher dispatcher = ServiceDispatcher.valueOf(serviceEnumName);

            return dispatcher.execute(methodName, args);
        } catch (IllegalArgumentException e) {
            log.error("错误：无效的服务名称 '{}'。", serviceName);
            throw new RuntimeException("错误：无效的服务名称 '{}'。",e);
        } catch (RuntimeException e) {
            log.error("执行失败，根本原因: {}", e.getMessage());
            throw new RuntimeException("执行失败，根本原因: {}。",e);
        }
    }
}