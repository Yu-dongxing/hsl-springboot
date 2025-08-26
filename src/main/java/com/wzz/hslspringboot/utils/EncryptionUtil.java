package com.wzz.hslspringboot.utils;


import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.PublicKey;
import java.util.Base64;

public class EncryptionUtil {
    private static final Logger log = LogManager.getLogger(EncryptionUtil.class);
    private String publicKey="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0uFFjWtTp+La/vi/MqlnuoMpHYR8QNVSuaV0WA0eYei+FnWt9e+pCYfocL8/tMQA4vWxCM9ZcffsgknC1H7hbKEnIAGrG+FOCTzzlWUbm9N7XdVUuyD5hrjr79rN64lwKR5SY0msogBdCf5Nlt4QX1A5klVnDU7NfCyBNmIo6G2tWWCsEEL7mp4PyEjD0LXmx8uBVboexkmIBV/eFTNdIduCKsCp43SCpyu8yfZ6aaSLlHP5Pj3cyC5IzTqBBeiSu/JyXoE9X4D6rxnzc+Ge/stpzXV9Qe9ZC85TsxfmDkERB61rYbrOq7dnw8aAGtkwaGvdqjAPLGK1dGileBOy1QIDAQAB";
    /**
     * 密码+"{1#2$3%4(5)6@7!poeeww$3%4(5)djjkkldss}"
     */
    public String password(String password) {
        String str="{1#2$3%4(5)6@7!poeeww$3%4(5)djjkkldss}";
        String md5HexPassword = DigestUtil.md5Hex(password+str);
        return md5HexPassword;
    }
    /*
    *
    *
    * 加密密文拼接方式:var str = params.phone+"i"+params.pznm+"n"+params.pzmxnm+"s"+params.sfz+"p"+params.rq+"u"+params.cphStr+"r"+params.zznm
                     + params.uuid + params.dxyzm + params.yyfsnm + params.yyfsmc;
    * */
    public String rsa(PostPointmentDTO post){
        log.info("加密的数据：{}",post);
        String str=post.getPhone()+"i"+post.getPznm()+"n"+post.getPzmxnm()+"s"+post.getSfz()+"p"+post.getRq()+"u"+post.getCphStr()+"r"
                +post.getZznm()+post.getUuid()+post.getDxyzm()+post.getYyfsnm()+post.getYyfsmc();
        log.info("<加密的数据,拼接后：>{}",str);
        RSA rsa = new RSA(null,publicKey);
        byte[] encrypt = rsa.encrypt(StrUtil.bytes(str, CharsetUtil.CHARSET_UTF_8), KeyType.PublicKey);
        log.info("<加密后的数据：：>{}",Base64.getEncoder().encodeToString(encrypt));
        return Base64.getEncoder().encodeToString(encrypt);
    }

//    public static void main(String[] args) {
//        String publicKey="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0uFFjWtTp+La/vi/MqlnuoMpHYR8QNVSuaV0WA0eYei+FnWt9e+pCYfocL8/tMQA4vWxCM9ZcffsgknC1H7hbKEnIAGrG+FOCTzzlWUbm9N7XdVUuyD5hrjr79rN64lwKR5SY0msogBdCf5Nlt4QX1A5klVnDU7NfCyBNmIo6G2tWWCsEEL7mp4PyEjD0LXmx8uBVboexkmIBV/eFTNdIduCKsCp43SCpyu8yfZ6aaSLlHP5Pj3cyC5IzTqBBeiSu/JyXoE9X4D6rxnzc+Ge/stpzXV9Qe9ZC85TsxfmDkERB61rYbrOq7dnw8aAGtkwaGvdqjAPLGK1dGileBOy1QIDAQAB";
//        String str="13170151816i3af35309a782460ea167efc8e9693574n964b6c405419495eafc34264fd55f039s522427200208012257p20250825u晋CC0753,rzh8080816c611ae0016c6138cd030005O00000000000000058682d497769fad44eb690a7ef1f930b9ec01预约挂号";
//        RSA rsa = new RSA(null,publicKey);
//        byte[] encrypt = rsa.encrypt(StrUtil.bytes(str, CharsetUtil.CHARSET_UTF_8), KeyType.PublicKey);
//        String m=Base64.getEncoder().encodeToString(encrypt);
//        System.out.println(m);
//    }
    /**
     * 请求加密数据
     */
    public String postRsa(String t){

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("text",t);


        HttpResponse response = HttpRequest.post("http://127.0.0.1:3000/encrypt")
                // 设置请求头
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .header("Host", "127.0.0.1:3000")
                .header("Connection", "keep-alive")
                // 设置请求体
                .body(jsonObject.toString())
                // 执行请求
                .execute();
        // 判断请求是否成功
        if (response.isOk()) {
            System.out.println("请求成功，响应内容：");
            JSONObject re = JSONObject.parseObject(response.body());
            return re.getString("result");
        } else {
            System.err.println("请求失败，状态码：" + response.getStatus());
            System.err.println("响应内容：" + response.body());
        }
        return null;
    }


}
