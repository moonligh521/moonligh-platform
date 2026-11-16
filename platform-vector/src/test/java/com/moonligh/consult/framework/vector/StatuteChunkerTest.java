package com.moonligh.consult.framework.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatuteChunkerTest {

    @Test
    void keepsParagraphsAndItemsInsideOneArticle() {
        String text = """
                第一编 总则
                第一章 基本规定
                第一条 为了保护民事主体的合法权益。
                民事主体的人身权利受法律保护。
                （一）行为人具有相应的民事行为能力
                第二条 民法调整平等主体之间的人身关系和财产关系。
                第二章 自然人
                第十三条 自然人从出生时起到死亡时止，具有民事权利能力。
                """;
        var articles = StatuteChunker.split("中华人民共和国民法典", text);
        assertEquals(3, articles.size());
        StatuteChunker.Article first = articles.getFirst();
        assertEquals("第一条", first.articleNo());
        assertTrue(first.text().contains("（一）行为人具有相应的民事行为能力"));
        assertTrue(first.text().contains("人身权利受法律保护"));
        assertTrue(first.path().contains("第一编 总则"));
        assertTrue(first.path().contains("第一章 基本规定"));
        assertEquals("第十三条", articles.get(2).articleNo());
        assertTrue(articles.get(2).path().contains("第二章 自然人"));
    }

    @Test
    void keepsArticleSuffixTogether() {
        String text = """
                第一百四十三条 具备下列条件的民事法律行为有效。
                第一百四十三条之一 本条是示例。
                """;
        var articles = StatuteChunker.split("民法典", text);
        assertEquals(2, articles.size());
        assertEquals("第一百四十三条", articles.get(0).articleNo());
        assertEquals("第一百四十三条之一", articles.get(1).articleNo());
    }
}
