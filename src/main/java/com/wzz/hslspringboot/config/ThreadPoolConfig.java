package com.wzz.hslspringboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {
    /**
     * 定义一个专门用于执行动态任务的虚拟线程池
     * JDK 21+
     * @return ExecutorService
     */
    @Bean("virtualThreadTaskExecutor")
    public ExecutorService virtualThreadTaskExecutor() {
        // 每次提交任务都会创建一个新的虚拟线程，适合处理大量短时、可能阻塞的任务
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}