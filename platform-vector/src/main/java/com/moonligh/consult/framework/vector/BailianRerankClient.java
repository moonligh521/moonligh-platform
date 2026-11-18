package com.moonligh.consult.framework.vector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 百炼 gte-rerank-v2 原生 text-rerank；失败返回空列表。 */
final class BailianRerankClient implements TextReranker {

    private static final Logger log = LoggerFactory.getLogger(BailianRerankClient.class);
    private static final String PATH = "/api/v1/services/rerank/text-rerank/text-rerank";

    private final VectorProperties properties;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper json = new ObjectMapper();

    BailianRerankClient(VectorProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<String> rerank(String query, List<String> ids, List<String> texts, int topN) {
        if (!properties.isEnabled()
                || !properties.isRerankEnabled()
                || blank(properties.getApiKey())
                || blank(query)
                || ids == null
                || texts == null
                || ids.isEmpty()
                || ids.size() != texts.size()) {
            return List.of();
        }
        int limit = Math.max(1, Math.min(topN <= 0 ? ids.size() : topN, ids.size()));
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", blank(properties.getRerankModel()) ? "gte-rerank-v2" : properties.getRerankModel());
            body.put("input", Map.of("query", query, "documents", texts));
            body.put("parameters", Map.of("top_n", limit, "return_documents", false));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trim(properties.getBaseUrl()) + PATH))
                    .timeout(Duration.ofMillis(Math.max(200, properties.getRerankTimeoutMs())))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("rerank http status={}", response.statusCode());
                return List.of();
            }
            JsonNode results = json.readTree(response.body()).path("output").path("results");
            if (!results.isArray() || results.isEmpty()) {
                return List.of();
            }
            List<Scored> scored = new ArrayList<>();
            for (JsonNode node : results) {
                int index = node.path("index").asInt(-1);
                if (index < 0 || index >= ids.size()) {
                    continue;
                }
                scored.add(new Scored(ids.get(index), node.path("relevance_score").asDouble(0)));
            }
            scored.sort(Comparator.comparingDouble(Scored::score).reversed());
            List<String> ordered = new ArrayList<>();
            for (Scored item : scored) {
                if (ordered.size() >= limit) {
                    break;
                }
                ordered.add(item.id());
            }
            return ordered;
        } catch (Exception error) {
            log.warn("rerank failed type={}", error.getClass().getSimpleName());
            return List.of();
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://dashscope.aliyuncs.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record Scored(String id, double score) {
    }
}
