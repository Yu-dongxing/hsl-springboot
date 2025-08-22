package com.wzz.hslspringboot.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzz.hslspringboot.mapper.UserSmsWebSocketMapper;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import com.wzz.hslspringboot.utils.SmsParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserSmsWebSocketServiceImpl implements UserSmsWebSocketService {
    private static final Logger log = LogManager.getLogger(UserSmsWebSocketService.class);
    @Autowired
    private UserSmsWebSocketMapper userSmsWebSocketMapper;

    /**
     * 保存
     * @param userSmsWebSocket
     */
    @Override
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
    @Transactional
    @Override
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
     * 根据用户手机号
     */
    @Override
    public UserSmsWebSocket ByUserPhoneSelect(String phone) {
        LambdaQueryWrapper<UserSmsWebSocket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSmsWebSocket::getUserPhone, phone);
        return userSmsWebSocketMapper.selectOne(queryWrapper);
    }
    /**
     * 更新信息
     */
    @Override
    public Boolean Update(UserSmsWebSocket userSmsWebSocket) {
        return  userSmsWebSocketMapper.updateById(userSmsWebSocket) > 0;
    }
    /**
     * 根据session id 查询当前信息
     */
    @Override
    public UserSmsWebSocket SelectBySessionId(String id){
        LambdaQueryWrapper<UserSmsWebSocket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSmsWebSocket::getUserWebSocketId,id);
        return userSmsWebSocketMapper.selectOne(queryWrapper);
    }

    /**
     * 根据任务状态获取预约信息列表
     * @param taskStatus 任务状态 (例如：进行中、已完成、已取消等)，如果为 null 或空字符串，则查询所有
     * @return 预约信息列表
     */
    @Override
    public List<UserSmsWebSocket> getAll(String taskStatus) {
        // 1. 创建 LambdaQueryWrapper 查询构造器
        LambdaQueryWrapper<UserSmsWebSocket> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(taskStatus)) {
            queryWrapper.eq(UserSmsWebSocket::getTaskStatus, taskStatus);
        }

        return userSmsWebSocketMapper.selectList(queryWrapper);
    }
    /**
     * 根据设备ID查询
     * 从UserSmsWebSocket中的cookie（json字符串格式）字段中根据传入的设备id查询符合字段mobileDeviceId的值的数据
     * @param deviceId 要查询的设备ID (例如: "os7mus9HY8oa5IQjlAevxA5YdUVM")
     * @return UserSmsWebSocket 实体对象，如果找不到则返回null
     */
    @Override
    public UserSmsWebSocket selectByDeviceId(String deviceId) {
        // 1. 创建 LambdaQueryWrapper 对象
        LambdaQueryWrapper<UserSmsWebSocket> queryWrapper = new LambdaQueryWrapper<>();

        // 2. 构建查询条件
        // 从JSON中提取 'mobileDeviceId' 的值 (例如: "mobileDeviceId=os7mus9HY8oa5IQjlAevxA5YdUVM")
        // 然后和 'mobileDeviceId=' + 传入的deviceId 拼接后的字符串进行比较
        queryWrapper.apply(
                "JSON_UNQUOTE(JSON_EXTRACT(user_cookie, '$.mobileDeviceId')) LIKE {0}",
                "%"+deviceId + "%" // 注意这里，通常模糊匹配是 deviceId% 或者 %deviceId%
        );

        // 3. 执行查询
        UserSmsWebSocket userSmsWebSocket = userSmsWebSocketMapper.selectOne(queryWrapper);

        // 4. 返回结果
        return userSmsWebSocket;
    }
    /**
     * 高效地更新指定ID用户的任务状态和详情。
     * 这种方式只更新必要的字段，比先查询再更新整个对象性能更好。
     * @param userId 用户记录的ID
     * @param status 要更新的任务状态
     * @param details 状态的详细描述（例如：错误信息）
     */
    @Override
    public void updateTaskStatus(Long userId, String status, String details) {
        if (userId == null || !StringUtils.hasText(status)) {
            return; // 基本的参数校验
        }
        UserSmsWebSocket updateEntity = new UserSmsWebSocket();
        updateEntity.setId(userId);
        updateEntity.setTaskStatus(status);
        updateEntity.setStatusDetails(details);
        userSmsWebSocketMapper.updateById(updateEntity);
    }
}
