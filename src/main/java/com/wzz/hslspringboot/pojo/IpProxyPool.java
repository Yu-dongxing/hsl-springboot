package com.wzz.hslspringboot.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * ip代理池
 */
@TableName("ip_proxy_pool")
public class IpProxyPool {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * ip
     */
    @TableField("ip")
    private String ip;
    /**
     * 端口
     */
    @TableField("port")
    private String port;

}
