package com.wzz.hslspringboot.controller;

import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.DTO.UserSmsDTO;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户短信验证码接口控制
 */
@RestController
@RequestMapping("/api/sms")
public class UserSmsController {
    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    /**
     * 上传用户短信验证码接口
     * @param upDTO
     * @return
     */
    @PostMapping("/up")
    public Result<?> up(@RequestBody UserSmsDTO upDTO) {
        UserSmsWebSocket u= new UserSmsWebSocket();
        u.setUserPhone(upDTO.getUserPhone());
        u.setUserSmsMessage(upDTO.getUserSmsMessage());
        userSmsWebSocketService.saveOrUpdateSmsInfoWithBeanUtil(u);
        return Result.success("用户验证码上传成功");
    }
}
