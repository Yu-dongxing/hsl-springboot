package com.wzz.hslspringboot.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * ip代理池
 */
@Data
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

    /**
     * 认证用户
     */
    @TableField("user")
    private String user;
    /**
     * 认证密码
     */
    @TableField("password")
    private String password;
    /**
     * 过期时间
     */
    @TableField("expiration_time")
    private Long expirationTime;
    /**
     * 使用次数
     */
    @TableField("number_of_uses")
    private Integer numberOfUses;

    /**
     * 是否启用https(true:启用，false：不使用)
     */
//    @TableField("https")
//    private Boolean https;

}
