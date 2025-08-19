package com.wzz.hslspringboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.pojo.IpProxyPool;
import com.wzz.hslspringboot.service.IpProxyPoolService;
import com.wzz.hslspringboot.task.IpProxyTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * ip代理池接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ipProxyPool")
public class IpProxyPoolController {

    @Autowired
    private IpProxyPoolService ipProxyPoolService;

    @Autowired
    private IpProxyTask ipProxyTask; // 注入定时任务类，用于手动触发

    /**
     * 获取一个可用的代理IP
     * @return
     */
    @GetMapping("/getAvailable")
    public Result<IpProxyPool> getAvailableProxy() {
        IpProxyPool proxy = ipProxyPoolService.getAvailableProxy();
        if (proxy != null) {
            // 获取成功后，立即增加其使用次数
            ipProxyPoolService.incrementUsageCount(proxy.getId());
            log.info("提供可用代理IP: {}:{}", proxy.getIp(), proxy.getPort());
            return Result.success(proxy);
        } else {
            return Result.error("当前无可用代理IP，请稍后再试");
        }
    }

    /**
     * 分页获取代理IP列表
     * @param pageNum  页码，默认为1
     * @param pageSize 每页数量，默认为10
     * @return
     */
    @GetMapping("/list")
    public Result<IPage<IpProxyPool>> listProxies(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        IPage<IpProxyPool> pageResult = ipProxyPoolService.getProxyListByPage(pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 根据ID删除一个代理IP
     * @param id
     * @return
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteProxy(@PathVariable Long id) {
        boolean success = ipProxyPoolService.removeProxyById(id);
        if (success) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败或该IP不存在");
    }

    /**
     * 手动触发一次IP拉取任务
     * @return
     */
    @PostMapping("/triggerFetch")
    public Result<String> triggerFetch() {
        log.info("收到手动触发IP拉取任务的请求...");
        // 建议在生产环境中将此任务转为异步执行，避免长时间阻塞HTTP请求
        // 这里为了简单直接同步调用
        ipProxyTask.fetchIpProxy();
        return Result.success("已成功触发IP拉取任务，请稍后查看列表");
    }

    /**
     * 手动触发一次过期IP清理任务
     * @return
     */
    @PostMapping("/triggerCleanup")
    public Result<String> triggerCleanup() {
        log.info("收到手动触发过期IP清理任务的请求...");
        ipProxyTask.cleanupExpiredProxies();
        return Result.success("已成功触发过期IP清理任务");
    }
}