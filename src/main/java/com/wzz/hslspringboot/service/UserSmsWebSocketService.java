package com.wzz.hslspringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzz.hslspringboot.mapper.UserSmsWebSocketMapper;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserSmsWebSocketService {
    @Autowired
    private UserSmsWebSocketMapper userSmsWebSocketMapper;

    /**
     * 保存
     * @param userSmsWebSocket
     */
    public void save(UserSmsWebSocket userSmsWebSocket) {
        userSmsWebSocketMapper.insert(userSmsWebSocket);
    }
    /**
     * 根据用户手机号查询该记录是否存在
     */
    public UserSmsWebSocket ByUserPhoneSelect(String phone) {
        LambdaQueryWrapper<UserSmsWebSocket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSmsWebSocket::getUserPhone, phone);
        return userSmsWebSocketMapper.selectOne(queryWrapper);
    }
    /**
     * 更新信息
     */
    public Boolean Update(UserSmsWebSocket userSmsWebSocket) {
        return  userSmsWebSocketMapper.updateById(userSmsWebSocket) > 0;
    }
    /**
     * 根据session id 查询当前信息
     */
    public UserSmsWebSocket SelectBySessionId(String id){
        LambdaQueryWrapper<UserSmsWebSocket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSmsWebSocket::getUserWebSocketId,id);
        return userSmsWebSocketMapper.selectOne(queryWrapper);
    }

 }
