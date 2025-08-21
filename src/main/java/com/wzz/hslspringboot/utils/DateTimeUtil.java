package com.wzz.hslspringboot.utils;


import lombok.extern.log4j.Log4j2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;

/**
 * 日期时间字符串解析工具（最终优化版）
 */
@Log4j2
public class DateTimeUtil {

    private static final List<String> DATETIME_FORMATS = Arrays.asList(
            // 优先匹配更完整的格式
            "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy.MM.dd HH:mm:ss",
            "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "yyyy.MM.dd HH:mm",
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd",
            "yyyy-MM", "yyyy/MM", "yyyy.MM",
            // 格式中不含年份，将默认使用当前年
            "MM-dd HH:mm:ss", "MM/dd HH:mm:ss", "MM.dd HH:mm:ss",
            "MM-dd", "MM/dd", "MM.dd",
            // 仅时间，将默认使用当前日期
            "HH:mm:ss", "HH:mm"
    );

    /**
     * 尝试将日期字符串转换为 LocalDateTime (最终优化版)
     * @param dateTimeStr 日期字符串
     * @return LocalDateTime，失败返回 null
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            log.warn("输入日期字符串为空");
            return null;
        }

        String trimmedDateTimeStr = dateTimeStr.trim();
        log.info("开始解析日期字符串：" + trimmedDateTimeStr);

        for (String format : DATETIME_FORMATS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                TemporalAccessor temporalAccessor = formatter.parse(trimmedDateTimeStr);

                LocalDate date;
                // 检查解析出的对象是否包含年份信息
                if (temporalAccessor.isSupported(ChronoField.YEAR)) {
                    // Case 1: 包含年份信息
                    if (temporalAccessor.isSupported(ChronoField.DAY_OF_MONTH)) {
                        // 如果包含日，则直接构建 LocalDate
                        date = LocalDate.from(temporalAccessor);
                    } else {
                        // 如果只包含年月 (例如 "2025-07")，则默认为该月的第一天
                        date = YearMonth.from(temporalAccessor).atDay(1);
                    }
                } else {
                    // Case 2: 不包含年份信息 (例如 "07-22" 或 "15:32")
                    if (temporalAccessor.isSupported(ChronoField.MONTH_OF_YEAR)) {
                        // 如果有月和日，则使用当前年份构建日期
                        date = MonthDay.from(temporalAccessor).atYear(Year.now().getValue());
                    } else {
                        // 如果只包含时间，则使用当前日期
                        date = LocalDate.now();
                    }
                }

                LocalTime time;
                // 检查是否包含小时信息
                if (temporalAccessor.isSupported(ChronoField.HOUR_OF_DAY)) {
                    time = LocalTime.from(temporalAccessor);
                } else {
                    // 不包含时间信息，则默认为一天的开始
                    time = LocalTime.MIDNIGHT;
                }

                log.info("成功使用格式 '{}' 解析字符串 '{}'", format, trimmedDateTimeStr);
                return LocalDateTime.of(date, time);

            } catch (DateTimeParseException e) {
                // 当前格式不匹配，忽略异常，继续尝试下一个格式
            }
        }

        log.error("无法使用任何预设格式解析日期字符串: " + trimmedDateTimeStr);
        return null;
    }

    public static void main(String[] args) {
        String[] testStrs = {
                "2025-07-22 15:32:11", // 完整日期时间
                "2025/07/22",          // 只有日期
                "2025-07",             // 只有年月
                "07-22 10:30:00",      // 只有月日和时间
                "07-22",               // 只有月日 (之前出错的case)
                "18:18:16",            // 只有时间
                "15:32",               // 只有时分
                "invalid-date"         // 无效格式
        };

        for (String s : testStrs) {
            System.out.println("------------------------------------");
            LocalDateTime ldt = parseDateTime(s);
            System.out.println("输入: \"" + s + "\" -> " + (ldt != null ? ldt.toString() : "无法解析"));
        }
    }
}