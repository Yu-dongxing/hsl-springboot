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
@RequestMapping("/")
public class csController {
    @GetMapping()
    public Result<String> cs (){
        return Result.success("本站所公布的所有内容均从互联网收集而来，无意侵犯任何人(企业)的利益和权益，仅供个人学习和研究使用，不得用于任何违法及商业用途，否则所有后果由其个人承担，在收集资源时本站会确保其合法合规的情况下进行发布，但第三方链接内的内容不由本站实际控制，如有侵权和违规请及时与管理员联系处理。");
    }
}
