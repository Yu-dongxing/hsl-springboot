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
    private final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0uFFjWtTp+La/vi/MqlnuoMpHYR8QNVSuaV0WA0eYei+FnWt9e+pCYfocL8/tMQA4vWxCM9ZcffsgknC1H7hbKEnIAGrG+FOCTzzlWUbm9N7XdVUuyD5hrjr79rN64lwKR5SY0msogBdCf5Nlt4QX1A5klVnDU7NfCyBNmIo6G2tWWCsEEL7mp4PyEjD0LXmx8uBVboexkmIBV/eFTNdIduCKsCp43SCpyu8yfZ6aaSLlHP5Pj3cyC5IzTqBBeiSu/JyXoE9X4D6rxnzc+Ge/stpzXV9Qe9ZC85TsxfmDkERB61rYbrOq7dnw8aAGtkwaGvdqjAPLGK1dGileBOy1QIDAQAB";

    public String password(String password) {
        String str = "{1#2$3%4(5)6@7!poeeww$3%4(5)djjkkldss}";
        return DigestUtil.md5Hex(password + str);
    }


    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

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
}