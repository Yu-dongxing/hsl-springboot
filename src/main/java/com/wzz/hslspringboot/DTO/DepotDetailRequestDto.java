package com.wzz.hslspringboot.DTO;


import lombok.Data;
import java.util.List;

/**
 * 获取粮库详情接口的请求体 DTO
 */
@Data
public class DepotDetailRequestDto {
    private String dz;
    private String fhsj;
    private int jl;
    private double latitude;
    private double longitude;
    private String lspz;
    private String lxfs;
    private String rq;
    private int xsfhsl;
    private int xssyhsl;
    private String yyfsmc;
    private String yyfsnm;
    private List<Yypzmx> yypzmxList;
    private String yypznm;
    private int zsyhsl;
    private String zt;
    private String zzmc;
    private String zznm;
    private String devicetype = "weixin";

    @Data
    public static class Yypzmx {
        private String fhjssj;
        private String fhkssj;
        private String jssj;
        private String kssj;
        private boolean outTime;
        private int yfhsl;
        private String yypzmxnm;
    }
}