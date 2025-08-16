package com.wzz.hslspringboot.controller;

//import cn.dev33.satoken.annotation.SaCheckRole;
import com.wzz.hslspringboot.DTO.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 测试接口
 */
@RestController
@RequestMapping("/api/cs")
public class csController {
    @GetMapping("/get")
    public Result<String> cs (){
        return Result.success("成功");
    }
}
