package com.wzz.hslspringboot;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.hslspringboot.Captcha.Captcha;
import com.wzz.hslspringboot.Captcha.CaptchaData;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import com.wzz.hslspringboot.apis.Function;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.service.AppointmentProcessorService;
import com.wzz.hslspringboot.service.UserSmsWebSocketService;
import com.wzz.hslspringboot.utils.DataConverterUtil;
import com.wzz.hslspringboot.utils.EncryptionUtil;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;


@Component
@SpringBootTest
class HslSpringbootApplicationTests {
    private static final Logger log = LogManager.getLogger(HslSpringbootApplicationTests.class);

//    EncryptionUtil encryptionUtil = new EncryptionUtil();

//    Function function = new Function();

    @Autowired
    private Function function;


    @Autowired
    private AppointmentProcessorService appointmentProcessorService;

    @Autowired
    private UserSmsWebSocketService userSmsWebSocketService;

    EncryptionUtil encryptionUtil = new EncryptionUtil();

    @Test
    void contextLoads() throws InterruptedException, IOException {


        /**
         * 用户id
         */
        String userNm = "";
        /**
         * 用户类型
         */
        String userType = "";
        /**
         * 用户车牌号类型
         */
        String cphlx = "";

        PostPointmentDTO postPointmentDTO = new PostPointmentDTO();

        UserSmsWebSocket u = userSmsWebSocketService.ByUserPhoneSelect("13170151816");




        RequestHeaderUtil requestHeaderUtil = new RequestHeaderUtil(u);

        /**
         * 获取用户信息
         */
        JSONObject s = function.checkCookieAndGetResponse(u,requestHeaderUtil);
        if(s.getBoolean("success")){
            JSONObject userData = s.getJSONObject("data");
            JSONObject userObject = userData.getJSONObject("user");
            if (userObject != null) {
                userNm = userObject.getString("userNm");
                userType = userObject.getString("userType");
                postPointmentDTO.setTjr(userNm);
                postPointmentDTO.setUserType(userType);
                postPointmentDTO.setCs(String.valueOf(u.getNumberOfVehiclesAndShips()));
                postPointmentDTO.setCyr(u.getUserName());
                postPointmentDTO.setJsr(u.getUserName());
                postPointmentDTO.setDxyzm("");

                postPointmentDTO.setSfz(u.getUserIdCard());
                String mobileDeviceIdStr = "";
                String mobileDeviceId = "";
                if(!StrUtil.hasBlank(requestHeaderUtil.getMobileDeviceId())) {
                    mobileDeviceIdStr = requestHeaderUtil.getMobileDeviceId(); // "mobileDeviceId=os7mus9HY8oa5IQjlAevxA5YdUVM"

                    postPointmentDTO.setMobileDeviceId(mobileDeviceIdStr);
                    postPointmentDTO.setOpenId(mobileDeviceIdStr);
                }
            }
        }

        /**
         * 获取车牌号
         */
        JSONObject d = function.getLicensePlateId(requestHeaderUtil,u);
        JSONArray dataArray = d.getJSONArray("data");
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject vehicleInfo = dataArray.getJSONObject(i);
            String cph = vehicleInfo.getString("cph");
            if(u.getVehicleLicensePlateNumber().equals(cph)){
                cphlx = vehicleInfo.getString("cclx");
                /**
                 * no
                 */
                postPointmentDTO.setCllxNm(cphlx);
                postPointmentDTO.setCphStr(u.getVehicleLicensePlateNumber()+",");
                log.info("找到车牌号数据：{}，汽车类型：{}", vehicleInfo,vehicleInfo.getString("cclx"));
            }
        }

        /**
         * 搜索粮库信息
         */
        JSONObject rejson=function.search(u, requestHeaderUtil);
        JSONArray a = rejson.getJSONArray("data");
        JSONObject data = a.getJSONObject(0);
        String zzmc = data.getString("zzmc");
        String zznm = data.getString("zznm");
        String latitude = data.getString("latitude");
        String longitude = data.getString("longitude");
        String lxfs = data.getString("lxfs");
        String rq = data.getString("rq");
        String pzmxnm_from_yypznm = data.getString("yypznm");
        String kssj = "";
        String jssj = "";
        String pznm_from_yypzmxnm = "";
        JSONArray yypzmxList = data.getJSONArray("yypzmxList");
        JSONObject firstTimeSlot = yypzmxList.getJSONObject(0);
        kssj = firstTimeSlot.getString("kssj");
        jssj = firstTimeSlot.getString("jssj");
        pznm_from_yypzmxnm = firstTimeSlot.getString("yypzmxnm");
        postPointmentDTO.setRq(rq);
        postPointmentDTO.setZzmc(zzmc);
        postPointmentDTO.setZznm(zznm);
        postPointmentDTO.setLatitude(latitude);
        postPointmentDTO.setLongitude(longitude);
        postPointmentDTO.setLxfs(lxfs);
        postPointmentDTO.setRq(rq);
        postPointmentDTO.setPznm(pzmxnm_from_yypznm);
        postPointmentDTO.setKssj(kssj);
        postPointmentDTO.setJssj(jssj);
        postPointmentDTO.setPzmxnm(pznm_from_yypzmxnm);
        String lspzString = data.getString("lspz");
        JSONArray lspzArray = JSON.parseArray(lspzString);
        String lsmc = "";
        String lsnm = "";

        if (lspzArray != null && !lspzArray.isEmpty()) {
            for (int i = 0; i < lspzArray.size(); i++) {
                JSONObject firstGrain = lspzArray.getJSONObject(i);
                if(u.getGrainVarieties().equals(firstGrain.getString("name"))){
                    lsmc = firstGrain.getString("name");
                    lsnm = firstGrain.getString("nm");
                    postPointmentDTO.setLsmc(lsmc);
                    postPointmentDTO.setLsnm(lsnm);
                }
            }
        }
        log.info("预约配置列表: {}",firstTimeSlot);
        log.info("返回数据:{}", rejson.toString());



        /**
         * getGrxxStatus
         */
        JSONObject  tr = function.getGrxxStatus(postPointmentDTO,requestHeaderUtil,u);
        log.info("<getGrxxStatus>{}",tr);
        /**
         *hdmCheck
         */
        JSONObject ew = function.hdmCheck(requestHeaderUtil,u);
        log.info("<hdmCheck>{}",ew);


        /**
         * getDistanceByCurrentLocation
         */
        JSONObject qs = function.getDistanceByCurrentLocation(postPointmentDTO.getZznm(),postPointmentDTO.getLongitude(),postPointmentDTO.getLatitude(),postPointmentDTO, requestHeaderUtil, u);
        log.info("<getDistanceByCurrentLocation>{}", qs);




        /**
         * getSmsBooles
         */
        JSONObject lw = function.getSmsBooles(postPointmentDTO,requestHeaderUtil);
        log.info("<getSmsBooles>{}",lw);
        /**
         * getResvSjList
         */
        JSONObject SjList = function.getResvSjList(requestHeaderUtil,data);
        log.info("<getSmsBooles>{}",SjList);


        /**
         * 获取随机数
         */
        function.getRandomcode(postPointmentDTO, requestHeaderUtil,u);



        /**
         * 获取图片验证码
         * 发送手机验证码
         */
        JSONObject checkRe= function.checkData(postPointmentDTO,requestHeaderUtil,u);
        if (!checkRe.getBoolean("needsms")){
            postPointmentDTO.setDxyzm("");
        }
        UserSmsWebSocket ua = userSmsWebSocketService.ByUserPhoneSelect(postPointmentDTO.getPhone());
        if (ua != null) {
            // 获取用户上传验证码的时间戳
            LocalDateTime upSmsTime = ua.getUpSmsTime();
            // 获取服务器当前时间
            LocalDateTime now = LocalDateTime.now();
            // 计算从上传验证码到当前时间的时间差
            Duration duration = Duration.between(upSmsTime, now);
            // 获取时间差的总秒数
            long secondsDifference = duration.getSeconds();
            // 判断时间差是否超过30秒
            if (secondsDifference > 30) {
                log.info("<验证码已超过30秒有效期，需要用户重新获取。>");
                postPointmentDTO.setDxyzm("");

            } else {
                postPointmentDTO.setDxyzm(ua.getUserSmsMessage());
            }
        }


        /**
         * 检查车牌号
         */
        JSONObject se = function.checkCphYycs(postPointmentDTO,requestHeaderUtil);
        log.info("<检查车牌号>{}",se);





        postPointmentDTO.setSl(u.getFoodOfGrainNum());
        postPointmentDTO.setPhone(u.getUserPhone());
        postPointmentDTO.setYyr(u.getUserName());
        postPointmentDTO.setYwlx("0");
        postPointmentDTO.setCyrnm("");
        postPointmentDTO.setTzdbh("");
        postPointmentDTO.setCyr("");
        postPointmentDTO.setCyrsfzh("");
        postPointmentDTO.setQymc("");
        postPointmentDTO.setXydm("");
        postPointmentDTO.setWxdlFlag("true");
        postPointmentDTO.setYyfsnm("1");
        postPointmentDTO.setYyfsmc("预约挂号");
        postPointmentDTO.setDevicetype("weixin");
        postPointmentDTO.setZldd("");
        postPointmentDTO.setCyrsjh("");




        String e = encryptionUtil.rsa(postPointmentDTO);
        postPointmentDTO.setSecretData(e);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // 将Java对象转换为JSON字符串
            String jsonString = objectMapper.writeValueAsString(postPointmentDTO);
            log.info("输出：{}", jsonString);

        } catch (JsonProcessingException jsonProcessingException) {
            log.error("DTO转JSON失败", jsonProcessingException);
        }
        log.info("提交：{}",function.postInfo(requestHeaderUtil,postPointmentDTO));
    }
}
