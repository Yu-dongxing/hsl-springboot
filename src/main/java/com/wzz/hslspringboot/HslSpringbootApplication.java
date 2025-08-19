package com.wzz.hslspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HslSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(HslSpringbootApplication.class, args);
    }

}
