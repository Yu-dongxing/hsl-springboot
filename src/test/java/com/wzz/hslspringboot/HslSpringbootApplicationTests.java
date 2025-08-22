package com.wzz.hslspringboot;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    void contextLoads() throws InterruptedException {


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
                    String[] parts = mobileDeviceIdStr.split("=");
                    if (parts.length > 1) {
                        mobileDeviceId = parts[1]; // 获取"="后面的部分
                    } else {
                        mobileDeviceId = "";
                    }

                    postPointmentDTO.setMobileDeviceId(mobileDeviceId);
                    postPointmentDTO.setOpenId(mobileDeviceId);
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

                postPointmentDTO.setCphStr(u.getVehicleLicensePlateNumber());
                log.info("找到车牌号数据：{}，汽车类型：{}", vehicleInfo,vehicleInfo.getString("cclx"));
            }
        }
        //构建提交数据
//        PostPointmentDTO po = new PostPointmentDTO(userNm,);

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

        EncryptionUtil encryptionUtil = new EncryptionUtil();


        log.info("预约配置列表: {}",firstTimeSlot);
        System.out.println("返回数据"+rejson.toString());

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

        /**
         * 获取随机数
         */
        JSONObject o =function.getRandomcode(u,requestHeaderUtil);
        log.info("获取随机数 :{}",o);
        JSONObject dataJson = o.getJSONObject("data");
        String randomCode = dataJson.getString("randomCode");
        log.info("成功解析到 randomCode: {}", randomCode);
        postPointmentDTO.setLxfs(u.getUserPhone()+"_"+randomCode);


        /**
         * 获取是否需要短信验证码
         * 获取图片验证码
         * 发送手机验证码
         */
        JSONObject checkRe= function.checkData(postPointmentDTO,requestHeaderUtil,u);

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

    @Test
    void tt(){
        UserSmsWebSocket u = userSmsWebSocketService.ByUserPhoneSelect("13170151816");
        appointmentProcessorService.processAppointment(u);
    }

    /**
     * 封装（）
     */


}
