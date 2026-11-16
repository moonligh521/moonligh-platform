package com.moonligh.consult.framework.vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.dml.UpsertParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;

/** 把调用方切好的一条写入 Milvus，查询只返回主键。连不上时返回空。 */
public class MilvusVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStore.class);

    private final VectorProperties properties;
    private final BailianEmbeddingClient embedding;
    private final Set<String> ready = ConcurrentHashMap.newKeySet();
    private MilvusServiceClient client;

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
        List<UpsertParam.Field> fields = new ArrayList<>();
        fields.add(field("id", List.of(document.id())));
        for (Map.Entry<String, String> entry : fieldsOf(document).entrySet()) {
            fields.add(field(entry.getKey(), List.of(entry.getValue())));
        }
        fields.add(field("embedding", List.of(floats(vector))));
        R<?> response = client.upsert(UpsertParam.newBuilder()
                .withCollectionName(document.collection())
                .withFields(fields)
                .build());
        if (response.getStatus() != R.Status.Success.getCode()) {
            ready.remove(document.collection());
            log.warn("向量写入失败 collection={}", document.collection());
        }
    }

    @Override
    public List<String> search(String collection, float[] vector, String filter, int limit) {
        if (!properties.isEnabled() || blank(collection) || vector == null || vector.length != EmbeddingPayload.DIMENSION
                || !connected()) {
            return List.of();
        }
        SearchParam.Builder query = SearchParam.newBuilder()
                .withCollectionName(collection)
                .withVectorFieldName("embedding")
                .withVectors(List.of(floats(vector)))
                .withTopK(Math.max(1, limit))
                .withMetricType(MetricType.COSINE)
                .withParams("{\"nprobe\":16}")
                .withOutFields(List.of("id"));
        if (filter != null && !filter.isBlank()) {
            query.withExpr(filter);
        }
        R<SearchResults> response = client.search(query.build());
        if (response.getStatus() != R.Status.Success.getCode() || response.getData() == null) {
            return List.of();
        }
        List<?> ids = new SearchResultsWrapper(response.getData().getResults()).getFieldData("id", 0);
        List<String> found = new ArrayList<>();
        if (ids != null) {
            for (Object id : ids) {
                found.add(String.valueOf(id));
            }
        }
        return found;
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
            R<Boolean> exists = client.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(document.collection())
                    .build());
            if (exists.getStatus() != R.Status.Success.getCode()) {
                return false;
            }
            if (!Boolean.TRUE.equals(exists.getData())) {
                List<FieldType> types = new ArrayList<>();
                types.add(textField("id", 128, true));
                for (String name : fieldsOf(document).keySet()) {
                    types.add(textField(name, 512, false));
                }
                types.add(FieldType.newBuilder()
                        .withName("embedding")
                        .withDataType(DataType.FloatVector)
                        .withDimension(EmbeddingPayload.DIMENSION)
                        .build());
                client.createCollection(CreateCollectionParam.newBuilder()
                        .withCollectionName(document.collection())
                        .withFieldTypes(types)
                        .build());
                client.createIndex(CreateIndexParam.newBuilder()
                        .withCollectionName(document.collection())
                        .withFieldName("embedding")
                        .withIndexType(IndexType.IVF_FLAT)
                        .withMetricType(MetricType.COSINE)
                        .withExtraParam("{\"nlist\":128}")
                        .withSyncMode(Boolean.TRUE)
                        .build());
            }
            R<?> loaded = client.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(document.collection())
                    .build());
            if (loaded.getStatus() != R.Status.Success.getCode()) {
                return false;
            }
            ready.add(document.collection());
            return true;
        } catch (RuntimeException error) {
            log.warn("向量集合不可用 collection={} type={}", document.collection(), error.getClass().getSimpleName());
            return false;
        }
    }

    private synchronized boolean connected() {
        if (client != null) {
            return true;
        }
        try {
            client = new MilvusServiceClient(ConnectParam.newBuilder()
                    .withHost(properties.getHost())
                    .withPort(properties.getPort())
                    .withConnectTimeout(3, TimeUnit.SECONDS)
                    .build());
            return true;
        } catch (RuntimeException error) {
            log.warn("向量库连不上 type={}", error.getClass().getSimpleName());
            return false;
        }
    }

    private static Map<String, String> fieldsOf(VectorDocument document) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (document.fields() != null) {
            document.fields().forEach((key, value) -> fields.put(key, value == null ? "" : value));
        }
        return fields;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static FieldType textField(String name, int length, boolean primary) {
        FieldType.Builder builder = FieldType.newBuilder()
                .withName(name)
                .withDataType(DataType.VarChar)
                .withMaxLength(length);
        if (primary) {
            builder.withPrimaryKey(true).withAutoID(false);
        }
        return builder.build();
    }

    private static UpsertParam.Field field(String name, List<?> values) {
        return new UpsertParam.Field(name, values);
    }

    private static List<Float> floats(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) {
            values.add(value);
        }
        return values;
    }
}
