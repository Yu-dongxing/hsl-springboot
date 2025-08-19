package com.wzz.hslspringboot.controller;



import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 预约信息接口控制
 */
@RestController
@RequestMapping("/api/ReservationInformation")
public class UploadReservationInformation {
    private static final Logger log = LogManager.getLogger(UploadReservationInformation.class);
    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;
    /**
     * 上传预约信息
     */
    @PostMapping("/up")
    public Result<?> upload(@RequestBody UserSmsWebSocket user) {
        try{
            userSmsWebSocketService.save(user);
            return Result.success("上传成功");
        } catch (Exception e) {
            log.error("上传失败：{}", e.getMessage());
            return Result.error("上传失败");
        }

    }
    /**
     * 获取所有预约信息
     */
    @GetMapping("/all")
    public Result<?> getAll(){
        List<UserSmsWebSocket> u = userSmsWebSocketService.getAll();
        if(!u.isEmpty()){
            return Result.success(u);
        }
        return Result.error("没有数据");
    }

}
