package com.wzz.hslspringboot.DTO;

import lombok.Data;

@Data
public class WebSocketDTO {
    /**
     * 用户
     */
    private String user;
    /**
     * 服务类
     */
    private String server;
    /**
     * 服务类中的方法
     */
    private String method;
    /**
     * 参数
     */
    private Object args;

}
