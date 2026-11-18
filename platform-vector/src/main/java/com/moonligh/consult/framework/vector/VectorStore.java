package com.moonligh.consult.framework.vector;

import java.util.List;

/** 写入向量，查询只返回主键。失败时写入被忽略，查询返回空列表。 */
public interface VectorStore {

    float[] embed(String text);

    void save(VectorDocument document);

    /** 稠密向量单路检索。 */
    List<String> search(String collection, float[] vector, String filter, int limit);

    /**
     * BM25 文本单路检索（依赖集合内 content + sparse BM25 Function）。
     * 未实现或库侧不支持时返回空列表。
     */
    default List<String> searchText(String collection, String text, String filter, int limit) {
        return List.of();
    }

    /** 仅按过滤条件取主键。filter 为空时返回空，避免全表扫描。 */
    List<String> query(String collection, String filter, int limit);

    /** 删除集合；测试或换 schema 时用。集合不存在时忽略。 */
    default void dropCollection(String collection) {
    }
}
