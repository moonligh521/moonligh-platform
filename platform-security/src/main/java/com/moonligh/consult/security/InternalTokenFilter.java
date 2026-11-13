package com.moonligh.consult.security;

/**
 * /internal/** 只认 X-Internal-Token，不走浏览器 JWT。
 */
public class InternalTokenFilter extends SharedSecretHeaderFilter {

    public InternalTokenFilter(String internalToken) {
        super(AuthTokens.INTERNAL_HEADER, internalToken, null, FailBody.RESULT);
    }
}
