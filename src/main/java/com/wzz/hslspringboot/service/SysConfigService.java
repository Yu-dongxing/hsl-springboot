package com.wzz.hslspringboot.service;

import com.wzz.hslspringboot.pojo.NewSysConfig;
import java.util.List;

public interface SysConfigService {

    /**
     * 根据配置名获取配置详情
     * @param configName 配置名
     * @return NewSysConfig 配置实体
     */
    NewSysConfig getConfigByName(String configName);

    /**
     * 新增配置
     * @param newSysConfig 配置实体
     * @return boolean 操作结果
     */
    boolean addConfig(NewSysConfig newSysConfig);

    /**
     * 根据ID删除配置
     * @param id 配置ID
     * @return boolean 操作结果
     */
    boolean deleteConfigById(Long id);

    /**
     * 更新配置
     * @param newSysConfig 配置实体
     * @return boolean 操作结果
     */
    boolean updateConfig(NewSysConfig newSysConfig);

    /**
     * 根据ID查询配置
     * @param id 配置ID
     * @return NewSysConfig 配置实体
     */
    NewSysConfig getConfigById(Long id);

    /**
     * 获取所有配置列表
     * @return List<NewSysConfig> 配置列表
     */
    List<NewSysConfig> listAllConfigs();
}