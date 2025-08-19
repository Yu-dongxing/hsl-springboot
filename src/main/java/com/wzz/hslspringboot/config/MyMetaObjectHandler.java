package com.wzz.hslspringboot.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component // 必须将处理器注册为 Spring Bean
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入元数据填充
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("start insert fill ....");

        // 官方推荐用法，严格填充（字段不存在时会报错）
        // this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 或者使用 setFieldValByName，更常用，当字段不存在时不会报错
        // 第一个参数是实体类中的属性名，不是数据库中的字段名
        this.setFieldValByName("createTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);

        // 也可以先判断是否存在该字段，再进行填充
        // if (metaObject.hasSetter("createTime")) {
        //     this.setFieldValByName("createTime", LocalDateTime.now(), metaObject);
        // }
    }

    /**
     * 更新元数据填充
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("start update fill ....");
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }
}