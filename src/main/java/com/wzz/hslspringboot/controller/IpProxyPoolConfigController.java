package com.wzz.hslspringboot.controller;

import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.pojo.IpProxyPoolConfig;
import com.wzz.hslspringboot.service.IpProxyPoolConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ip代理池配置接口
 */
@RestController
@RequestMapping("/api/ipProxyPoolConfig")
public class IpProxyPoolConfigController {

    @Autowired
    private IpProxyPoolConfigService ipProxyPoolConfigService;

    /**
     * 新增IP代理商配置
     * @param config 前端传递的JSON格式的配置信息
     * @return 操作结果
     */
    @PostMapping("/add")
    public Result<String> addConfig(@RequestBody IpProxyPoolConfig config) {
        boolean success = ipProxyPoolConfigService.addIpProxyConfig(config);
        if (success) {
            return Result.success("新增配置成功");
        }
        return Result.error("新增配置失败");
    }

    /**
     * 根据ID删除配置
     * @param id 配置的ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteConfig(@PathVariable Long id) {
        boolean success = ipProxyPoolConfigService.deleteIpProxyConfigById(id);
        if (success) {
            return Result.success("删除配置成功");
        }
        return Result.error("删除配置失败或配置不存在");
    }

    /**
     * 更新IP代理商配置
     * @param config 前端传递的JSON格式的配置信息，必须包含id
     * @return 操作结果
     */
    @PutMapping("/update")
    public Result<String> updateConfig(@RequestBody IpProxyPoolConfig config) {
        boolean success = ipProxyPoolConfigService.updateIpProxyConfig(config);
        if (success) {
            return Result.success("更新配置成功");
        }
        return Result.error("更新配置失败，请检查ID是否存在");
    }

    /**
     * 根据ID查询单个配置详情
     * @param id 配置的ID
     * @return 配置详情
     */
    @GetMapping("/get/{id}")
    public Result<IpProxyPoolConfig> getConfigById(@PathVariable Long id) {
        IpProxyPoolConfig config = ipProxyPoolConfigService.getIpProxyConfigById(id);
        if (config != null) {
            return Result.success(config);
        }
        return Result.error("未找到对应的配置信息");
    }

    /**
     * 查询所有IP代理商配置列表
     * @return 配置列表
     */
    @GetMapping("/list")
    public Result<List<IpProxyPoolConfig>> listAllConfigs() {
        List<IpProxyPoolConfig> list = ipProxyPoolConfigService.listAllIpProxyConfigs();
        return Result.success(list);
    }
}