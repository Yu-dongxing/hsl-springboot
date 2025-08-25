package com.wzz.hslspringboot.controller;

import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.pojo.NewSysConfig;
import com.wzz.hslspringboot.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置控制
 */
@RestController
@RequestMapping("/api/sys/config")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 新增配置
     * POST /api/sys/config
     */
    @PostMapping
    public Result<?> addConfig(@RequestBody NewSysConfig newSysConfig) {
        try {
            if (sysConfigService.addConfig(newSysConfig)) {
                return Result.success("新增配置成功");
            }
            return Result.error("新增配置失败");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID删除配置
     * DELETE /api/sys/config/{id}
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteConfig(@PathVariable Long id) {
        if (sysConfigService.deleteConfigById(id)) {
            return Result.success("删除配置成功");
        }
        return Result.error("配置不存在或删除失败");
    }

    /**
     * 更新配置
     * PUT /api/sys/config
     */
    @PutMapping
    public Result<?> updateConfig(@RequestBody NewSysConfig newSysConfig) {
        if (newSysConfig.getId() == null) {
            return Result.error("更新时ID不能为空");
        }
        if (sysConfigService.updateConfig(newSysConfig)) {
            return Result.success("更新配置成功");
        }
        return Result.error("配置不存在或更新失败");
    }

    /**
     * 根据配置名查询
     * GET /api/sys/config/by-name?name=xxx
     */
    @GetMapping("/by-name")
    public Result<?> getConfigByName(@RequestParam("name") String configName) {
        NewSysConfig config = sysConfigService.getConfigByName(configName);
        if (config != null) {
            return Result.success(config);
        }
        return Result.error("没有数据？");
    }

    /**
     * 根据ID查询
     * GET /api/sys/config/{id}
     */
    @GetMapping("/{id}")
    public Result<?> getConfigById(@PathVariable Long id) {
        NewSysConfig config = sysConfigService.getConfigById(id);
        if (config != null) {
            return Result.success(config);
        }
        return Result.error("错误？");
    }

    /**
     * 查询所有配置
     * GET /api/sys/config/list
     */
    @GetMapping("/list")
    public Result<?> listAll() {
        List<NewSysConfig> configs = sysConfigService.listAllConfigs();
        return Result.success(configs);
    }
}