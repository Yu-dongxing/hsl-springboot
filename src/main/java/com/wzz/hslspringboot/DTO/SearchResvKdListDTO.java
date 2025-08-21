package com.wzz.hslspringboot.DTO;

import cn.hutool.core.util.URLUtil;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索粮库
 */
@Data
public class SearchResvKdListDTO {
    private String devicetype;
    private String jlmj;
    private String kqmc;
    private String latitude;
    private String longitude;
    private String max;
    private String min;
    private String mobileDeviceId;
    private String phone;
    private String rq;
    private String selectedVariety;
    private String sfz;
    private String wxdlFlag;
    private String yk;
    private String ywlx;

    /**
     * longitude=117.30741119384766
     * &latitude=31.87824821472168&
     * phone=13170151816
     * &sfz=522427200208012257
     * &rq=20250820
     * &min=0&max=10
     * &ywlx=0
     * &mobileDeviceId=os7mus9HY8oa5IQjlAevxA5YdUVM
     * &yk=
     * &jlmj=
     * &kqmc=%E4%B8%AD%E5%A4%AE&selectedVariety=&wxdlFlag=true&devicetype=weixin
     * @param user
     */

    public SearchResvKdListDTO(UserSmsWebSocket user) {
        RequestHeaderUtil h = new RequestHeaderUtil(user);
        this.jlmj = "200000";
        this.kqmc = user.getTargetGranary();
        this.devicetype = "weixin";
        this.latitude = user.getUserLatitude();
        this.longitude = user.getUserLongitude();
        this.max = "10";
        this.min = "0";
        this.mobileDeviceId = h.getMobileDeviceId();
        this.phone = user.getUserPhone();
        this.rq = user.getFoodReservationDate();
        this.selectedVariety = "";
        this.sfz = user.getUserIdCard();
        this.wxdlFlag = "true";
        this.yk = "";
        this.ywlx = "0";
    }

    public Map<String, Object> get() {
        Map<String, Object> map = new HashMap<>();
        map.put("jlmj", this.jlmj);
        map.put("kqmc", this.kqmc);
        map.put("latitude", this.latitude);
        map.put("longitude", this.longitude);
        map.put("max", this.max);
        map.put("min", this.min);
        map.put("mobileDeviceId", this.mobileDeviceId);
        map.put("phone", this.phone);
        map.put("rq", this.rq);
        map.put("selectedVariety", this.selectedVariety);
        map.put("sfz", this.sfz);
        map.put("wxdlFlag", this.wxdlFlag);
        map.put("yk", this.yk);
        map.put("ywlx", this.ywlx);
        map.put("devicetype", this.devicetype);
        return map;
    }
}
