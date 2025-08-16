package com.wzz.hslspringboot.DTO;

import lombok.Data;

@Data
public class UserSmsWebSocketDTO {
    private String user;
    private String cookie;
    private String server;
    private String method;

}
