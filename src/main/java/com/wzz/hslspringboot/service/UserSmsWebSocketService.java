package com.wzz.hslspringboot.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzz.hslspringboot.mapper.UserSmsWebSocketMapper;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.utils.SmsParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

public interface UserSmsWebSocketService {

    void save(UserSmsWebSocket userSmsWebSocket);

    @Transactional // 1. 添加事务注解，保证操作的原子性
    void saveOrUpdateSmsInfoWithBeanUtil(UserSmsWebSocket incomingSmsData);

    UserSmsWebSocket ByUserPhoneSelect(String phone);

    Boolean Update(UserSmsWebSocket userSmsWebSocket);

    UserSmsWebSocket SelectBySessionId(String id);

    List<UserSmsWebSocket> getAll(String taskStatus);

    UserSmsWebSocket selectByDeviceId(String deviceId);
}
