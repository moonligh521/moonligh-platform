package com.moonligh.consult.framework.vector;

import java.util.ArrayList;
import java.util.List;

/**
 * 长文本切分。先在段落、换行、句号处切开，实在装不下才按字切。
 * 相邻两块保留一段重叠，并记下在原文中的位置。
 * 菜谱一行一道菜，不要调用这里。报告和法律材料的长正文可以复用。
 */
public final class TextChunker {

    public static final int MIN_CHARS = 500;
    public static final int MAX_CHARS = 1500;
    public static final int OVERLAP = 100;

    /** 越靠前越优先保留。中文句子和英文句号都算。 */
    private static final String[] SEPARATORS = {"\n\n", "\n", "。", "！", "？", "；", ".", "，", " "};

    private TextChunker() {
    }

    public record Chunk(int index, String text, int start, int end) {
    }

    public static List<String> byLength(String text) {
        return split(text).stream().map(Chunk::text).toList();
    }

    public static List<Chunk> split(String text) {
        return split(text, MAX_CHARS, OVERLAP);
    }

    public static List<Chunk> split(String text, int maxChars, int overlap) {
        if (text == null || text.isBlank() || maxChars < 1) {
            return List.of();
        }
        String body = text.trim();
        int size = Math.max(1, maxChars);
        int shared = Math.max(0, Math.min(overlap, size - 1));
        List<Chunk> chunks = new ArrayList<>();
        int cursor = 0;
        while (cursor < body.length()) {
            int end = breakAt(body, cursor, Math.min(body.length(), cursor + size));
            String piece = body.substring(cursor, end).trim();
            if (!piece.isEmpty()) {
                int from = body.indexOf(piece, cursor);
                chunks.add(new Chunk(chunks.size(), piece, from, from + piece.length()));
            }
            if (end >= body.length()) {
                break;
            }
            int next = Math.max(cursor + 1, end - shared);
            cursor = next;
        }
        return chunks;
    }

    /** 在不超过上限的前提下，选最靠前的分隔符，尽量不把一句话切断。 */
    static int breakAt(String text, int start, int idealEnd) {
        if (idealEnd >= text.length()) {
            return text.length();
        }
        for (String separator : SEPARATORS) {
            int at = text.lastIndexOf(separator, idealEnd);
            if (at >= start) {
                return Math.min(text.length(), at + separator.length());
            }
        }
        return idealEnd;
    }
}
