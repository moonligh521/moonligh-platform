package com.moonligh.consult.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 各服务共用的 JWT、内部令牌与 AccountAuth 路径规则。短信、管理员账号仍留在身份服务。
 */
@ConfigurationProperties(prefix = "consult.auth")
public class ConsultAuthProperties {

    private String jwtSecret = "consult-dev-jwt-secret-key-32bytes!";
    private int jwtTtlHours = 168;
    private String internalToken = "consult-internal";
    private List<String> openPaths = new ArrayList<>(List.of(
            "/api/auth/sms",
            "/api/auth/login",
            "/api/admin/login",
            "/actuator/health"));
    private List<String> openPathPrefixes = new ArrayList<>(List.of("/internal/", "/api/pay/callback"));
    private List<String> staffOnlyPrefixes = new ArrayList<>(List.of("/api/admin/", "/api/doctor/"));

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public int getJwtTtlHours() {
        return jwtTtlHours;
    }

    public void setJwtTtlHours(int jwtTtlHours) {
        this.jwtTtlHours = jwtTtlHours;
    }

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    public List<String> getOpenPaths() {
        return openPaths;
    }

    public void setOpenPaths(List<String> openPaths) {
        this.openPaths = openPaths;
    }

    public List<String> getOpenPathPrefixes() {
        return openPathPrefixes;
    }

    public void setOpenPathPrefixes(List<String> openPathPrefixes) {
        this.openPathPrefixes = openPathPrefixes;
    }

    public List<String> getStaffOnlyPrefixes() {
        return staffOnlyPrefixes;
    }

    public void setStaffOnlyPrefixes(List<String> staffOnlyPrefixes) {
        this.staffOnlyPrefixes = staffOnlyPrefixes;
    }
}
