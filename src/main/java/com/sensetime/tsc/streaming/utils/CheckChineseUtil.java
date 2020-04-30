package com.sensetime.tsc.streaming.utils;

import org.springframework.util.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/30 17:55
 */
public class CheckChineseUtil {

    private final static Pattern PATTERN = Pattern.compile("[\u4E00-\u9FA5|\\！|\\，|\\。|\\（|\\）|\\《|\\》|\\“|\\”|\\？|\\：|\\；|\\【|\\】]");

    /**
     * 字符串是否包含中文
     *
     * @param str 待校验字符串
     * @return true 包含中文字符 false 不包含中文字符
     */
    public static boolean isContainChinese(String str) {
        Assert.notNull(str, "str can not be empty ");
        Matcher m = PATTERN.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }
}
