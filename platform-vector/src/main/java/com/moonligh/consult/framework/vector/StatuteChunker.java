package com.moonligh.consult.framework.vector;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按条切中国法律。一条里的款和项不拆开。
 * 编、章、节只作为这条的位置，不单独成块。
 * 民法典、刑法这类官方法律用这个，不用按字数切。
 */
public final class StatuteChunker {

    private static final Pattern PART = Pattern.compile("^第[零〇一二三四五六七八九十百千万0-9]+编.*");
    private static final Pattern SUBPART = Pattern.compile("^第[零〇一二三四五六七八九十百千万0-9]+分编.*");
    private static final Pattern CHAPTER = Pattern.compile("^第[零〇一二三四五六七八九十百千万0-9]+章.*");
    private static final Pattern SECTION = Pattern.compile("^第[零〇一二三四五六七八九十百千万0-9]+节.*");
    private static final Pattern ARTICLE = Pattern.compile(
            "^(第[零〇一二三四五六七八九十百千万0-9]+条(?:之[零〇一二三四五六七八九十0-9]+)?)(.*)$");

    private StatuteChunker() {
    }

    /** 一条法律。text 含本条全部款、项。path 是编、章、节，便于过滤。 */
    public record Article(
            int index,
            String lawName,
            String part,
            String subpart,
            String chapter,
            String section,
            String articleNo,
            String path,
            String text,
            int start,
            int end) {
    }

    public static List<Article> split(String lawName, String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] lines = text.split("\\R", -1);
        List<Article> articles = new ArrayList<>();
        String part = "";
        String subpart = "";
        String chapter = "";
        String section = "";
        String articleNo = "";
        StringBuilder body = new StringBuilder();
        int bodyStart = 0;
        int cursor = 0;
        boolean open = false;
        for (String raw : lines) {
            String line = raw.trim();
            int lineEnd = cursor + raw.length();
            if (line.isEmpty()) {
                cursor = lineEnd + 1;
                continue;
            }
            if (PART.matcher(line).matches()) {
                flush(articles, lawName, part, subpart, chapter, section, articleNo, body, bodyStart, cursor);
                part = line;
                subpart = "";
                chapter = "";
                section = "";
                open = false;
            } else if (SUBPART.matcher(line).matches()) {
                flush(articles, lawName, part, subpart, chapter, section, articleNo, body, bodyStart, cursor);
                subpart = line;
                chapter = "";
                section = "";
                open = false;
            } else if (CHAPTER.matcher(line).matches()) {
                flush(articles, lawName, part, subpart, chapter, section, articleNo, body, bodyStart, cursor);
                chapter = line;
                section = "";
                open = false;
            } else if (SECTION.matcher(line).matches()) {
                flush(articles, lawName, part, subpart, chapter, section, articleNo, body, bodyStart, cursor);
                section = line;
                open = false;
            } else {
                Matcher article = ARTICLE.matcher(line);
                if (article.matches()) {
                    flush(articles, lawName, part, subpart, chapter, section, articleNo, body, bodyStart, cursor);
                    articleNo = article.group(1);
                    bodyStart = cursor;
                    body.append(line);
                    open = true;
                } else if (open) {
                    body.append('\n').append(line);
                }
            }
            cursor = lineEnd + 1;
        }
        flush(articles, lawName, part, subpart, chapter, section, articleNo, body, bodyStart, text.length());
        return articles;
    }

    private static void flush(
            List<Article> articles,
            String lawName,
            String part,
            String subpart,
            String chapter,
            String section,
            String articleNo,
            StringBuilder body,
            int start,
            int end) {
        if (articleNo == null || articleNo.isBlank() || body.isEmpty()) {
            body.setLength(0);
            return;
        }
        String path = join(part, subpart, chapter, section);
        articles.add(new Article(
                articles.size(),
                lawName == null ? "" : lawName,
                part,
                subpart,
                chapter,
                section,
                articleNo,
                path,
                body.toString().trim(),
                start,
                end));
        body.setLength(0);
    }

    private static String join(String... parts) {
        StringBuilder path = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!path.isEmpty()) {
                path.append(" / ");
            }
            path.append(part);
        }
        return path.toString();
    }
}
