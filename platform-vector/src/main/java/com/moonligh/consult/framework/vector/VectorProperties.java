package com.moonligh.consult.framework.vector;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 向量库地址和百炼向量模型。业务集合名由调用方传入。 */
@ConfigurationProperties(prefix = "consult.vector")
public class VectorProperties {

    private boolean enabled = true;
    private String host = "127.0.0.1";
    private int port = 19530;
    private String apiKey = "";
    private String baseUrl = "https://dashscope.aliyuncs.com";
    private String model = "qwen3-vl-embedding";
    private int timeoutMs = 15000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
