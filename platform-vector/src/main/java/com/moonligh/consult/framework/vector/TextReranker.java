package com.moonligh.consult.framework.vector;

import java.util.List;

/** 对已召回文档按 query 重排，只返回 id；失败返回空列表由调用方回退。 */
public interface TextReranker {

    /**
     * @param query 用户查询
     * @param ids 候选主键，与 texts 一一对应
     * @param texts 候选正文
     * @param topN 返回条数上限
     * @return 按相关性降序的 id；失败或未配置时返回空
     */
    List<String> rerank(String query, List<String> ids, List<String> texts, int topN);
}
