package com.moonligh.consult.framework.gateway;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关横切保护配置（限流、在途、匿名路径）。业务路由 URI 仍由各产品网关自己配置。
 */
@ConfigurationProperties(prefix = "moonligh.gateway")
public class GatewayProtectionProperties {

    private int maxInFlight = 4000;
    private int trustedProxyHops = 0;
    private List<String> openPaths = new ArrayList<>(List.of(
            "/api/auth/sms",
            "/api/auth/login",
            "/api/admin/login",
            "/actuator/health"));
    private List<String> openPathPrefixes = new ArrayList<>(List.of(
            "/api/pay/notify/",
            "/api/pay/callback",
            "/api/payments",
            "/api/benefit"));
    private List<String> staffOnlyPrefixes = new ArrayList<>(List.of("/api/admin/", "/api/doctor/"));
    private final RateLimit rateLimit = new RateLimit();

    public int getMaxInFlight() {
        return maxInFlight;
    }

    public void setMaxInFlight(int maxInFlight) {
        this.maxInFlight = maxInFlight;
    }

    public int getTrustedProxyHops() {
        return trustedProxyHops;
    }

    public void setTrustedProxyHops(int trustedProxyHops) {
        this.trustedProxyHops = trustedProxyHops;
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

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int perSecond = 5;
        private int burst = 20;
        private List<Rule> rules = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPerSecond() {
            return perSecond;
        }

        public void setPerSecond(int perSecond) {
            this.perSecond = perSecond;
        }

        public int getBurst() {
            return burst;
        }

        public void setBurst(int burst) {
            this.burst = burst;
        }

        public List<Rule> getRules() {
            return rules;
        }

        public void setRules(List<Rule> rules) {
            this.rules = rules;
        }
    }

    public static class Rule {
        private String name;
        private List<String> paths = new ArrayList<>();
        private List<String> methods = new ArrayList<>();
        private KeyBy keyBy = KeyBy.ACCOUNT;
        private int perSecond;
        private int burst;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods;
        }

        public KeyBy getKeyBy() {
            return keyBy;
        }

        public void setKeyBy(KeyBy keyBy) {
            this.keyBy = keyBy;
        }

        public int getPerSecond() {
            return perSecond;
        }

        public void setPerSecond(int perSecond) {
            this.perSecond = perSecond;
        }

        public int getBurst() {
            return burst;
        }

        public void setBurst(int burst) {
            this.burst = burst;
        }
    }

    public enum KeyBy {
        ACCOUNT,
        IP
    }
}
