package com.github.zeng.alt.json.internal;

import com.github.zeng.alt.json.annotation.SensitiveType;

/**
 * 两个 Jackson 版本共用的脱敏规则。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class SensitiveValueHelper {
    private SensitiveValueHelper() {
    }

    /** 按注解选择的规则隐藏非空字符串中的敏感内容。 */
    public static String mask(String value, SensitiveType type, String placeholder) {
        if (value.isEmpty()) {
            return value;
        }
        final String p = placeholder;
        return switch (type) {
            case PHONE -> maskPhone(value, p);
            case NAME -> maskName(value, p);
            case EMAIL -> maskEmail(value, p);
            case ID_CARD -> maskIdCard(value, p);
            case BANK_CARD -> maskBankCard(value, p);
            case ADDRESS -> maskAddress(value, p);
            case PASSWORD -> maskPassword(p);
        };
    }

    private static String maskPhone(String value, String p) {
        if (value.length() < 7) {
            return value.charAt(0) + repeat(p, value.length() - 1);
        }
        return value.substring(0, 3) + repeat(p, 4) + value.substring(7);
    }

    private static String maskName(String value, String p) {
        return value.charAt(0) + repeat(p, value.length() - 1);
    }

    private static String maskEmail(String value, String p) {
        final int at = value.indexOf('@');
        if (at <= 0) {
            return value;
        }
        return value.charAt(0) + repeat(p, Math.min(3, at - 1)) + value.substring(at);
    }

    private static String maskIdCard(String value, String p) {
        final int len = value.length();
        if (len < 8) {
            return value;
        }
        return value.substring(0, 3) + repeat(p, len - 7) + value.substring(len - 4);
    }

    private static String maskBankCard(String value, String p) {
        final int len = value.length();
        if (len < 8) {
            return value;
        }
        return value.substring(0, 4) + repeat(p, len - 8) + value.substring(len - 4);
    }

    private static String maskAddress(String value, String p) {
        final int keep = Math.min(6, value.length());
        return value.substring(0, keep) + repeat(p, Math.min(4, value.length() - keep));
    }

    private static String maskPassword(String p) {
        return repeat(p, 6);
    }

    private static String repeat(String s, int count) {
        if (count <= 0) {
            return "";
        }
        return s.repeat(count);
    }
}
