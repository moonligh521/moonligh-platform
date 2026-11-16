package com.moonligh.consult.framework.vector;

import java.util.List;

/** 写入向量，查询只返回主键。失败时写入被忽略，查询返回空列表。 */
public interface VectorStore {

    float[] embed(String text);

    void save(VectorDocument document);

    List<String> search(String collection, float[] vector, String filter, int limit);
}
