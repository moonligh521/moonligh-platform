package com.moonligh.consult.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonligh.consult.framework.BusinessException;
import com.moonligh.consult.framework.ErrorCode;
import com.moonligh.consult.framework.Result;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 服务内二次校验。网关已验过 JWT；这里防止直连服务时冒充账号。
 * 匿名路径与 staff 前缀由 {@link ConsultAuthProperties} 配置，不硬编码产品路径。
 */
public class AccountAuthFilter extends OncePerRequestFilter {

    private final AuthTokens tokens;
    private final List<String> openPaths;
    private final List<String> openPathPrefixes;
    private final List<String> staffOnlyPrefixes;
    private final ObjectMapper mapper = new ObjectMapper();

    public AccountAuthFilter(AuthTokens tokens, ConsultAuthProperties properties) {
        this.tokens = tokens;
        this.openPaths = List.copyOf(properties.getOpenPaths());
        this.openPathPrefixes = List.copyOf(properties.getOpenPathPrefixes());
        this.staffOnlyPrefixes = List.copyOf(properties.getStaffOnlyPrefixes());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        if (openPaths.contains(path)) {
            return true;
        }
        for (String prefix : openPathPrefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String bearer = request.getHeader(AuthTokens.HEADER);
            if (bearer != null && !bearer.isBlank()) {
                AccountIds.bind(request, tokens.parse(bearer));
            } else {
                AccountIds.of(request);
            }
            String path = request.getRequestURI();
            if (isStaffOnly(path) && !AccountIds.principal(request).staff()) {
                throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
            }
            chain.doFilter(request, response);
        } catch (BusinessException error) {
            writeLoginRequired(response, error);
        }
    }

    private boolean isStaffOnly(String path) {
        for (String prefix : staffOnlyPrefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void writeLoginRequired(HttpServletResponse response, BusinessException error) throws IOException {
        ErrorCode code = error.getErrorCode() == null ? ErrorCode.LOGIN_REQUIRED : error.getErrorCode();
        response.setStatus(200);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(), Result.fail(code));
    }
}
