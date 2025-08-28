package com.wzz.hslspringboot.utils;


import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Base64;

public class EncryptionUtil {
    private static final Logger log = LogManager.getLogger(EncryptionUtil.class);
    // 这是一个实例变量，属于对象
    private final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0uFFjWtTp+La/vi/MqlnuoMpHYR8QNVSuaV0WA0eYei+FnWt9e+pCYfocL8/tMQA4vWxCM9ZcffsgknC1H7hbKEnIAGrG+FOCTzzlWUbm9N7XdVUuyD5hrjr79rN64lwKR5SY0msogBdCf5Nlt4QX1A5klVnDU7NfCyBNmIo6G2tWWCsEEL7mp4PyEjD0LXmx8uBVboexkmIBV/eFTNdIduCKsCp43SCpyu8yfZ6aaSLlHP5Pj3cyC5IzTqBBeiSu/JyXoE9X4D6rxnzc+Ge/stpzXV9Qe9ZC85TsxfmDkERB61rYbrOq7dnw8aAGtkwaGvdqjAPLGK1dGileBOy1QIDAQAB";

    /**
     * 密码+"{1#2$3%4(5)6@7!poeeww$3%4(5)djjkkldss}"
     */
    public String password(String password) {
        String str = "{1#2$3%4(5)6@7!poeeww$3%4(5)djjkkldss}";
        return DigestUtil.md5Hex(password + str);
    }
    /*
     *
     *
     * 加密密文拼接方式:var str = params.phone+"i"+params.pznm+"n"+params.pzmxnm+"s"+params.sfz+"p"+params.rq+"u"+params.cphStr+"r"+params.zznm
     * + params.uuid + params.dxyzm + params.yyfsnm + params.yyfsmc;
     *
     *
     *
     */


    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 将JSON字符串转换为PostPointmentDTO对象
     *
     * @param jsonString 待转换的JSON字符串
     * @return 转换后的PostPointmentDTO对象，如果转换失败则返回null
     */
    public static PostPointmentDTO convertJsonToDto(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonString, PostPointmentDTO.class);
        } catch (JsonProcessingException e) {
            System.err.println("JSON解析失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 这是实例方法，因为它使用了实例变量 publicKey
     */
    public String rsa(PostPointmentDTO post) {
        log.info("准备加密的数据DTO：{}", post);
        String str = post.getPhone() + "i" + post.getPznm() + "n" + post.getPzmxnm() + "s" + post.getSfz() + "p" + post.getRq() + "u" + post.getCphStr() + "r"
                + post.getZznm() + post.getUuid() + post.getDxyzm() + post.getYyfsnm() + post.getYyfsmc();
        log.info("<加密的数据,拼接后：>{}", str);
        RSA rsa = new RSA(null, publicKey);
        byte[] encrypt = rsa.encrypt(StrUtil.bytes(str, CharsetUtil.CHARSET_UTF_8), KeyType.PublicKey);
        String encryptedBase64 = Base64.getEncoder().encodeToString(encrypt);
        log.info("<加密后的数据(Base64)：>{}", encryptedBase64);
        return encryptedBase64;
    }

    public static void main(String[] args) {
        String jsonData = """
                {
                    "yyr": "王应六",
                    "tjr": "9f62b344500642289274991829233f84",
                    "sfz": "522427200208012257",
                    "jsr": "王应六",
                    "phone": "13170151816",
                    "lxfs": "13170151816_j5jUcaR0BFUqzWpjfLmXaosMDgdNOqwbRfsXJgWpDn7wvYL4H6Nh2IPjf+tmt18Yvu3f1vHdtZTqPxmPa+EJOA6icx1lWgehEFMm8/T4KHy3KrEpyT4NZZlGt37uZPrbJ3P/BcIy3YF+nZilYK2ahGl0KtrTVc+TPNh4GUxg/Cj2wkrQ2N0KRXAUHmPSuAyeYFgjyrjcTjkGpBG5rZ11rt3zhzudC4xi6zz/TvB4h2ppemvJQqu7QbOaKSjnL3QxBx9wDLsN6FdmCyMUtIYXsAwKrUjCkHSyI6ECd8RLxHwTU5EPi6Vuy2ABNN6rLF2uzaJGIEDUvXwzVrtQv+oNLQ==",
                    "pznm": "0a93bef9b3504464917750251e610fdf",
                    "pzmxnm": "3a4883914dca401d965ffc5465fb4126",
                    "zznm": "zh8080816c611ae0016c6138cd030005O0000000000000005868",
                    "zzmc": "中央储备粮芜湖直属库有限公司",
                    "rq": "20250827",
                    "lsnm": "200001",
                    "lsmc": "小麦",
                    "zldd": "",
                    "kssj": "0500",
                    "jssj": "2300",
                    "cs": "1",
                    "sl": "20",
                    "cphStr": "晋CC0753,",
                    "cllxNm": "1",
                    "cyrnm": "",
                    "tzdbh": "",
                    "ywlx": 0,
                    "cyr": "",
                    "cyrsfzh": "",
                    "cyrsjh": "",
                    "qymc": "",
                    "xydm": "",
                    "wxdlFlag": true,
                    "userType": "01",
                    "dxyzm": "",
                    "mobileDeviceId": "os7mus9HY8oa5IQjlAevxA5YdUVM",
                    "longitude": "118.35924535989761",
                    "latitude": "31.36091992235885",
                    "uuid": "ffba56a03da045df851a7bad11a53576",
                    "yyfsnm": "1",
                    "yyfsmc": "预约挂号",
                    "openId": "os7mus9HY8oa5IQjlAevxA5YdUVM",
                    "secretData": "YNw5iWCgqeKxRsMedY1b5hsUJqSIMBmSn41cJHK6OEDmyPteWhPQLGtXWd4gibznWxzghlBXq6zo0n8ptKyb/tJ5qVxQgvdl95pH3BaLILatiUJ9ZXGyhzVhDsvH3ijibPsRWMPYvkcJUHWa/BsFclHpGt71WbDGuFcHjNiXCkT9TmEVbo5AufaYRLHxkdRe30rvwQHOz2RV0Wkt96mxufAIqM6FrOTjqxJ0Lwj8h3BeawUYI+er73gF0UuDppIUmSZJ8EEF2vq78zCvGbtkVFcFTs0MsLrKakHW4ZHESX2MRegVeiFDaQF3x2KsixJ+0aea62rnOF0hS7jmJpiP5g=="
                }""";
        // 步骤 1: 调用静态方法将JSON转换为DTO
        PostPointmentDTO dto = convertJsonToDto(jsonData);

        // 步骤 2: 增加健壮性检查，防止后续代码出现空指针异常
        if (dto == null) {
            log.error("JSON 转换为 DTO 失败，程序终止。");
            return; // 提前退出
        }

        log.info("JSON 转换 DTO 成功，手机号为: {}", dto.getPhone());

        // 步骤 3: 创建 EncryptionUtil 的实例，因为 rsa() 是一个实例方法
        EncryptionUtil encryptionUtil = new EncryptionUtil();

        // 步骤 4: 使用创建的实例来调用 rsa 方法
        String encryptedData = encryptionUtil.rsa(dto);

        // 步骤 5: 打印最终的加密结果进行验证
        System.out.println("=========================================================");
        System.out.println("原始数据中的手机号: " + dto.getPhone());
        System.out.println("拼接后用于加密的字符串: " + (dto.getPhone() + "i" + dto.getPznm() + "n" + dto.getPzmxnm() + "s" + dto.getSfz() + "p" + dto.getRq() + "u" + dto.getCphStr() + "r" + dto.getZznm() + dto.getUuid() + dto.getDxyzm() + dto.getYyfsnm() + dto.getYyfsmc()));
        System.out.println("RSA 加密后的 Base64 结果: " + encryptedData);
        System.out.println("=========================================================");

        // 你的原始JSON中的secretData字段值，用于比对
        String originalSecretData = "dW29DNP7XP58Qez+3rj/mKHFg4LLdOX4JuZFuL9dazkSevloJXZEGkGTl5Xw2cexmJcPrNNb0nTIItcaIATfC8tCYRyDsnGOaKqGCiECGKZq6I70PfnA49KCCq/3wyz6B9hSqGLECUeceIfMh/BEdDQiqkjeOz8sGEfDBJhHyKhqDnafY4hQ3TXZjO5zYqtTDobWhA8/GeRJG3TqZyYMVgNfK9WP4dDK+WGNcb5sW0Z1iqrbnU+IgX4HD3+wlvDJcQ1IT4MNqMu/JnT/De/NH+ykUqtoGdboC1hbkRQh4d1WNkdSokFt+V25H/53CE4Dfs/IAXSNMFCcWr8Qipfhlg==";
        System.out.println("原始JSON中的secretData值为: " + originalSecretData);
        System.out.println("我们自己加密的结果与原始值是否一致: " + encryptedData.equals(originalSecretData));
    }


    /**
     * 请求加密数据
     */
    public String postRsa(String t) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("text", t);

        HttpResponse response = HttpRequest.post("http://127.0.0.1:3000/encrypt")
                .header("Content-Type", "application/json")
                .body(jsonObject.toString())
                .execute();

        if (response.isOk()) {
            JSONObject re = JSONObject.parseObject(response.body());
            return re.getString("result");
        } else {
            log.error("请求加密服务失败，状态码：{}，响应：{}", response.getStatus(), response.body());
        }
        return null;
    }
}