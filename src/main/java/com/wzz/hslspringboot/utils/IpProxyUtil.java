package com.wzz.hslspringboot.utils;

import cn.hutool.core.annotation.Alias;
import com.wzz.hslspringboot.pojo.NewSysConfig;
import com.wzz.hslspringboot.service.SysConfigService;
import com.wzz.hslspringboot.service.impl.SysConfigServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class IpProxyUtil {

    private static final Logger log = LogManager.getLogger(IpProxyUtil.class);

    @Autowired
    private SysConfigService sysConfigService;

    // 注入RestTemplate，这是Spring推荐的HTTP客户端
    @Autowired
    private RestTemplate restTemplate;
    /**
     * 查询ip代理配置是否启用
     */
    public Boolean isProxy(){
        NewSysConfig co = sysConfigService.getConfigByName("sys_config");
        Map<String, Object> configValue = (co != null) ? co.getConfigValue() : null;
        Object value = configValue.get("is_proxy");
        log.info("<UNK>IP是否配置<UNK>"+value);
        return Boolean.valueOf(value.toString());
    }
    /**
     * 获取一个代理的详细信息
     * 请求 http://127.0.0.1:8585/get
     * 该接口返回数据为
     * {
     *   "ip": "218.95.39.66",
     *   "latency_ms": 369.92,
     *   "port": 14100
     * }
     * @return 返回包含代理信息的 Map，如 {"ip":"218.95.39.66", "port":14100, ...}，获取失败则返回 null
     */
    public Map<String, Object> getProxyAsMap() {
        final String apiUrl = "http://127.0.0.1:8585/get";
        log.info("开始请求IP代理接口: " + apiUrl);

        try {
            // 使用RestTemplate发起GET请求，并期望返回一个Map类型的数据
            Map<String, Object> responseMap = restTemplate.getForObject(apiUrl, Map.class);

            // 进行基本的验证，确保返回的Map不是null且包含关键信息
            if (responseMap != null && responseMap.containsKey("ip") && responseMap.containsKey("port")) {
                log.info("成功获取IP代理信息: " + responseMap);
                return responseMap; // 直接返回整个Map对象
            } else {
                // 即使请求成功，但返回的数据格式不正确或为空
                log.warn("获取IP代理失败：响应数据格式不正确或为空。 响应内容: " + responseMap);
                return null;
            }
        } catch (RestClientException e) {
            // 处理所有网络请求相关的异常，如连接超时、无法访问等
            log.error("请求IP代理接口时发生网络异常", e);
            return null;
        } catch (Exception e) {
            // 捕获其他潜在的运行时异常
            log.error("获取IP代理时发生未知错误", e);
            return null;
        }
    }
}