package com.wzz.hslspringboot.modules.demo;


import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Msg {
    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;



    public String m(String msg){
        return msg+"|收到了";
    }

    public String m(){
        return "没有参数";
    }
}
