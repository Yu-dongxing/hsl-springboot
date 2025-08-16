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
     * 代理商返回数据解析规则
     */
    @TableField("type")
    private String type;
    /**
     * 是否启用（1：启用，0：不使用）
     */
    @TableField("enabled")
    private Boolean enabled;

}
