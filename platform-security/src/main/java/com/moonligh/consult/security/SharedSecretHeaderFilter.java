package com.moonligh.consult.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonligh.consult.framework.ErrorCode;
import com.moonligh.consult.framework.Result;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 共享密钥请求头校验（X-Pay-Token / X-Benefit-Token / X-Internal-Token 等）。常量时间比较。
 */
public class SharedSecretHeaderFilter extends OncePerRequestFilter {

    public enum FailBody {
        /** HTTP 200 + Result JSON（与业务接口一致）。 */
        RESULT,
        /** HTTP 401 + 简单 UNAUTHORIZED JSON。 */
        SIMPLE_UNAUTHORIZED
    }

    private final String headerName;
    private final byte[] expected;
    private final String pathPrefix;
    private final FailBody failBody;
    private final ObjectMapper mapper = new ObjectMapper();

    public SharedSecretHeaderFilter(String headerName, String expectedToken, String pathPrefix, FailBody failBody) {
        this.headerName = headerName;
        this.expected = expectedToken == null ? new byte[0] : expectedToken.trim().getBytes(StandardCharsets.UTF_8);
        this.pathPrefix = pathPrefix;
        this.failBody = failBody == null ? FailBody.RESULT : failBody;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (pathPrefix == null || pathPrefix.isBlank()) {
            return false;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith(pathPrefix);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String given = request.getHeader(headerName);
        byte[] actual = given == null ? new byte[0] : given.getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || actual.length != expected.length || !MessageDigest.isEqual(expected, actual)) {
            writeFail(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private void writeFail(HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (failBody == FailBody.SIMPLE_UNAUTHORIZED) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\"}");
            return;
        }
        response.setStatus(200);
        mapper.writeValue(response.getWriter(), Result.fail(ErrorCode.LOGIN_REQUIRED));
    }
}
