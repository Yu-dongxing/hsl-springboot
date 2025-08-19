package com.wzz.hslspringboot.modules;

import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ServiceDispatcherInitializer implements ApplicationRunner {

    // 注入所有需要的Service Bean
    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    // 如果你有更多的服务，在这里继续注入
    // @Autowired
    // private OtherService otherService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 将Service Bean和它的Class关联起来
        Map<Class<?>, Object> serviceMap = Map.of(
                UserSmsWebSocketService.class, userSmsWebSocketService
                // OtherService.class, otherService
        );

        // 遍历所有枚举常量，并注入对应的Service Bean
        for (ServiceDispatcher dispatcher : ServiceDispatcher.values()) {
            Object serviceInstance = serviceMap.get(dispatcher.getServiceClass());
            if (serviceInstance != null) {
                dispatcher.setServiceInstance(serviceInstance);
            }
        }
    }
}