package com.wzz.hslspringboot.Captcha;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;

public class Captcha {
    private final api api;

    public Captcha(CaptchaData captchaData) throws IOException {
        this.api = new api();
        if (captchaData.getType().contains("SLIDER")) {
            SLIDER(captchaData);
        }else if (captchaData.getType().contains("WORD_IMAGE_CLICK")) {
            WORD_IMAGE_CLICK(captchaData);
        }
    }
    public void SLIDER(CaptchaData captchaData) throws IOException {
        CaptchaUtil Util=new CaptchaUtil();
        api.geePassSdk(captchaData);
        if (captchaData.getStatus()==200){
            Util.SLIDER(captchaData);
            JSONObject jsons=captchaData.getJson();
            System.out.println(jsons);
        }
    }
    public void WORD_IMAGE_CLICK(CaptchaData captchaData) throws IOException {
        CaptchaUtil Util=new CaptchaUtil();
        api.baiDuSdk(captchaData);
        System.out.println(captchaData.getStatus());
        System.out.println(captchaData.getExtra());
        api.YMSdk(captchaData);
        if (captchaData.getStatus()==200){
            System.out.println(captchaData.getStatus());
            Util.IMAGE_CLICK(captchaData);
            System.out.println(captchaData.getTrackList());
        }
    }
}
