package com.moonligh.consult.framework.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void shortTextStaysOnePiece() {
        var chunks = TextChunker.split("一行一道菜，不按字数切开。");
        assertEquals(1, chunks.size());
        assertEquals(0, chunks.getFirst().start());
    }

    @Test
    void prefersTheSentenceBoundary() {
        String text = "第一句很短。第二句" + "甲".repeat(30);
        int end = TextChunker.breakAt(text, 0, 8);
        assertTrue(text.substring(0, end).endsWith("。"));
    }

    @Test
    void longTextOverlapsAndKeepsPosition() {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < 40; index++) {
            text.append("第").append(index).append("段说明。\n");
            text.append("甲".repeat(80)).append('\n');
        }
        var chunks = TextChunker.split(text.toString(), 200, 20);
        assertTrue(chunks.size() > 1);
        assertTrue(chunks.getFirst().text().length() <= 200);
        TextChunker.Chunk second = chunks.get(1);
        assertEquals(second.text(), text.substring(second.start(), second.end()).trim());
        assertTrue(second.start() < chunks.getFirst().end());
    }
}
