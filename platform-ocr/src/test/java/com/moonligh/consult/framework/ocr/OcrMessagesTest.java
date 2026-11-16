package com.moonligh.consult.framework.ocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OcrMessagesTest {

    @Test
    void requestCarriesCallerPromptAndImage() {
        String body = OcrMessages.request("qwen-vl-plus", new byte[] {1, 2}, "image/png", "只抄检验数字");
        assertTrue(body.contains("\"model\":\"qwen-vl-plus\""));
        assertTrue(body.contains("只抄检验数字"));
        assertTrue(body.contains("data:image/png;base64,"));
    }

    @Test
    void readsTextAndDropsFence() {
        String raw = """
                {"choices":[{"message":{"content":"```text\\n葡萄糖 5.2\\n```"}}]}
                """;
        assertEquals("葡萄糖 5.2", OcrMessages.text(raw));
    }
}
