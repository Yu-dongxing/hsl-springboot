package com.wzz.hslspringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    //jwt配置
//    @Bean
//    public StpLogic getStpLogicJwt(){
//        return new StpLogicJwtForSimple();
//    }
    // 注册登陆，角色拦截器
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(new SaInterceptor(handler ->{
//            SaRouter.match("/**")
//                    .notMatch("/api/user/register")
//                    .notMatch("/api/user/login")
//                    .notMatch("/api/user/login/captcha")
//                    .notMatch("/api/user/add/admin")
//                    .check(r->{
//                        StpUtil.checkLogin();
//                    });
//        })).addPathPatterns("/**");
//    }
}
