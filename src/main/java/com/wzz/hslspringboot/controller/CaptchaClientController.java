package com.wzz.hslspringboot.controller;

import com.alibaba.fastjson.JSONObject;
import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.DTO.UserSmsDTO;
import com.wzz.hslspringboot.apis.Function;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/captcha")
public class CaptchaClientController {
    @Autowired
    private Function function;
    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;
    /**
     * 用户请求新的验证码接口
     * @param upDTO
     * @return
     */
    @PostMapping("/getCaptcha")
    public String getCaptcha(@RequestBody UserSmsDTO upDTO) {
        UserSmsWebSocket u= userSmsWebSocketService.getByPhone(upDTO.getUserPhone());

        if(u==null){
            return "未初始化 请等待";
        }
        RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(u);
        requestHeaderUtil.setPzmxnm(u.getPzmxnm());
        requestHeaderUtil.setPhone(u.getUserPhone());
        JSONObject rrrr = function.getCaptcha(requestHeaderUtil);
        return rrrr.toString();
    }

    @PostMapping("/checkCaptcha")
    public String checkCaptcha(@RequestBody UserSmsDTO upDTO) {
        System.out.println("验证码参数"+upDTO);
        UserSmsWebSocket u= userSmsWebSocketService.getByPhone(upDTO.getUserPhone());
        if(u==null){
            return "未初始化 请等待";
        }
        RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(u);
        requestHeaderUtil.setPhone(u.getUserPhone());
        JSONObject rrrr = function.checkCaptcha(requestHeaderUtil,upDTO.getData().toString());
        if (rrrr!=null&&!rrrr.getJSONObject("data").getString("uuid").isEmpty()){
            u.setUuid(rrrr.getJSONObject("data").getString("uuid"));
            u.setNeedCaptcha("0");
            userSmsWebSocketService.save(u);
        }
        return rrrr.toString();
    }

    @PostMapping("/needCaptcha")
    public Result<?> needCaptcha(@RequestBody UserSmsDTO upDTO) {
        UserSmsWebSocket u= userSmsWebSocketService.getByPhone(upDTO.getUserPhone());
        if(u==null) {
            return Result.error("未初始化");
        }
        return Result.success(u.getNeedCaptcha()==null?0:Integer.parseInt(u.getNeedCaptcha()));
    }

}
