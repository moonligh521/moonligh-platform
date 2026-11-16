package com.moonligh.consult.framework.vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/** 百炼 qwen3-vl-embedding 的请求和回复。只传已经切好的文字。 */
public final class EmbeddingPayload {

    public static final String MODEL = "qwen3-vl-embedding";
    public static final String PATH = "/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding";
    public static final int DIMENSION = 1024;

    private EmbeddingPayload() {
    }

    public static Map<String, Object> request(String model, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model == null || model.isBlank() ? MODEL : model);
        body.put("input", Map.of("contents", List.of(Map.of("text", text == null ? "" : text))));
        body.put("parameters", Map.of("dimension", DIMENSION, "output_type", "dense"));
        return body;
    }

    public static float[] vector(JsonNode body) {
        if (body == null) {
            return null;
        }
        JsonNode embeddings = body.path("output").path("embeddings");
        if (!embeddings.isArray() || embeddings.isEmpty()) {
            return null;
        }
        JsonNode values = embeddings.get(0).path("embedding");
        if (!values.isArray() || values.size() != DIMENSION) {
            return null;
        }
        float[] vector = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            vector[index] = (float) values.get(index).asDouble();
        }
        return vector;
    }
}
