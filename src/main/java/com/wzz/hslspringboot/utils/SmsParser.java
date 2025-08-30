package com.wzz.hslspringboot.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    private static final Logger log = LogManager.getLogger(SmsParser.class);
    public static String parseVerificationCode(String smsContent) {
        if (smsContent == null || smsContent.trim().isEmpty()) {
            return null;
        }
        String trimmedContent = smsContent.trim();
        Pattern directCodePattern = Pattern.compile("^\\d{6}$");
        if (directCodePattern.matcher(trimmedContent).matches()) {
            return trimmedContent;
        }
        final String expectedSignature = "【中储粮惠三农】";
        if (!trimmedContent.startsWith(expectedSignature)) {
            log.info("短信内容既不是纯6位数字，签名也不匹配，期望签名: " + expectedSignature);
            return null;
        }
        Pattern embeddedCodePattern = Pattern.compile("(\\d{6})");
        Matcher matcher = embeddedCodePattern.matcher(trimmedContent);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
           log.info("在带签名的短信内容中未找到6位数字验证码。");
            return null;
        }
    }

    public static void main(String[] args) {
        String validSms = "【中储粮惠三农】您的验证码是171220，仅用于本次操作。如收到陌生验证码，请及时检查。";
        System.out.println("测试1 (正常短信): " + validSms);
        String code1 = parseVerificationCode(validSms);
        System.out.println("解析结果: " + code1); // 期望输出: 171220
        System.out.println("--------------------");
        String invalidSenderSms = "【其他平台】您的验证码是987654，请勿泄露。";
        System.out.println("测试2 (签名不匹配): " + invalidSenderSms);
        String code2 = parseVerificationCode(invalidSenderSms);
        System.out.println("解析结果: " + code2); // 期望输出: null
        System.out.println("--------------------");
        String noCodeSms = "【中储粮惠三农】您好，您的业务已办理成功。";
        System.out.println("测试3 (无验证码): " + noCodeSms);
        String code3 = parseVerificationCode(noCodeSms);
        System.out.println("解析结果: " + code3); // 期望输出: null
        System.out.println("--------------------");
        String wrongLengthCodeSms = "【中储粮惠三农】您的验证码是12345，请注意查收。";
        System.out.println("测试4 (验证码位数错误): " + wrongLengthCodeSms);
        String code4 = parseVerificationCode(wrongLengthCodeSms);
        System.out.println("解析结果: " + code4); // 期望输出: null
        System.out.println("--------------------");
        System.out.println("123456");
        String code5 = parseVerificationCode("123456");
        System.out.println("解析结果: " + code5);
        System.out.println("--------------------");
    }
}
