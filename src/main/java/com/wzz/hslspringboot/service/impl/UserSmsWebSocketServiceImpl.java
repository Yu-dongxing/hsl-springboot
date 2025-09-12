package com.wzz.hslspringboot.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class UserSmsWebSocketServiceImpl implements UserSmsWebSocketService {
    private static final Logger log = LogManager.getLogger(UserSmsWebSocketService.class);
    @Autowired
    private UserSmsWebSocketMapper userSmsWebSocketMapper;

    /**
     * 保存
     */
    @Override
    public void save(UserSmsWebSocket userSmsWebSocket) {
        try {
            UserSmsWebSocket u = ByUserPhoneSelect(userSmsWebSocket.getUserPhone());
            if (u == null) {
                if(StrUtil.isBlank(userSmsWebSocket.getUserCookie())){
                    userSmsWebSocket.setUserCookie(null);
                }
                userSmsWebSocket.setTaskStatus("待处理");
                processJsonStringField(userSmsWebSocket);
                userSmsWebSocketMapper.insert(userSmsWebSocket);
            }else {
                CopyOptions copyOptions = CopyOptions.create().setIgnoreNullValue(true);
                BeanUtil.copyProperties(userSmsWebSocket, u, copyOptions);
                u.setTaskStatus("待处理");
                if(StrUtil.isBlank(u.getUserCookie())){
                    u.setUserCookie(null);
                }
                processJsonStringField(u);
                userSmsWebSocketMapper.updateById(u);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public UserSmsWebSocket getByPhone(String phone){
        LambdaQueryWrapper<UserSmsWebSocket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSmsWebSocket::getUserPhone, phone);
        return userSmsWebSocketMapper.selectOne(queryWrapper);
    }
    /**
     * 新增的私有方法：处理对象中需要转为JSON的字符串字段
     * @param userSmsWebSocket 待处理的对象
     */
    private void processJsonStringField(UserSmsWebSocket userSmsWebSocket) {
        String params = userSmsWebSocket.getUserCookie();
        if (StrUtil.isNotBlank(params)) {
            if (JSONUtil.isTypeJSONObject(params)) {
                JSONObject jsonObject = JSONUtil.parseObj(params);
                userSmsWebSocket.setUserCookie(jsonObject.toString());
            }
        }
    }

    /**
     * 保存用户线上状态
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
     * 优化点：简化了更新逻辑的嵌套，提高了代码的可读性和健壮性。
     *
     * @param incomingSmsData 包含手机号和原始短信内容的对象
     */
    @Transactional
    @Override
    public void saveOrUpdateSmsInfoWithBeanUtil(UserSmsWebSocket incomingSmsData) {
        if (incomingSmsData == null || !StringUtils.hasText(incomingSmsData.getUserPhone())) {
            log.error("传入的短信数据对象为空或手机号为空，处理终止。");
            return;
        }
        String verificationCode = SmsParser.parseVerificationCode(incomingSmsData.getUserSmsMessage());
        if (verificationCode == null) {
            return;
        }
        UserSmsWebSocket existingRecord = ByUserPhoneSelect(incomingSmsData.getUserPhone());
        if (existingRecord == null) {
            UserSmsWebSocket newSmsRecord = new UserSmsWebSocket();
            newSmsRecord.setUserPhone(incomingSmsData.getUserPhone());
            newSmsRecord.setUserSmsMessage(verificationCode);
            newSmsRecord.setUpSmsTime(LocalDateTime.now());
            userSmsWebSocketMapper.insert(newSmsRecord);
            log.info("为手机号 {} 新增验证码记录。", incomingSmsData.getUserPhone());
        } else {
            if (!verificationCode.equals(existingRecord.getUserSmsMessage())) {
                log.info("手机号 {} 的验证码发生变化，旧值: {}, 新值: {}，执行更新。", existingRecord.getUserPhone(), existingRecord.getUserSmsMessage(), verificationCode);
                CopyOptions copyOptions = CopyOptions.create().setIgnoreNullValue(true);
                BeanUtil.copyProperties(incomingSmsData, existingRecord, copyOptions);
                existingRecord.setUserSmsMessage(verificationCode);
                existingRecord.setUpSmsTime(LocalDateTime.now());
                userSmsWebSocketMapper.updateById(existingRecord);
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
        LambdaQueryWrapper<UserSmsWebSocket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.apply(
                "JSON_UNQUOTE(JSON_EXTRACT(user_cookie, '$.mobileDeviceId')) = {0}",
                deviceId
        );
        queryWrapper.last("LIMIT 1");
        return userSmsWebSocketMapper.selectOne(queryWrapper);
    }
    /**
     * 高效地更新指定ID用户的任务状态和详情，并将变更记录追加到用户预约详情日志中。
     * 这种方式在更新必要字段的同时，保留了完整的状态变更历史。
     * @param userId 用户记录的ID
     * @param status 要更新的任务状态
     * @param details 状态的详细描述（例如：错误信息）
     */
    @Override
    public void updateTaskStatus(Long userId, String status, String details) {
        if (userId == null || !StringUtils.hasText(status)) {
            return;
        }
        UserSmsWebSocket currentUserState = userSmsWebSocketMapper.selectById(userId);
        if (currentUserState == null) {
            log.error("尝试更新一个不存在的用户记录，userId: {}", userId);
            return;
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String newLogEntry = String.format("[%s] 任务状态: %s, 详情: %s", timestamp, status, details);
        String existingLogs = currentUserState.getUserLogInfo();
        String updatedLogs;
        if (StringUtils.hasText(existingLogs)) {
            updatedLogs = existingLogs + "<br>" + newLogEntry;
        } else {
            updatedLogs = newLogEntry;
        }
        UserSmsWebSocket updateEntity = new UserSmsWebSocket();
        updateEntity.setId(userId);
        updateEntity.setTaskStatus(status);
        updateEntity.setStatusDetails(details);
        updateEntity.setUserLogInfo(updatedLogs);
        userSmsWebSocketMapper.updateById(updateEntity);
    }
    /**
     * 根据id删除
     */
    @Override
    public void deleteById(Long id){
        userSmsWebSocketMapper.deleteById(id);
    }

    @Override
    public UserSmsWebSocket getById(Long id) {
        LambdaQueryWrapper<UserSmsWebSocket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSmsWebSocket::getId, id);
        return userSmsWebSocketMapper.selectOne(queryWrapper);
    }

    /**
     * 根据id清除用户日志详情
     */
    @Override
    public void clearUserLogInfoById(Long id){
        UserSmsWebSocket u = userSmsWebSocketMapper.selectById(id);
        if (u == null) {
            return;
        }
        u.setUserLogInfo("");
        userSmsWebSocketMapper.updateById(u);
    }
}
