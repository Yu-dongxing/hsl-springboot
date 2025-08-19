package com.wzz.hslspringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzz.hslspringboot.mapper.IpProxyPoolConfigMapper;
import com.wzz.hslspringboot.pojo.IpProxyPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IpProxyPoolConfigService {
    @Autowired
    private IpProxyPoolConfigMapper ipProxyPoolConfigMapper;

    /**
     * 新增ip代理配置
     * @param ipProxyPoolConfig
     */
    public boolean addIpProxyConfig(IpProxyPoolConfig ipProxyPoolConfig) {
        // 可以在这里添加一些业务校验逻辑，例如检查name或url是否重复
        return ipProxyPoolConfigMapper.insert(ipProxyPoolConfig) > 0;
    }

    /**
     * 根据ID删除配置
     * @param id
     */
    public boolean deleteIpProxyConfigById(Long id) {
        return ipProxyPoolConfigMapper.deleteById(id) > 0;
    }

    /**
     * 更新ip代理配置
     * @param ipProxyPoolConfig
     */
    public boolean updateIpProxyConfig(IpProxyPoolConfig ipProxyPoolConfig) {
        // 确保ID不为空
        if (ipProxyPoolConfig.getId() == null) {
            return false;
        }
        return ipProxyPoolConfigMapper.updateById(ipProxyPoolConfig) > 0;
    }

    /**
     * 根据ID查询配置
     * @param id
     * @return
     */
    public IpProxyPoolConfig getIpProxyConfigById(Long id) {
        return ipProxyPoolConfigMapper.selectById(id);
    }

    /**
     * 查询所有配置列表
     * @return
     */
    public List<IpProxyPoolConfig> listAllIpProxyConfigs() {
        return ipProxyPoolConfigMapper.selectList(null);
    }


    /**
     * 获取启用的ip代理配置数据
     * @return
     */
    public List<IpProxyPoolConfig> GetIpProxyPoolConfigByState() {
        LambdaQueryWrapper<IpProxyPoolConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IpProxyPoolConfig::getEnabled,true);
        return ipProxyPoolConfigMapper.selectList(queryWrapper);
    }

}