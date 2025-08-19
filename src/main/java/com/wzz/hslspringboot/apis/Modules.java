package com.wzz.hslspringboot.apis;

import com.alibaba.fastjson.JSONObject;
import com.wzz.hslspringboot.DTO.Result;
import com.wzz.hslspringboot.pojo.UserSmsWebSocket;
import com.wzz.hslspringboot.utils.HttpRequestUtil;
import com.wzz.hslspringboot.utils.RequestHeaderUtil;

import java.util.HashMap;
import java.util.Map;

public class Modules {
    public JSONObject getResvKdList(UserSmsWebSocket userSmsWebSocket, RequestHeaderUtil requestHeaderUtil) {
        HttpRequestUtil util = new HttpRequestUtil();
        Map n = new HashMap();
        n.put("userId",userSmsWebSocket.getUserPhone());
        n.put("devicetype","weixin");
        return util.postForm("/slyyServlet/service/nhyy/getClxxByUserId",requestHeaderUtil,n);
    }
}
