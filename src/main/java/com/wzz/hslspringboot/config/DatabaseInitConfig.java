package com.wzz.hslspringboot.config;

import com.wzz.hslspringboot.service.DatabaseInitService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

@Configuration
public class DatabaseInitConfig {

    private final DatabaseInitService databaseInitService;
    public DatabaseInitConfig(DatabaseInitService databaseInitService) {
        this.databaseInitService = databaseInitService;
    }
    @Bean
    public DatabaseInitService initDatabase() throws SQLException {
        databaseInitService.initDatabase();
        return databaseInitService; // 返回数据库初始化服务的实例
    }
}