package com.wzz.hslspringboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wzz.hslspringboot.mapper.SysConfigMapper;
import com.wzz.hslspringboot.pojo.NewSysConfig;
import com.wzz.hslspringboot.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
public class SysConfigServiceImpl implements SysConfigService {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    /**
     * 根据配置名获取配置详情
     * Mybatis-Plus会把驼峰命名的configName自动映射到数据库的config_name字段
     */
    @Override
    public NewSysConfig getConfigByName(String configName) {
        Assert.hasText(configName, "配置名不能为空");
        QueryWrapper<NewSysConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_name", configName);
        return sysConfigMapper.selectOne(queryWrapper);
    }

    /**
     * 新增配置
     */
    @Override
    public boolean addConfig(NewSysConfig newSysConfig) {
        // 简单校验
        Assert.notNull(newSysConfig, "配置实体不能为空");
        Assert.hasText(newSysConfig.getConfigName(), "配置名不能为空");

        // 检查配置名是否已存在
        NewSysConfig existingConfig = this.getConfigByName(newSysConfig.getConfigName());
        if (existingConfig != null) {
            throw new IllegalArgumentException("配置名 '" + newSysConfig.getConfigName() + "' 已存在");
        }

        return sysConfigMapper.insert(newSysConfig) > 0;
    }

    /**
     * 根据ID删除配置
     */
    @Override
    public boolean deleteConfigById(Long id) {
        Assert.notNull(id, "配置ID不能为空");
        return sysConfigMapper.deleteById(id) > 0;
    }

    /**
     * 更新配置
     */
    @Override
    public boolean updateConfig(NewSysConfig newSysConfig) {
        Assert.notNull(newSysConfig, "配置实体不能为空");
        Assert.notNull(newSysConfig.getId(), "更新时配置ID不能为空");
        return sysConfigMapper.updateById(newSysConfig) > 0;
    }

    /**
     * 根据ID查询配置
     */
    @Override
    public NewSysConfig getConfigById(Long id) {
        Assert.notNull(id, "配置ID不能为空");
        return sysConfigMapper.selectById(id);
    }

    /**
     * 获取所有配置列表
     */
    @Override
    public List<NewSysConfig> listAllConfigs() {
        return sysConfigMapper.selectList(null);
    }
}