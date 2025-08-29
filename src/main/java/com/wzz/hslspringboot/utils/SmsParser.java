package com.wzz.hslspringboot.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    private static final Logger log = LogManager.getLogger(SmsParser.class);

    /**
     * 从指定短信内容中解析出验证码。
     * <p>
     * 1. 纯6位数字验证码字符串，例如："171220"
     * 2. 由"中储粮惠三农"发出的标准短信，例如："【中储粮惠三农】您的验证码是171220，仅用于本次操作。..."
     * @param smsContent 短信的完整内容或纯验证码
     * @return 如果成功解析出验证码，则返回6位数字验证码字符串；否则返回 null。
     */
    public static String parseVerificationCode(String smsContent) {
        // 1. 基本校验：检查输入是否为空或仅有空白字符
        if (smsContent == null || smsContent.trim().isEmpty()) {
            log.info("输入短信内容为空。");
            return null;
        }
        // 对内容进行trim，方便后续处理
        String trimmedContent = smsContent.trim();
        // 2. 优先匹配纯6位数字格式
        //    使用正则表达式 "^\\d{6}$" 来确保整个字符串不多不少，就是6个数字
        Pattern directCodePattern = Pattern.compile("^\\d{6}$");
        if (directCodePattern.matcher(trimmedContent).matches()) {
            return trimmedContent;
        }
        // 3. 如果不是纯数字，则尝试解析标准短信格式
        // 3.1 签名匹配：检查是否是指定发送方
        final String expectedSignature = "【中储粮惠三农】";
        if (!trimmedContent.startsWith(expectedSignature)) {
            log.info("短信内容既不是纯6位数字，签名也不匹配，期望签名: " + expectedSignature);
            return null;
        }
        // 3.2 验证码匹配：使用正则表达式提取短信内容中的第一个6位数字
        //    正则表达式 "\\d{6}" 会匹配任意连续的6个数字。
        //    这比原有的 "验证码是(\\d{6})" 更具健壮性，可以兼容 "验证码为:123456" 等不同说法。
        Pattern embeddedCodePattern = Pattern.compile("(\\d{6})");
        Matcher matcher = embeddedCodePattern.matcher(trimmedContent);

        // 3.3 提取并返回结果
        if (matcher.find()) {
            // matcher.group(1) 会返回第一个捕获组的内容，即第一个匹配到的6位数字 "171220"
            return matcher.group(1);
        } else {
            log.info("在带签名的短信内容中未找到6位数字验证码。");
            return null;
        }
    }

    /**
     * 用于测试的主方法
     */
    public static void main(String[] args) {
        // --- 测试用例 ---

        // 1. 正常情况
        String validSms = "【中储粮惠三农】您的验证码是171220，仅用于本次操作。如收到陌生验证码，请及时检查。";
        System.out.println("测试1 (正常短信): " + validSms);
        String code1 = parseVerificationCode(validSms);
        System.out.println("解析结果: " + code1); // 期望输出: 171220
        System.out.println("--------------------");

        // 2. 签名不匹配
        String invalidSenderSms = "【其他平台】您的验证码是987654，请勿泄露。";
        System.out.println("测试2 (签名不匹配): " + invalidSenderSms);
        String code2 = parseVerificationCode(invalidSenderSms);
        System.out.println("解析结果: " + code2); // 期望输出: null
        System.out.println("--------------------");

        // 3. 签名正确，但内容不含验证码
        String noCodeSms = "【中储粮惠三农】您好，您的业务已办理成功。";
        System.out.println("测试3 (无验证码): " + noCodeSms);
        String code3 = parseVerificationCode(noCodeSms);
        System.out.println("解析结果: " + code3); // 期望输出: null
        System.out.println("--------------------");

        // 4. 验证码位数不正确
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
