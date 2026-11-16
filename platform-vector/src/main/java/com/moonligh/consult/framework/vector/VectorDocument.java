package com.moonligh.consult.framework.vector;

import java.util.Map;

/** 调用方已经切好的一条。vector 为空时，保存前再做向量化。 */
public record VectorDocument(
        String collection, String id, Map<String, String> fields, String text, float[] vector) {

    public VectorDocument(String collection, String id, Map<String, String> fields, String text) {
        this(collection, id, fields, text, null);
    }
}
