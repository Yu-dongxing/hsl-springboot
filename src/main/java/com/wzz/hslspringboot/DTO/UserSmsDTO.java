package com.wzz.hslspringboot.DTO;

import com.fasterxml.jackson.databind.JsonNode;
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

    private JsonNode data;
}
