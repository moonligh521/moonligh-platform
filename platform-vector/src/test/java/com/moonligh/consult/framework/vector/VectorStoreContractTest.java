package com.moonligh.consult.framework.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class VectorStoreContractTest {

    @Test
    void saveWithPrecomputedVectorDoesNotEmbed() {
        AtomicInteger embeds = new AtomicInteger();
        RecordingVectorStore store = new RecordingVectorStore(embeds);
        float[] vector = new float[] {0.1f, 0.2f};
        store.save(new VectorDocument("nutrition_recipe", "r1", Map.of("cuisine", "川菜"), "doc", vector));
        assertEquals(0, embeds.get());
        assertSame(vector, store.saved.getFirst().vector());
    }

    @Test
    void queryRequiresNonBlankFilter() {
        RecordingVectorStore store = new RecordingVectorStore(new AtomicInteger());
        store.save(new VectorDocument("nutrition_recipe", "r1", Map.of("cuisine", "川菜"), "doc", new float[] {1f}));
        assertTrue(store.query("nutrition_recipe", "", 10).isEmpty());
        assertEquals(List.of("r1"), store.query("nutrition_recipe", "cuisine == \"川菜\"", 10));
    }

    @Test
    void searchTextReturnsBm25Hits() {
        RecordingVectorStore store = new RecordingVectorStore(new AtomicInteger());
        store.save(new VectorDocument("nutrition_recipe", "r1", Map.of(), "清淡快手番茄蛋", new float[] {1f}));
        assertEquals(List.of("r1"), store.searchText("nutrition_recipe", "清淡", "", 5));
        assertTrue(store.searchText("nutrition_recipe", "", "", 5).isEmpty());
    }

    @Test
    void milvusVersionGateForBm25() {
        assertTrue(MilvusVectorStore.parseMajorMinorAtLeast("v2.5.4", 2, 5));
        assertTrue(MilvusVectorStore.parseMajorMinorAtLeast("2.5.0", 2, 5));
        assertTrue(!MilvusVectorStore.parseMajorMinorAtLeast("v2.3.4", 2, 5));
        assertTrue(!MilvusVectorStore.parseMajorMinorAtLeast("2.4.8", 2, 5));
    }

    private static final class RecordingVectorStore implements VectorStore {
        private final AtomicInteger embeds;
        private final List<VectorDocument> saved = new ArrayList<>();

        private RecordingVectorStore(AtomicInteger embeds) {
            this.embeds = embeds;
        }

        @Override
        public float[] embed(String text) {
            embeds.incrementAndGet();
            return new float[] {1f};
        }

        @Override
        public void save(VectorDocument document) {
            saved.add(document);
        }

        @Override
        public List<String> search(String collection, float[] vector, String filter, int limit) {
            return List.of();
        }

        @Override
        public List<String> searchText(String collection, String text, String filter, int limit) {
            if (text == null || text.isBlank()) {
                return List.of();
            }
            List<String> ids = new ArrayList<>();
            for (VectorDocument document : saved) {
                if (!collection.equals(document.collection())) {
                    continue;
                }
                if (document.text() != null && document.text().contains(text)) {
                    ids.add(document.id());
                }
            }
            return ids.size() <= limit ? ids : ids.subList(0, limit);
        }

        @Override
        public List<String> query(String collection, String filter, int limit) {
            if (filter == null || filter.isBlank()) {
                return List.of();
            }
            List<String> ids = new ArrayList<>();
            for (VectorDocument document : saved) {
                if (!collection.equals(document.collection())) {
                    continue;
                }
                Map<String, String> fields = document.fields() == null ? Map.of() : document.fields();
                if (filter.contains("cuisine == \"川菜\"") && "川菜".equals(fields.get("cuisine"))) {
                    ids.add(document.id());
                }
            }
            return ids.size() <= limit ? ids : ids.subList(0, limit);
        }
    }
}
