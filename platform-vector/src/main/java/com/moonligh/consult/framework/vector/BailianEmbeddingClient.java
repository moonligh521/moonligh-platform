package com.moonligh.consult.framework.vector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

/** 把一条已经切好的文本交给百炼。失败返回空。 */
final class BailianEmbeddingClient {

    private final VectorProperties properties;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper json = new ObjectMapper();

    BailianEmbeddingClient(VectorProperties properties) {
        this.properties = properties;
    }

    float[] embed(String text) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank() || text == null || text.isBlank()) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trim(properties.getBaseUrl()) + EmbeddingPayload.PATH))
                    .timeout(Duration.ofMillis(Math.max(1000, properties.getTimeoutMs())))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(
                            EmbeddingPayload.request(properties.getModel(), text))))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return null;
            }
            return EmbeddingPayload.vector(json.readTree(response.body()));
        } catch (Exception error) {
            return null;
        }
    }

    private static String trim(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://dashscope.aliyuncs.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
