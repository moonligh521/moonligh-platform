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
        store.save(new VectorDocument("recipe_chunk", "r1", Map.of("cuisine", "川菜"), "doc", vector));
        assertEquals(0, embeds.get());
        assertSame(vector, store.saved.getFirst().vector());
    }

    @Test
    void queryRequiresNonBlankFilter() {
        RecordingVectorStore store = new RecordingVectorStore(new AtomicInteger());
        store.save(new VectorDocument("recipe_chunk", "r1", Map.of("cuisine", "川菜"), "doc", new float[] {1f}));
        assertTrue(store.query("recipe_chunk", "", 10).isEmpty());
        assertEquals(List.of("r1"), store.query("recipe_chunk", "cuisine == \"川菜\"", 10));
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
