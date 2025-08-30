package com.wzz.hslspringboot.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.wzz.hslspringboot.annotation.ColumnType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_sms_web_socket")
public class UserSmsWebSocket {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     *
     */
    @TableField("user_web_socket_id")
    private String userWebSocketId;

    /**
     *
     */
    @TableField("user_phone")
    private String userPhone;
    /**
     *
     */
    @ColumnType("JSON")
    @TableField("user_cookie")
    private String userCookie;
    /**
     */
    @TableField("status")
    private Boolean status;

    /**
     *
     */
    @TableField("user_sms_message")
    private String userSmsMessage;

    /**
     *
     */
    @TableField("up_sms_time")
    private LocalDateTime upSmsTime;
    /**
     *
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     *
     */
    @TableField("user_password")
    private String userPassword;
    /**
     *
     */
    @TableField("user_location")
    private String userLocation;
    /**
     *
     */
    @TableField("user_longitude")
    private String userLongitude;
    /**
     *
     */
    @TableField("user_latitude")
    private String userLatitude;
    /**
     *
     */
    @TableField("user_id_card")
    private String userIdCard;
    /**
     *
     */
    @TableField("user_name")
    private String userName;

    /**
     *
     */
    @TableField("appointment_time")
    private String appointmentTime;

    /**
     *
     */
    @TableField("vehicle_license_plate_number")
    private String vehicleLicensePlateNumber;

    /**
     *
     */
    @TableField("target_granary")
    private String targetGranary;

    /**
     *
     */
    @TableField("vehicle_and_vessel_type")
    private String vehicleAndVesselType;

    /**
     */
    @TableField("number_of_vehicles_and_ships")
    private Integer numberOfVehiclesAndShips;

    /**
     *
     */
    @TableField("grain_varieties")
    private String grainVarieties;

    /**
     */
    @TableField("receive_sms_status")
    private String receiveSmsStatus;

    /**
     */
    @TableField("account_login_status")
    private String accountLoginStatus;

    /**
     */
    @TableField("status_details")
    private String statusDetails;

    /**
     */
    @TableField("task_status")
    private String taskStatus;
    /**
     *
     */
    @TableField("food_reservation_date")
    private String foodReservationDate;

    /**
     *
     */
    @TableField("food_of_grain_num")
    private String foodOfGrainNum;
}
