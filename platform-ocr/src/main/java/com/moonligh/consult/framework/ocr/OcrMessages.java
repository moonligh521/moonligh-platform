package com.moonligh.consult.framework.ocr;

import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** 组装百炼视觉请求，并取出回复里的文字。 */
final class OcrMessages {

    static final String DEFAULT_PROMPT = "只输出图片中的文字，不要解释。";

    private static final ObjectMapper JSON = new ObjectMapper();

    private OcrMessages() {
    }

    static String request(String model, byte[] image, String mimeType, String prompt) {
        String mime = mimeType == null || mimeType.isBlank() ? "image/png" : mimeType;
        String words = prompt == null || prompt.isBlank() ? DEFAULT_PROMPT : prompt;
        String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(image);
        ObjectNode root = JSON.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0);
        ArrayNode messages = root.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        ArrayNode content = user.putArray("content");
        content.addObject().put("type", "text").put("text", words);
        content.addObject()
                .put("type", "image_url")
                .putObject("image_url")
                .put("url", dataUrl);
        return root.toString();
    }

    static String text(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        try {
            JsonNode content = JSON.readTree(responseBody).path("choices").path(0).path("message").path("content");
            if (content.isTextual()) {
                return stripFence(content.asText());
            }
            if (content.isArray()) {
                StringBuilder text = new StringBuilder();
                for (JsonNode part : content) {
                    if (part.path("text").isTextual()) {
                        text.append(part.path("text").asText());
                    }
                }
                return stripFence(text.toString());
            }
            return "";
        } catch (Exception error) {
            return "";
        }
    }

    static String stripFence(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int line = text.indexOf('\n');
            if (line > 0) {
                text = text.substring(line + 1);
            }
            int end = text.lastIndexOf("```");
            if (end >= 0) {
                text = text.substring(0, end);
            }
        }
        return text.trim();
    }
}
