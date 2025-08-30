package com.wzz.hslspringboot.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import com.wzz.hslspringboot.apis.Function;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.utils.EncryptionUtil;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 预约处理器服务
 * 封装了完整的单次预约逻辑，设计为线程安全，可供多线程调用。
 */
public interface AppointmentProcessorService {


    JSONObject processAppointment(UserSmsWebSocket user) throws IOException, InterruptedException;

    boolean preProcessCheck(UserSmsWebSocket user);
}