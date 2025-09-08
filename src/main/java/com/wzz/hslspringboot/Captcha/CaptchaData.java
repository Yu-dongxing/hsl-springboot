package com.wzz.hslspringboot.Captcha;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CaptchaData {
    private static final Logger log = LogManager.getLogger(CaptchaData.class);
    private String backgroundImageTag;
    private String templateImageTag;
    private String backgroundImage;
    private String templateImage;
    private String type;
    private String id;
    private String extra;
    private String msg;
    private int templateImageWidth;
    private int templateImageHeight;
    private int backgroundImageHeight;
    private int backgroundImageWidth;
    private int status;
    private Object data;
    private JSONArray trackList;
    private Object result;
    private String startTime;
    private String stopTime;


    public CaptchaData(JSONObject jsonObject) {
        JSONObject captcha = jsonObject.getJSONObject("captcha");
        if (captcha != null) {
            this.backgroundImageTag = captcha.getString("backgroundImageTag");
            this.templateImageTag = captcha.getString("templateImageTag");
            this.backgroundImage = captcha.getString("backgroundImage");
            this.templateImage = captcha.getString("templateImage");
            this.type = captcha.getString("type");
            this.templateImageWidth = captcha.getIntValue("templateImageWidth");
            this.templateImageHeight = captcha.getIntValue("templateImageHeight");
            this.backgroundImageHeight = captcha.getIntValue("backgroundImageHeight");
            this.backgroundImageWidth = captcha.getIntValue("backgroundImageWidth");
        } else {
            this.templateImageWidth = 0;
            this.templateImageHeight = 0;
            this.backgroundImageHeight = 0;
            this.backgroundImageWidth = 0;
            log.warn("警告: 传入的JSON中缺少 'captcha' 对象。");
        }
        this.id = jsonObject.getString("id");
        this.data = jsonObject.get("data");
    }
    public JSONObject getJson(){
        JSONObject jsonObject = new JSONObject();
        JSONObject dataJson = new JSONObject();
        jsonObject.put("bgImageWidth", 300);
        if (type.equals("WORD_IMAGE_CLICK")){
            jsonObject.put("templateImageWidth", 132);
            jsonObject.put("templateImageHeight", 35);
        }else {
            jsonObject.put("templateImageWidth", 55);
            jsonObject.put("templateImageHeight", 180);
        }

        jsonObject.put("bgImageHeight", 180);
        jsonObject.put("trackList", trackList);
        jsonObject.put("startTime", startTime);
        jsonObject.put("stopTime", stopTime);
        dataJson.put("data", jsonObject);
        dataJson.put("id", id);
        return dataJson;
    }

    public String getBackgroundImageTag() {
        return backgroundImageTag;
    }

    public void setBackgroundImageTag(String backgroundImageTag) {
        this.backgroundImageTag = backgroundImageTag;
    }

    public String getTemplateImageTag() {
        return templateImageTag;
    }

    public void setTemplateImageTag(String templateImageTag) {
        this.templateImageTag = templateImageTag;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getTemplateImage() {
        return templateImage;
    }

    public void setTemplateImage(String templateImage) {
        this.templateImage = templateImage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTemplateImageWidth() {
        return templateImageWidth;
    }

    public void setTemplateImageWidth(int templateImageWidth) {
        this.templateImageWidth = templateImageWidth;
    }

    public int getTemplateImageHeight() {
        return templateImageHeight;
    }

    public void setTemplateImageHeight(int templateImageHeight) {
        this.templateImageHeight = templateImageHeight;
    }

    public int getBackgroundImageHeight() {
        return backgroundImageHeight;
    }

    public void setBackgroundImageHeight(int backgroundImageHeight) {
        this.backgroundImageHeight = backgroundImageHeight;
    }

    public int getBackgroundImageWidth() {
        return backgroundImageWidth;
    }

    public void setBackgroundImageWidth(int backgroundImageWidth) {
        this.backgroundImageWidth = backgroundImageWidth;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public JSONArray getTrackList() {
        return trackList;
    }

    public void setTrackList(JSONArray trackList) {
        this.trackList = trackList;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getStopTime() {
        return stopTime;
    }

    public void setStopTime(String stopTime) {
        this.stopTime = stopTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "CaptchaData{" +
                "backgroundImageTag='" + backgroundImageTag + '\'' +
                ", templateImageTag='" + templateImageTag + '\'' +
                ", backgroundImage='" + backgroundImage + '\'' +
                ", templateImage='" + templateImage + '\'' +
                ", type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", extra='" + extra + '\'' +
                ", msg='" + msg + '\'' +
                ", templateImageWidth=" + templateImageWidth +
                ", templateImageHeight=" + templateImageHeight +
                ", backgroundImageHeight=" + backgroundImageHeight +
                ", backgroundImageWidth=" + backgroundImageWidth +
                ", status=" + status +
                ", data=" + data +
                ", trackList=" + trackList +
                ", result=" + result +
                ", startTime='" + startTime + '\'' +
                ", stopTime='" + stopTime + '\'' +
                '}';
    }
}
