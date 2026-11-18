package com.moonligh.consult.framework.vector;

import java.util.List;
import java.util.Map;

/**
 * 调用方已经切好的一条。vector 为空时，保存前再做向量化。
 * collectionDescription / fieldDescriptions 仅在集合首次创建时写入 Milvus 备注。
 * listFields 写入 ARRAY&lt;VARCHAR&gt;，可用 array_contains 过滤（旧版 like "%x%" 不可用）。
 */
public record VectorDocument(
        String collection,
        String id,
        Map<String, String> fields,
        String text,
        float[] vector,
        String collectionDescription,
        Map<String, String> fieldDescriptions,
        Map<String, List<String>> listFields) {

    public VectorDocument {
        collectionDescription = collectionDescription == null ? "" : collectionDescription;
        fieldDescriptions = fieldDescriptions == null ? Map.of() : Map.copyOf(fieldDescriptions);
        listFields = listFields == null ? Map.of() : Map.copyOf(listFields);
    }

    public VectorDocument(String collection, String id, Map<String, String> fields, String text) {
        this(collection, id, fields, text, null, "", Map.of(), Map.of());
    }

    public VectorDocument(String collection, String id, Map<String, String> fields, String text, float[] vector) {
        this(collection, id, fields, text, vector, "", Map.of(), Map.of());
    }

    public VectorDocument(
            String collection,
            String id,
            Map<String, String> fields,
            String text,
            float[] vector,
            String collectionDescription,
            Map<String, String> fieldDescriptions) {
        this(collection, id, fields, text, vector, collectionDescription, fieldDescriptions, Map.of());
    }
}
