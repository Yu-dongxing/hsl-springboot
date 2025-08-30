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

import java.time.LocalDateTime;
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
            userSmsWebSocket.setTaskStatus("待处理");
            userSmsWebSocketMapper.insert(userSmsWebSocket);
        }else {
            CopyOptions copyOptions = CopyOptions.create().setIgnoreNullValue(true);
            // 2. 执行拷贝
            // 源: userSmsWebSocket (传入的、可能不完整的对象)
            // 目标: u(从数据库查出的、带有完整ID和数据的对象)
            BeanUtil.copyProperties(userSmsWebSocket, u, copyOptions);
            u.setTaskStatus("待处理");
            // 3. 使用带有正确ID和更新后字段的 u 对象进行更新
            userSmsWebSocketMapper.updateById(u);
        }

    }

    /**
     * 保存用户线上状态
     * @param userSmsWebSocket
     */
    @Override
    public void saveUserWsaocket(UserSmsWebSocket userSmsWebSocket) {
        UserSmsWebSocket u = ByUserPhoneSelect(userSmsWebSocket.getUserPhone());
        if (u == null) {
            userSmsWebSocketMapper.insert(userSmsWebSocket);
        }else {
            CopyOptions copyOptions = CopyOptions.create().setIgnoreNullValue(true);
            BeanUtil.copyProperties(userSmsWebSocket, u, copyOptions);
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
        // 1. 健壮性前置检查
        if (incomingSmsData == null || !StringUtils.hasText(incomingSmsData.getUserPhone())) {
            log.error("传入的短信数据对象为空或手机号为空");
            return;
        }
        String smsMessage = incomingSmsData.getUserSmsMessage();
        // 2. 解析验证码，如果无法解析，则直接返回
        String verificationCode = SmsParser.parseVerificationCode(smsMessage);
        if (verificationCode == null) {
            //log.warn("未能从手机号 {} 的短信内容中解析出验证码，忽略该条消息。{}", incomingSmsData.getUserPhone(),incomingSmsData);
            return; // 提前返回，使后续逻辑更清晰
        }
        // 3. 根据手机号查找现有记录
        UserSmsWebSocket existingRecord = ByUserPhoneSelect(incomingSmsData.getUserPhone());
        LocalDateTime now = LocalDateTime.now();

        if (incomingSmsData == null) {
            // 4. 新增逻辑：创建一个新的持久化对象
            UserSmsWebSocket newSmsRecord = new UserSmsWebSocket();
            newSmsRecord.setUserPhone(incomingSmsData.getUserPhone());
            newSmsRecord.setUserSmsMessage(verificationCode); // 只存储解析后的验证码
            newSmsRecord.setUpSmsTime(now);
            userSmsWebSocketMapper.insert(newSmsRecord);

        } else {
            if (incomingSmsData.getUserSmsMessage()!=null){
                if(existingRecord!=null&&existingRecord.getUserSmsMessage()!=null&&existingRecord.getUserSmsMessage().equals(verificationCode)) {
                }else {
               log.info("手机号 {} 的验证码发生变化，旧值: {}, 新值: {}，执行更新。", existingRecord.getUserPhone(), existingRecord.getUserSmsMessage(), verificationCode);
                    CopyOptions copyOptions = CopyOptions.create().setIgnoreNullValue(true);
                    BeanUtil.copyProperties(incomingSmsData, existingRecord, copyOptions);
                    existingRecord.setUserSmsMessage(verificationCode);
                    existingRecord.setUpSmsTime(LocalDateTime.now());
                    userSmsWebSocketMapper.updateById(existingRecord);
                }
            }

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
        queryWrapper.isNotNull(UserSmsWebSocket::getUserName)
                .ne(UserSmsWebSocket::getUserName, "")
                .isNotNull(UserSmsWebSocket::getUserIdCard)
                .ne(UserSmsWebSocket::getUserIdCard, "")
                .isNotNull(UserSmsWebSocket::getUserPhone)
                .ne(UserSmsWebSocket::getUserPhone, "");


        return userSmsWebSocketMapper.selectList(queryWrapper);
    }
    /**
     * 根据设备ID查询
     * <p>
     * 从UserSmsWebSocket中的cookie（json字符串格式）字段中根据传入的设备id查询符合字段mobileDeviceId的值的数据
     *
     * @param deviceId 要查询的设备ID (例如: "os7mus9HY8oa5IQjlAevxA5YdUVM")
     * @return UserSmsWebSocket 实体对象，如果找不到则返回null
     */
    @Override
    public UserSmsWebSocket selectByDeviceId(String deviceId) {
        // 1. 创建 LambdaQueryWrapper 对象
        LambdaQueryWrapper<UserSmsWebSocket> queryWrapper = new LambdaQueryWrapper<>();

        // 2. 构建查询条件 - 优先使用精确匹配
        // 通常根据设备ID查询是精确查找，性能更好，也更符合业务逻辑
        queryWrapper.apply(
                "JSON_UNQUOTE(JSON_EXTRACT(user_cookie, '$.mobileDeviceId')) = {0}",
                deviceId
        );

        // 3. 在SQL语句末尾添加 "LIMIT 1"，确保数据库层面最多只返回一条记录
        // 这是防止 `selectOne` 抛出 TooManyResultsException 的最佳实践
        queryWrapper.last("LIMIT 1");

        // 4. 执行查询
        // 因为有 "LIMIT 1" 的保证，即使有多条数据满足条件，这里也绝对不会抛出异常
        UserSmsWebSocket userSmsWebSocket = userSmsWebSocketMapper.selectOne(queryWrapper);

        // 5. 返回结果
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

    /**
     * 根据id删除
     */
    @Override
    public void deleteById(Long id){
        userSmsWebSocketMapper.deleteById(id);
    }
}
