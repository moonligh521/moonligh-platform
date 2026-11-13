package com.moonligh.consult.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MobilesTest {

    @Test
    void acceptsMainlandMobile() {
        assertEquals("13800138000", Mobiles.require(" 13800138000 "));
    }

    @Test
    void rejectsInvalid() {
        BusinessException error = assertThrows(BusinessException.class, () -> Mobiles.require("123"));
        assertEquals(ErrorCode.INVALID_MOBILE, error.getErrorCode());
    }
}
