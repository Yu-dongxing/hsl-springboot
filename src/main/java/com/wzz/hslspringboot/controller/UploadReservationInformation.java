package com.wzz.hslspringboot.controller;



import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 预约信息接口控制
 */
@RestController
@RequestMapping("/api/ReservationInformation")
public class UploadReservationInformation {
    private static final Logger log = LogManager.getLogger(UploadReservationInformation.class);
    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;
    /**
     * 上传预约信息
     */
    @PostMapping("/up")
    public Result<?> upload(@RequestBody UserSmsWebSocket user) {
        log.info("上传预约信息：{}", user);
        try{
            userSmsWebSocketService.save(user);
            return Result.success("上传成功");
        } catch (Exception e) {
            log.error("上传失败：{}", e.getMessage());
            return Result.error("上传失败");
        }

    }
    /**
     * 获取预约信息列表，可根据任务状态进行筛选
     * @param taskStatus 任务状态 (可选参数)
     */
    @GetMapping("/all")
    public Result<?> getAll(@RequestParam(value = "taskStatus", required = false) String taskStatus) {
        // 调用 service 层修改后的方法，传入可能为 null 的 taskStatus
        List<UserSmsWebSocket> list = userSmsWebSocketService.getAll(taskStatus);
        if (list != null && !list.isEmpty()) {
            return Result.success(list);
        }
        return Result.error("没有找到相关数据");
    }
    /**
     * 修改信息接口
     * @param user 包含要更新字段和ID的UserSmsWebSocket对象
     * @return Result<?> 操作结果
     */
    @PostMapping("/update") // 使用PUT请求来表示更新操作
    public Result<?> update(@RequestBody UserSmsWebSocket user) {
        // 1. **关键校验**：更新操作必须提供记录的ID
        if (user.getId() == null) {
            return Result.error("修改失败，必须提供记录的ID");
        }

        try {
            user.setTaskStatus("待处理");
            // 2. 调用Service层的更新方法
            boolean isSuccess = userSmsWebSocketService.Update(user);

            // 3. 根据Service层返回的结果，判断更新是否成功
            if (isSuccess) {
                return Result.success("修改成功");
            } else {
                // isSuccess为false通常意味着数据库中没有找到对应ID的记录，所以0行受影响
                return Result.error("修改失败，未找到对应记录或数据无变化");
            }
        } catch (Exception e) {
            // 4. 捕获并记录未知异常
            log.error("修改信息时发生异常, user_id: {}, error: {}", user.getId(), e.getMessage(), e);
            return Result.error("修改失败，服务器内部错误");
        }
    }
    /**
     * 根据id删除
     */
    @GetMapping("/delete/{id}")
    public Result<?> deleteById(@PathVariable Long id) {
        userSmsWebSocketService.deleteById(id);
        return Result.success("删除成功");
    }


}
