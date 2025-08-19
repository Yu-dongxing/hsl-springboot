package com.wzz.hslspringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wzz.hslspringboot.mapper.IpProxyPoolMapper;
import com.wzz.hslspringboot.pojo.IpProxyPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IpProxyPoolService {

    @Autowired
    private IpProxyPoolMapper ipProxyPoolMapper;

    /**
     * 批量保存从API获取的代理IP
     * 使用@Transactional注解保证事务性
     * 保存前会进行去重检查，避免插入重复的 IP:Port
     * @param proxyList 待保存的代理IP列表
     */
    @Transactional
    public void saveProxies(List<IpProxyPool> proxyList) {
        if (proxyList == null || proxyList.isEmpty()) {
            return;
        }

        for (IpProxyPool proxy : proxyList) {
            // 根据IP和端口查询是否已存在
            LambdaQueryWrapper<IpProxyPool> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IpProxyPool::getIp, proxy.getIp())
                    .eq(IpProxyPool::getPort, proxy.getPort());

            Long count = ipProxyPoolMapper.selectCount(queryWrapper);
            // 如果不存在，则插入
            if (count == 0) {
                // 初始化使用次数
                proxy.setNumberOfUses(0);
                ipProxyPoolMapper.insert(proxy);
            }
        }
    }

    /**
     * 获取一个可用的代理IP
     * 筛选条件：未过期
     * 排序逻辑：优先使用次数少的，以实现负载均衡
     * @return 一个可用的IpProxyPool对象，若无则返回null
     */
    public IpProxyPool getAvailableProxy() {
        long now = System.currentTimeMillis();
        LambdaQueryWrapper<IpProxyPool> queryWrapper = new LambdaQueryWrapper<>();
        // 筛选出过期时间大于当前时间的代理
        queryWrapper.gt(IpProxyPool::getExpirationTime, now)
                // 按使用次数升序排序
                .orderByAsc(IpProxyPool::getNumberOfUses)
                // 只取一个
                .last("LIMIT 1");

        return ipProxyPoolMapper.selectOne(queryWrapper);
    }

    /**
     * 增加指定IP的使用次数
     * @param proxyId 代理IP的ID
     */
    public void incrementUsageCount(Long proxyId) {
        IpProxyPool proxy = ipProxyPoolMapper.selectById(proxyId);
        if (proxy != null) {
            proxy.setNumberOfUses(proxy.getNumberOfUses() + 1);
            ipProxyPoolMapper.updateById(proxy);
        }
    }

    /**
     * 删除所有已过期的代理IP
     * @return 删除的记录条数
     */
    public int removeExpiredProxies() {
        long now = System.currentTimeMillis();
        LambdaQueryWrapper<IpProxyPool> queryWrapper = new LambdaQueryWrapper<>();
        // 筛选出过期时间小于或等于当前时间的代理
        queryWrapper.le(IpProxyPool::getExpirationTime, now);
        return ipProxyPoolMapper.delete(queryWrapper);
    }
    /**
     * 分页查询代理IP列表
     * @param pageNum 当前页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    public IPage<IpProxyPool> getProxyListByPage(long pageNum, long pageSize) {
        Page<IpProxyPool> page = new Page<>(pageNum, pageSize);
        // 按过期时间倒序、使用次数升序排列
        LambdaQueryWrapper<IpProxyPool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(IpProxyPool::getExpirationTime)
                .orderByAsc(IpProxyPool::getNumberOfUses);

        return ipProxyPoolMapper.selectPage(page, queryWrapper);
    }

    /**
     * 根据ID删除代理IP
     * @param id
     * @return
     */
    public boolean removeProxyById(Long id) {
        return ipProxyPoolMapper.deleteById(id) > 0;
    }
}