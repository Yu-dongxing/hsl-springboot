package com.wzz.hslspringboot.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_sms_web_socket")
public class UserSmsWebSocket {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 用户websocketid
     */
    @TableField("user_web_socket_id")
    private String userWebSocketId;

    /**
     * 用户手机号
     */
    @TableField("user_phone")
    private String userPhone;
    /**
     * 用户cookie
     */
    @TableField("user_cookie")
    private String userCookie;
    /**
     * 当前链接状态(false:未链接，true：已链接)
     */
    @TableField("status")
    private Boolean status;
}
