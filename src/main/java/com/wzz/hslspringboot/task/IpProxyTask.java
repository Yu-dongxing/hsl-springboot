package com.wzz.hslspringboot.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.hslspringboot.pojo.IpProxyPool;
import com.wzz.hslspringboot.pojo.IpProxyPoolConfig;
import com.wzz.hslspringboot.service.IpProxyPoolConfigService;
import com.wzz.hslspringboot.service.IpProxyPoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IpProxyTask {

    @Autowired
    private IpProxyPoolConfigService configService;

    @Autowired
    private IpProxyPoolService ipProxyPoolService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper; // Spring Boot自动配置了Jackson的ObjectMapper

    // 定义一个日期时间格式化器来解析 last_time
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 定时从代理商处拉取IP并进行有效性验证
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void fetchIpProxy() {
        log.info("开始执行定时任务：拉取并验证代理IP...");
        // 1. 获取所有启用的代理商配置
        List<IpProxyPoolConfig> configs = configService.GetIpProxyPoolConfigByState();
        if (configs.isEmpty()) {
            log.info("没有启用状态的IP代理商配置，任务结束。");
            return;
        }

        for (IpProxyPoolConfig config : configs) {
            log.info("正在从代理商 [{}] 的接口 [{}] 拉取IP...", config.getName(), config.getUrl());
            try {
                // 2. 发起HTTP请求获取代理IP数据
                String responseBody = restTemplate.getForObject(config.getUrl(), String.class);
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    log.warn("从代理商 [{}] 获取的IP数据为空。", config.getName());
                    continue;
                }

                // 3. 调用独立的方法解析数据
                List<IpProxyPool> parsedProxies = parseIpProxyData(responseBody, config);
                if (parsedProxies.isEmpty()) {
                    log.info("从代理商 [{}] 的响应中没有解析到有效的IP数据。", config.getName());
                    continue;
                }
                log.info("从代理商 [{}] 解析到 {} 个IP，开始进行有效性验证...", config.getName(), parsedProxies.size());

                // 4.【新增】使用并行流对IP进行有效性验证
                List<IpProxyPool> validProxies = parsedProxies.parallelStream()
                        .filter(this::isProxyValid)
                        .collect(Collectors.toList());

                // 5. 批量保存验证通过的IP到数据库
                if (!validProxies.isEmpty()) {
                    ipProxyPoolService.saveProxies(validProxies);
                    log.info("代理商 [{}] -> 拉取 {}, 验证通过 {}, 成功入库。",
                            config.getName(), parsedProxies.size(), validProxies.size());
                } else {
                    log.warn("代理商 [{}] 拉取的 {} 个IP均未通过验证。", config.getName(), parsedProxies.size());
                }

            } catch (Exception e) {
                log.error("从代理商 [{}] 拉取IP时发生异常: {}", config.getName(), e.getMessage(), e);
            }
        }
        log.info("代理IP拉取及验证任务执行完毕。");
    }

    /**
     * 【新增】验证代理IP是否有效的方法
     * 通过访问百度来测试代理，超时时间500ms
     *
     * @param proxy 待验证的代理IP对象
     * @return true 如果有效, false 如果无效
     */
    private boolean isProxyValid(IpProxyPool proxy) {
        final String validationUrl = "http://www.baidu.com";
        final int timeout = 500; // 验证超时时间，单位：毫秒

        try {
            // 1. 为每个验证请求创建一个独立的请求工厂，并设置代理
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            Proxy netProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getIp(), Integer.parseInt(proxy.getPort())));
            requestFactory.setProxy(netProxy);

            // 2. 设置连接和读取超时时间
            requestFactory.setConnectTimeout(timeout);
            requestFactory.setReadTimeout(timeout);

            // 3. 使用配置好的工厂创建RestTemplate实例
            RestTemplate validationTemplate = new RestTemplate(requestFactory);

            // 4. 执行请求并计时
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = validationTemplate.getForEntity(validationUrl, String.class);
            long duration = System.currentTimeMillis() - startTime;

            // 5. 判断状态码和响应时间
            if (response.getStatusCode() == HttpStatus.OK && duration <= timeout) {
                log.debug("IP验证成功: {}:{}, 耗时: {}ms", proxy.getIp(), proxy.getPort(), duration);
                return true;
            } else {
                log.warn("IP验证失败: {}:{}, 状态码: {}, 耗时: {}ms > {}ms", proxy.getIp(), proxy.getPort(), response.getStatusCode(), duration, timeout);
                return false;
            }
        } catch (Exception e) {
            // 捕获所有异常（如连接超时、读取超时、无法访问等）
            log.warn("IP验证异常: {}:{}, 原因: {}", proxy.getIp(), proxy.getPort(), e.getMessage());
            return false;
        }
    }



    /**
     * 将从代理商处获取的响应数据解析为IpProxyPool实体列表
     *
     * @param responseBody 从代理商API获取的原始响应字符串
     * @param config       当前代理商的配置信息
     * @return 解析后的IpProxyPool实体列表
     * @throws JsonProcessingException 如果JSON解析失败
     */
    private List<IpProxyPool> parseIpProxyData(String responseBody, IpProxyPoolConfig config) throws JsonProcessingException {
        // 解析返回的数据 (适配单个对象或数组)
        List<Map<String, Object>> dataList;
        // 尝试判断响应是JSON数组还是JSON对象
        if (responseBody.trim().startsWith("[")) {
            // 是数组，正常解析
            dataList = objectMapper.readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {});
        } else {
            // 是单个对象，包装成只有一个元素的List，方便统一处理
            Map<String, Object> dataMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            dataList = Collections.singletonList(dataMap);
        }

        List<IpProxyPool> proxyList = new ArrayList<>();
        for (Map<String, Object> dataMap : dataList) {
            // 根据配置的字段名，动态提取IP、端口等信息
            IpProxyPool proxy = new IpProxyPool();

            // 从 "proxy" 字段 (例如: "218.77.183.214:5224") 中解析IP和端口
            String proxyStr = Objects.toString(dataMap.get(config.getDataIp()), null); // 假设 dataIp 配置为 "proxy"
            if (proxyStr != null && proxyStr.contains(":")) {
                String[] parts = proxyStr.split(":");
                proxy.setIp(parts[0]);
                proxy.setPort(parts[1]);
            } else {
                log.warn("从代理商 [{}] 获取的代理数据格式不正确，无法解析IP和端口: {}", config.getName(), proxyStr);
                continue; // 跳过这条不合法的数据
            }

            proxy.setUser(Objects.toString(dataMap.get(config.getDataUser()), null));
            proxy.setPassword(Objects.toString(dataMap.get(config.getDataPassword()), null));

            // 处理过期时间
            Object expireValue = dataMap.get("last_time"); // 假设我们用 last_time 来计算过期时间
            if (expireValue instanceof String) {
                try {
                    // 将 "2025-08-18 12:10:28" 转换为时间戳
                    LocalDateTime lastTime = LocalDateTime.parse((String) expireValue, DATETIME_FORMATTER);
                    // 假设代理在最后一次检查后的30分钟后过期 (这里时区使用的是+8，与国内时间一致)
                    proxy.setExpirationTime(lastTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli() + 30 * 60 * 1000);
                } catch (Exception e) {
                    log.warn("解析last_time失败，将使用默认过期时间。last_time: {}", expireValue);
                    // 解析失败，设置一个默认的，比如从现在开始30分钟后过期
                    proxy.setExpirationTime(System.currentTimeMillis() + 30 * 60 * 1000);
                }
            } else {
                // 如果没有过期时间字段，同样设置一个默认的
                proxy.setExpirationTime(System.currentTimeMillis() + 30 * 60 * 1000);
            }

            // 校验基本数据是否完整，确保IP和端口都存在
            if (proxy.getIp() != null && !proxy.getIp().isEmpty() && proxy.getPort() != null && !proxy.getPort().isEmpty()) {
                proxyList.add(proxy);
            }
        }
        return proxyList;
    }


    /**
     * 定时清理数据库中已过期的代理IP
     * cron表达式: "0 0 * * * ?" 表示每小时的0分0秒执行一次（即整点执行）
     */
//    @Scheduled(cron = "0 0 * * * ?")
    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanupExpiredProxies() {
        log.info("开始执行定时任务：清理过期代理IP...");
        try {
            int deletedCount = ipProxyPoolService.removeExpiredProxies();
            log.info("清理任务完成，共删除了 {} 条过期代理IP。", deletedCount);
        } catch (Exception e) {
            log.error("清理过期代理IP时发生异常: {}", e.getMessage());
        }
    }
}