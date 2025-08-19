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

@Service
public class UserSmsWebSocketService {
    private static final Logger log = LogManager.getLogger(UserSmsWebSocketService.class);
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
     * 保存或更新手机短信信息，此版本保留并优化了 BeanUtil.copyProperties 的使用。
     *
     * @param incomingSmsData 包含手机号和原始短信内容的对象
     */
    @Transactional // 1. 添加事务注解，保证操作的原子性
    public void saveOrUpdateSmsInfoWithBeanUtil(UserSmsWebSocket incomingSmsData) {
        // 2. 健壮性前置检查
        if (incomingSmsData == null || !StringUtils.hasText(incomingSmsData.getUserPhone())) {
            // 在实际项目中，这里应该记录日志
            log.error("传入的短信数据对象为空或手机号为空");
            return;
        }
        // 3. 先解析验证码，将结果保存到局部变量，保持输入参数不变
        String verificationCode = null;
        if (StringUtils.hasText(incomingSmsData.getUserSmsMessage())) {
            verificationCode = SmsParser.parseVerificationCode(incomingSmsData.getUserSmsMessage());
        }

        // 4. 根据手机号查询数据库，判断是新增还是更新
        UserSmsWebSocket existingRecord = ByUserPhoneSelect(incomingSmsData.getUserPhone());

        if (existingRecord == null) {
            // --- 记录不存在，执行插入操作 ---
            // 直接使用传入的对象，但将 message 字段替换为解析后的验证码
            incomingSmsData.setUserSmsMessage(verificationCode);
            userSmsWebSocketMapper.insert(incomingSmsData);
        } else {
            // --- 记录已存在，执行更新操作 ---
            // 5. 配置拷贝选项：忽略源对象中的 null 值
            // 这是安全使用 copyProperties 的关键，防止 null 值覆盖数据库中已有的数据
            CopyOptions copyOptions = CopyOptions.create().setIgnoreNullValue(true);
            // 6. 执行拷贝：将传入对象(source)的非空属性，覆盖到从数据库查出的对象(target)上
            BeanUtil.copyProperties(incomingSmsData, existingRecord, copyOptions);
            // 7. 明确设置解析后的验证码
            // 这一步是必须的，因为源对象中的 message 字段是原始短信，我们需要用解析结果覆盖它
            existingRecord.setUserSmsMessage(verificationCode);
            // 8. 执行更新
            // 使用 existingRecord 进行更新，因为它包含了正确的数据库ID和合并后的最新数据
            userSmsWebSocketMapper.updateById(existingRecord);
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

    public List<UserSmsWebSocket> getAll() {
        return userSmsWebSocketMapper.selectList(null);
    }
}
