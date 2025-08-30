package com.wzz.hslspringboot.modules;

import cn.hutool.core.bean.BeanUtil;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public enum ServiceDispatcher {
    /**
     * 定义服务名和服务对应的Class类型
     */
    USER(UserSmsWebSocketService.class);

    private static final Logger log = LogManager.getLogger(ServiceDispatcher.class);

    @Getter
    private final Class<?> serviceClass;
    // 提供一个setter方法，用于从外部注入Spring管理的Bean
    @Setter
    private Object serviceInstance; // 不再是 final，将由外部注入

    ServiceDispatcher(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Object execute(String methodName, Object... args) {
        // 在执行前，断言serviceInstance已经被注入，防止NPE
        Assert.notNull(serviceInstance, this.name() + " 服务的实例尚未被注入！");

      //log.info("准备在服务 {} 中执行方法 '{}'，参数: {}", this.name(), methodName, Arrays.toString(args));

        // ... (这里的反射逻辑保持不变)
        for (Method method : this.serviceClass.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                try {
                    Object[] convertedArgs = new Object[args.length];
                    Class<?>[] paramTypes = method.getParameterTypes();

                    boolean canConvert = true;
                    for (int i = 0; i < args.length; i++) {
                        Object arg = args[i];
                        Class<?> targetType = paramTypes[i];

                        if (arg instanceof Map && !Map.class.isAssignableFrom(targetType)) {
                            try {
                                convertedArgs[i] = BeanUtil.toBean(arg, targetType);
                            } catch (Exception conversionException) {
                                log.warn("参数类型转换失败: 无法将 Map 转换为 {}。跳过此方法。", targetType.getName());
                                canConvert = false;
                                break;
                            }
                        } else if (targetType.isInstance(arg)) {
                            convertedArgs[i] = arg;
                        } else {
                            canConvert = false;
                            break;
                        }
                    }

                    if (canConvert) {
                        return method.invoke(this.serviceInstance, convertedArgs);
                    }

                } catch (Exception e) {
                    log.error("执行方法 '{}' 时发生异常", methodName, e);
                    throw new RuntimeException("方法调用异常", e);
                }
            }
        }

        log.error("错误: 在服务 {} 中未找到与参数列表兼容的方法 '{}'", this.name(), methodName);
        throw new RuntimeException("方法未找到或参数不匹配");
    }
}