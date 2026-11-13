package com.moonligh.consult.framework;

import java.util.regex.Pattern;

/**
 * 中国大陆手机号校验。
 */
public final class Mobiles {

    private static final Pattern CN = Pattern.compile("^1[3-9]\\d{9}$");

    private Mobiles() {
    }

    public static String require(String mobile) {
        String value = mobile == null ? "" : mobile.trim();
        if (!CN.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.INVALID_MOBILE);
        }
        return value;
    }
}
