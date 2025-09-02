package com.wzz.hslspringboot.DTO;

import lombok.Data;

@Data
public class UserSmsDTO {
    /**
     * 手机号
     */
    private String userPhone;
    /**
     * 验证码
     */
    private String userSmsMessage;
}
