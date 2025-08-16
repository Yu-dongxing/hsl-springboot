package com.wzz.hslspringboot.modules;

public class ServiceExecutor {

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
            // 1. 将服务名称字符串转换为大写，以匹配枚举常量的标准命名
            String serviceEnumName = serviceName.toUpperCase();

            // 2. 使用 Enum.valueOf() 将字符串动态转换为枚举实例
            // 这是实现你需求的关键！
            ServiceDispatcher dispatcher = ServiceDispatcher.valueOf(serviceEnumName);

            // 3. 委托给枚举实例的 execute 方法执行
            return dispatcher.execute(methodName, args);

        } catch (IllegalArgumentException e) {
            // 当 valueOf() 找不到对应的枚举常量时，会抛出此异常
            System.err.println("错误：无效的服务名称 '" + serviceName + "'。");
            return null;
        } catch (RuntimeException e) {
            // 捕获从 execute 方法抛出的运行时异常
            System.err.println("执行失败，根本原因: " + e.getMessage());
            return null;
        }
    }
}