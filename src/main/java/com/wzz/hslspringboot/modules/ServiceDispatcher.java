package com.wzz.hslspringboot.modules;


import com.wzz.hslspringboot.modules.demo.Msg;

import java.lang.reflect.Method;

// 这个枚举现在是内部实现细节，主要负责映射和执行
public enum ServiceDispatcher {
//     定义枚举常量，并通过构造函数关联它们负责的服务类
    ORDER_SERVICE(new Msg()),
    REFUND_SERVICE(new String());

    private final Object serviceInstance;

    ServiceDispatcher(Object serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    /**
     * 根据方法名字符串动态调用方法 (此方法逻辑不变)
     *
     * @param methodName 要调用的方法的名称
     * @param args       方法的参数
     * @return 方法的返回值
     */
    public Object execute(String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }

            Class<?> serviceClass = this.serviceInstance.getClass();
            Method method = serviceClass.getMethod(methodName, paramTypes);
            return method.invoke(this.serviceInstance, args);

        } catch (NoSuchMethodException e) {
            System.err.println("错误: 在服务 " + this.name() + " 中未找到方法 '" + methodName + "'");
            // 为了让上层能感知到错误，可以选择抛出自定义异常
            throw new RuntimeException("方法未找到", e);
        } catch (Exception e) {
            System.err.println("错误: 调用方法 '" + methodName + "' 时发生异常");
            throw new RuntimeException("方法调用异常", e);
        }
    }
}