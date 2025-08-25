package com.wzz.hslspringboot.pojo;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wzz.hslspringboot.handler.HutoolJsonObjectTypeHandler;
import lombok.Data;

/**
 * 系统配置
 */
@Data
@TableName(value = "new_sys_config", autoResultMap = true)
public class NewSysConfig {
    /**
     * 配置id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 配置名
     */
    @TableField("config_name")
    private String configName;
    /*
     * 配置详情（json格式）
     */
    @TableField(value = "config_value", typeHandler = HutoolJsonObjectTypeHandler.class)
    private JSONObject configValue;

}
