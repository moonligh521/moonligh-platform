package com.moonligh.consult.framework.vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.milvus.common.clientenum.FunctionType;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;

/**
 * Milvus v2：稠密向量 + content/BM25 稀疏检索。
 * 写入须带 content 正文；sparse 由 BM25 Function 自动生成。
 */
public class MilvusVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStore.class);
    private static final String CONTENT = "content";
    private static final String SPARSE = "sparse";
    private static final String EMBEDDING = "embedding";
    private static final Gson GSON = new Gson();

    private final VectorProperties properties;
    private final BailianEmbeddingClient embedding;
    private final Set<String> ready = ConcurrentHashMap.newKeySet();
    private MilvusClientV2 client;
    /** null=未探测；BM25 Function 需 Milvus ≥ 2.5。 */
    private Boolean bm25Capable;

    public MilvusVectorStore(VectorProperties properties) {
        this.properties = properties;
        this.embedding = new BailianEmbeddingClient(properties);
    }

    @Override
    public float[] embed(String text) {
        return embedding.embed(text);
    }

    @Override
    public void save(VectorDocument document) {
        if (!properties.isEnabled() || document == null || blank(document.collection()) || blank(document.id())) {
            return;
        }
        float[] vector = document.vector() == null ? embed(document.text()) : document.vector();
        if (vector == null || !ensure(document)) {
            return;
        }
        try {
            JsonObject row = new JsonObject();
            row.addProperty("id", document.id());
            for (Map.Entry<String, String> entry : fieldsOf(document).entrySet()) {
                row.addProperty(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, List<String>> entry : listsOf(document).entrySet()) {
                JsonArray array = new JsonArray();
                for (String value : entry.getValue()) {
                    array.add(value == null ? "" : value);
                }
                row.add(entry.getKey(), array);
            }
            row.addProperty(CONTENT, document.text() == null ? "" : document.text());
            JsonArray embeddingValues = new JsonArray();
            for (float value : vector) {
                embeddingValues.add(value);
            }
            row.add(EMBEDDING, embeddingValues);
            client.upsert(UpsertReq.builder()
                    .collectionName(document.collection())
                    .data(List.of(row))
                    .build());
        } catch (RuntimeException error) {
            ready.remove(document.collection());
            log.warn("向量写入失败 collection={} type={}", document.collection(), error.getClass().getSimpleName());
        }
    }

    @Override
    public List<String> search(String collection, float[] vector, String filter, int limit) {
        if (!properties.isEnabled()
                || blank(collection)
                || vector == null
                || vector.length != EmbeddingPayload.DIMENSION
                || !connected()
                || !loadExisting(collection)) {
            return List.of();
        }
        try {
            List<Float> values = new ArrayList<>(vector.length);
            for (float value : vector) {
                values.add(value);
            }
            SearchReq.SearchReqBuilder builder = SearchReq.builder()
                    .collectionName(collection)
                    .data(List.of(new FloatVec(values)))
                    .annsField(EMBEDDING)
                    .topK(Math.max(1, limit))
                    .outputFields(List.of("id"));
            if (filter != null && !filter.isBlank()) {
                builder.filter(filter);
            }
            return idsOf(client.search(builder.build()));
        } catch (RuntimeException error) {
            log.warn("稠密检索失败 collection={} type={}", collection, error.getClass().getSimpleName());
            return List.of();
        }
    }

    @Override
    public List<String> searchText(String collection, String text, String filter, int limit) {
        if (!properties.isEnabled()
                || blank(collection)
                || blank(text)
                || !connected()
                || !bm25Capable()
                || !loadExisting(collection)) {
            return List.of();
        }
        try {
            SearchReq.SearchReqBuilder builder = SearchReq.builder()
                    .collectionName(collection)
                    .data(List.<BaseVector>of(new EmbeddedText(text)))
                    .annsField(SPARSE)
                    .topK(Math.max(1, limit))
                    .outputFields(List.of("id"));
            if (filter != null && !filter.isBlank()) {
                builder.filter(filter);
            }
            return idsOf(client.search(builder.build()));
        } catch (RuntimeException error) {
            log.warn("BM25 检索失败 collection={} type={}", collection, error.getClass().getSimpleName());
            return List.of();
        }
    }

    @Override
    public List<String> query(String collection, String filter, int limit) {
        if (!properties.isEnabled() || blank(collection) || blank(filter) || !connected() || !loadExisting(collection)) {
            return List.of();
        }
        try {
            QueryResp response = client.query(QueryReq.builder()
                    .collectionName(collection)
                    .filter(filter)
                    .outputFields(List.of("id"))
                    .limit((long) Math.max(1, limit))
                    .build());
            List<String> found = new ArrayList<>();
            if (response == null || response.getQueryResults() == null) {
                return found;
            }
            for (QueryResp.QueryResult row : response.getQueryResults()) {
                Object id = row.getEntity() == null ? null : row.getEntity().get("id");
                if (id != null) {
                    found.add(String.valueOf(id));
                }
            }
            return found;
        } catch (RuntimeException error) {
            log.warn("标量查询失败 collection={} type={}", collection, error.getClass().getSimpleName());
            return List.of();
        }
    }

    @Override
    public void dropCollection(String collection) {
        if (!properties.isEnabled() || blank(collection) || !connected()) {
            return;
        }
        try {
            Boolean exists = client.hasCollection(HasCollectionReq.builder().collectionName(collection).build());
            if (!Boolean.TRUE.equals(exists)) {
                ready.remove(collection);
                return;
            }
            client.dropCollection(DropCollectionReq.builder().collectionName(collection).build());
            ready.remove(collection);
        } catch (RuntimeException error) {
            log.warn("向量集合删除异常 collection={} type={}", collection, error.getClass().getSimpleName());
        }
    }

    private boolean loadExisting(String collection) {
        if (ready.contains(collection)) {
            return true;
        }
        if (!connected()) {
            return false;
        }
        try {
            Boolean exists = client.hasCollection(HasCollectionReq.builder().collectionName(collection).build());
            if (!Boolean.TRUE.equals(exists)) {
                return false;
            }
            client.loadCollection(LoadCollectionReq.builder().collectionName(collection).build());
            ready.add(collection);
            return true;
        } catch (RuntimeException error) {
            log.warn("向量集合不可用 collection={} type={}", collection, error.getClass().getSimpleName());
            return false;
        }
    }

    private synchronized boolean ensure(VectorDocument document) {
        if (!connected()) {
            return false;
        }
        if (ready.contains(document.collection())) {
            return true;
        }
        return open(document);
    }

    private boolean open(VectorDocument document) {
        try {
            Boolean exists = client.hasCollection(HasCollectionReq.builder()
                    .collectionName(document.collection())
                    .build());
            if (!Boolean.TRUE.equals(exists)) {
                createCollection(document);
            }
            client.loadCollection(LoadCollectionReq.builder().collectionName(document.collection()).build());
            ready.add(document.collection());
            return true;
        } catch (RuntimeException error) {
            try {
                client.dropCollection(DropCollectionReq.builder().collectionName(document.collection()).build());
            } catch (RuntimeException ignored) {
                // 建表半失败时尽量清掉残缺集合，下次可重建
            }
            ready.remove(document.collection());
            log.warn("向量集合不可用 collection={} type={}", document.collection(), error.getClass().getSimpleName());
            return false;
        }
    }

    private void createCollection(VectorDocument document) {
        boolean withBm25 = bm25Capable();
        Map<String, String> remarks = document.fieldDescriptions();
        CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder().build();
        schema.addField(AddFieldReq.builder()
                .fieldName("id")
                .dataType(DataType.VarChar)
                .maxLength(128)
                .isPrimaryKey(true)
                .autoID(false)
                .description(remark(remarks, "id", "业务主键"))
                .build());
        for (String name : fieldsOf(document).keySet()) {
            schema.addField(AddFieldReq.builder()
                    .fieldName(name)
                    .dataType(DataType.VarChar)
                    .maxLength(512)
                    .description(remark(remarks, name, "业务过滤字段"))
                    .build());
        }
        for (String name : listsOf(document).keySet()) {
            schema.addField(AddFieldReq.builder()
                    .fieldName(name)
                    .dataType(DataType.Array)
                    .elementType(DataType.VarChar)
                    .maxLength(64)
                    .maxCapacity(32)
                    .description(remark(remarks, name, "业务标签数组"))
                    .build());
        }
        AddFieldReq.AddFieldReqBuilder content = AddFieldReq.builder()
                .fieldName(CONTENT)
                .dataType(DataType.VarChar)
                .maxLength(65535)
                .description(remark(remarks, CONTENT, "菜谱检索正文，BM25 输入"));
        if (withBm25) {
            Map<String, Object> analyzer = new HashMap<>();
            analyzer.put("type", "chinese");
            content.enableAnalyzer(true).analyzerParams(analyzer);
        }
        schema.addField(content.build());
        if (withBm25) {
            schema.addField(AddFieldReq.builder()
                    .fieldName(SPARSE)
                    .dataType(DataType.SparseFloatVector)
                    .description("BM25 稀疏向量，由 Function 生成")
                    .build());
        }
        schema.addField(AddFieldReq.builder()
                .fieldName(EMBEDDING)
                .dataType(DataType.FloatVector)
                .dimension(EmbeddingPayload.DIMENSION)
                .description(remark(remarks, EMBEDDING, "稠密向量；余弦检索"))
                .build());
        if (withBm25) {
            schema.addFunction(CreateCollectionReq.Function.builder()
                    .functionType(FunctionType.BM25)
                    .name("content_bm25")
                    .inputFieldNames(List.of(CONTENT))
                    .outputFieldNames(List.of(SPARSE))
                    .build());
        }

        List<IndexParam> indexes = new ArrayList<>();
        indexes.add(IndexParam.builder()
                .fieldName(EMBEDDING)
                .indexType(IndexParam.IndexType.IVF_FLAT)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(Map.of("nlist", 128))
                .build());
        if (withBm25) {
            indexes.add(IndexParam.builder()
                    .fieldName(SPARSE)
                    .indexType(IndexParam.IndexType.SPARSE_INVERTED_INDEX)
                    .metricType(IndexParam.MetricType.BM25)
                    .extraParams(Map.of(
                            "inverted_index_algo", "DAAT_MAXSCORE",
                            "bm25_k1", 1.2,
                            "bm25_b", 0.75))
                    .build());
        }

        String collectionRemark = blank(document.collectionDescription())
                ? document.collection()
                : document.collectionDescription();
        if (!withBm25) {
            log.warn("Milvus 版本不支持 BM25 Function，集合仅稠密+content 正文 collection={}", document.collection());
        }
        client.createCollection(CreateCollectionReq.builder()
                .collectionName(document.collection())
                .description(collectionRemark)
                .collectionSchema(schema)
                .indexParams(indexes)
                .build());
    }

    /** BM25 Function / metric 自 Milvus 2.5 起可用。 */
    private boolean bm25Capable() {
        if (bm25Capable != null) {
            return bm25Capable;
        }
        if (!connected()) {
            return false;
        }
        try {
            String raw = client.getServerVersion();
            bm25Capable = parseMajorMinorAtLeast(raw, 2, 5);
            if (!bm25Capable) {
                log.warn("Milvus {} 无内置 BM25，searchText 将空返回；升级到 2.5+ 后 drop+重灌即可启用", raw);
            }
            return bm25Capable;
        } catch (RuntimeException error) {
            bm25Capable = false;
            return false;
        }
    }

    public static boolean parseMajorMinorAtLeast(String version, int major, int minor) {
        if (version == null || version.isBlank()) {
            return false;
        }
        String digits = version.trim();
        if (digits.startsWith("v") || digits.startsWith("V")) {
            digits = digits.substring(1);
        }
        String[] parts = digits.split("[^0-9]+");
        if (parts.length < 2) {
            return false;
        }
        try {
            int maj = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            return maj > major || (maj == major && min >= minor);
        } catch (NumberFormatException error) {
            return false;
        }
    }

    private synchronized boolean connected() {
        if (client != null) {
            return true;
        }
        try {
            String host = blank(properties.getHost()) ? "127.0.0.1" : properties.getHost();
            int port = properties.getPort() <= 0 ? 19530 : properties.getPort();
            client = new MilvusClientV2(ConnectConfig.builder()
                    .uri("http://" + host + ":" + port)
                    .connectTimeoutMs(3000)
                    .build());
            return true;
        } catch (RuntimeException error) {
            log.warn("向量库连不上 type={}", error.getClass().getSimpleName());
            return false;
        }
    }

    private static List<String> idsOf(SearchResp response) {
        List<String> found = new ArrayList<>();
        if (response == null || response.getSearchResults() == null || response.getSearchResults().isEmpty()) {
            return found;
        }
        for (List<SearchResp.SearchResult> group : response.getSearchResults()) {
            if (group == null) {
                continue;
            }
            for (SearchResp.SearchResult hit : group) {
                Object id = hit.getId();
                if (id == null && hit.getEntity() != null) {
                    id = hit.getEntity().get("id");
                }
                if (id != null) {
                    found.add(String.valueOf(id));
                }
            }
        }
        return found;
    }

    private static Map<String, String> fieldsOf(VectorDocument document) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (document.fields() != null) {
            document.fields().forEach((key, value) -> fields.put(key, value == null ? "" : value));
        }
        return fields;
    }

    private static Map<String, List<String>> listsOf(VectorDocument document) {
        Map<String, List<String>> lists = new LinkedHashMap<>();
        if (document.listFields() != null) {
            document.listFields().forEach((key, value) ->
                    lists.put(key, value == null ? List.of() : List.copyOf(value)));
        }
        return lists;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String remark(Map<String, String> remarks, String field, String fallback) {
        if (remarks == null) {
            return fallback;
        }
        String value = remarks.get(field);
        return blank(value) ? fallback : value;
    }
}
