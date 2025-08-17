package com.wzz.hslspringboot.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
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
        UserSmsWebSocket u = ByUserPhoneSelect(userSmsWebSocket.getUserPhone());
        if (u == null) {
            userSmsWebSocketMapper.insert(userSmsWebSocket);
        }else {
            CopyOptions copyOptions = CopyOptions.create().setIgnoreNullValue(true);
            // 2. 执行拷贝
            // 源: userSmsWebSocket (传入的、可能不完整的对象)
            // 目标: u(从数据库查出的、带有完整ID和数据的对象)
            BeanUtil.copyProperties(userSmsWebSocket, u, copyOptions);
            // 3. 使用带有正确ID和更新后字段的 u 对象进行更新
            userSmsWebSocketMapper.updateById(u);
        }

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
