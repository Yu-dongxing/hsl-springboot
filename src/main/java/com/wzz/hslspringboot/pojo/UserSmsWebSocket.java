package com.wzz.hslspringboot.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.wzz.hslspringboot.annotation.ColumnType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_sms_web_socket")
public class UserSmsWebSocket {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 用户websocketid
     */
    @TableField("user_web_socket_id")
    private String userWebSocketId;

    /**
     * 用户手机号
     */
    @TableField("user_phone")
    private String userPhone;
    /**
     * 用户cookie
     */
    @ColumnType("JSON")
    @TableField("user_cookie")
    private String userCookie;
    /**
     * 当前链接状态(false:未链接，true：已链接)
     */
    @TableField("status")
    private Boolean status;

    /**
     * 用户短信信息
     */
    @TableField("user_sms_message")
    private String userSmsMessage;

    /**
     * 获取手机信息时间
     */
    @TableField("up_sms_time")
    private LocalDateTime upSmsTime;
    /**
     * 上传信息时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 用户密码
     */
    @TableField("user_password")
    private String userPassword;
    /**
     * 用户位置
     */
    @TableField("user_location")
    private String userLocation;
    /**
     * 用户经度
     */
    @TableField("user_longitude")
    private String userLongitude;
    /**
     * 用户纬度
     */
    @TableField("user_latitude")
    private String userLatitude;
    /**
     * 用户身份证
     */
    @TableField("user_id_card")
    private String userIdCard;
    /**
     * 用户姓名
     */
    @TableField("user_name")
    private String userName;

    /**
     * 预约时间
     */
    @TableField("appointment_time")
    private String appointmentTime;

    /**
     * 车牌号
     */
    @TableField("vehicle_license_plate_number")
    private String vehicleLicensePlateNumber;

    /**
     * 目标粮仓
     */
    @TableField("target_granary")
    private String targetGranary;

    /**
     * 车船类型
     */
    @TableField("vehicle_and_vessel_type")
    private String vehicleAndVesselType;

    /**
     * 车船数量（任务涉及的车辆或船只数量）
     */
    @TableField("number_of_vehicles_and_ships")
    private Integer numberOfVehiclesAndShips;

    /**
     * 粮食品种
     */
    @TableField("grain_varieties")
    private String grainVarieties;

    /**
     * 是否成功接收短信（1：成功，0：失败，-1：异常）
     */
    @TableField("receive_sms_status")
    private String receiveSmsStatus;

    /**
     * 用户账号的登录状态（0：已登录，1：未登录，-1：登陆异常）
     */
    @TableField("account_login_status")
    private String accountLoginStatus;

    /**
     * 状态详情，描述当前任务或预约的详细状态信息（登陆异常原因，短信异常原因）
     */
    @TableField("status_details")
    private String statusDetails;

    /**
     * 任务状态，用于记录任务当前的进展情况（例如：进行中、已完成、已取消等）
     */
    @TableField("task_status")
    private String taskStatus;
    /**
     * 粮食预约日期
     */
    @TableField("food_reservation_date")
    private String foodReservationDate;

    /**
     * 粮食吨数
     */
    @TableField("food_of_grain_num")
    private String foodOfGrainNum;
    /**
     * 用户预约详情日志
     */
    @TableField("user_log_info")
    @ColumnType("TEXT")
    private String userLogInfo;

    /**
     * 短信验证码定时时间
     */
    @TableField("sms_code_time")
    private String smsCodeTime;
    /**
     * 需求图片验证码的状态
     */
    @TableField("need_captcha")
    private String needCaptcha;
    /**
     * 用户验证的uuid
     */
    @TableField("uuid")
    private String uuid;

}
