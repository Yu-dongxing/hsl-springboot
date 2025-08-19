package com.wzz.hslspringboot.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * ip代理池代理商配置
 */
@Data
@TableName("ip_proxy_pool_config")
public class IpProxyPoolConfig {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 代理商名称
     */
    @TableField("name")
    private String name;
    /**
     * 代理商接口
     */
    @TableField("url")
    private String url;
    /**
     * 返回数据中的端口字段
     */
    @TableField("data_port")
    private String dataPort;
    /**
     * 返回数据中的ip字段
     */
    @TableField("data_ip")
    private String dataIp;
    /**
     * 返回数据中的认证用户字段
     */
    @TableField("data_user")
    private String dataUser;
    /**
     * 返回数据中的认证用户密码字段
     */
    @TableField("data_password")
    private String dataPassword;
    /**
     * 是否启用（1：启用，0：不使用）
     */
    @TableField("enabled")
    private Boolean enabled;

}
