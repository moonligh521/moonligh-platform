package com.moonligh.consult.framework.ocr;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 百炼视觉识图。主模型没有文字时再试备用模型。不整理版面，也不保存材料。 */
public class DashScopeOcrClient implements OcrClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeOcrClient.class);

    private final OcrProperties properties;
    private final HttpClient http;

    public DashScopeOcrClient(OcrProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Override
    public Optional<String> recognize(byte[] image, String mimeType, String prompt) {
        if (image == null || image.length == 0 || !hasKey()) {
            return Optional.empty();
        }
        Optional<String> first = once(properties.getModel(), image, mimeType, prompt);
        if (first.isPresent()) {
            return first;
        }
        String fallback = properties.getFallbackModel();
        if (fallback == null || fallback.isBlank() || fallback.equalsIgnoreCase(properties.getModel())) {
            return Optional.empty();
        }
        return once(fallback, image, mimeType, prompt);
    }

    private Optional<String> once(String model, byte[] image, String mimeType, String prompt) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trim(properties.getBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofMillis(Math.max(1000, properties.getTimeoutMs())))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OcrMessages.request(model, image, mimeType, prompt)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("OCR 调用失败 status={}", response.statusCode());
                return Optional.empty();
            }
            String text = OcrMessages.text(response.body());
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (Exception error) {
            log.warn("OCR 调用失败 type={}", error.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private boolean hasKey() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    private static String trim(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
