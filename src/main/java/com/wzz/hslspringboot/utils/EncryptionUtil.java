package com.wzz.hslspringboot.utils;


import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.DigestUtil;
import com.wzz.hslspringboot.DTO.PostPointmentDTO;

import java.security.PublicKey;
import java.util.Base64;

public class EncryptionUtil {
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
        String str=post.getPhone()+"i"+post.getPznm()+"n"+post.getPzmxnm()+"s"+post.getSfz()+"p"+post.getRq()+"u"+post.getCphStr()+"r"
                +post.getZznm()+post.getUuid()+post.getDxyzm()+post.getYyfsmc()+post.getYyfsmc();
        RSA rsa = new RSA(null,publicKey);
        byte[] encrypt = rsa.encrypt(StrUtil.bytes(str, CharsetUtil.CHARSET_UTF_8), KeyType.PublicKey);
        return Base64.getEncoder().encodeToString(encrypt);
    }
}
