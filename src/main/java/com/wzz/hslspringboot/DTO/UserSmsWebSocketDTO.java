package com.wzz.hslspringboot.DTO;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class UserSmsWebSocketDTO {
    /**
     * 用户手机号
     */
    private String userPhone;
    /**
     * 用户cookie
     */
    private String userCookie;
    /**
     * 当前链接状态(false:未链接，true：已链接)
     */
    private Boolean status;

    /**
     * 用户短信信息
     */
    private String userSmsMessage;
}
