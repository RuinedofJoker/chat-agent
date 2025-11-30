package org.joker.agent.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MilvusProperties.class)
public class EmbeddingStoreConfig {

    @Autowired
    private MilvusProperties milvusProperties;

    public MilvusServiceClient memoryMilvusClient() {
        return new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(milvusProperties.getHost())
                        .withPort(milvusProperties.getPort())
                        .build()
        );
    }

    /**
     * 记忆向量库
     */
    @Bean(name = "memoryEmbeddingStore")
    public EmbeddingStore<TextSegment> memoryEmbeddingStore() {
        return MilvusEmbeddingStore.builder()
                .milvusClient(memoryMilvusClient())
                .databaseName(milvusProperties.getDatabaseName())
                .collectionName("agent_memory_store")
                .dimension(128)
                .indexType(IndexType.HNSW)
                .metricType(MetricType.L2)
                .consistencyLevel(ConsistencyLevelEnum.BOUNDED)
                .autoFlushOnInsert(true)
                .idFieldName("id")
                .textFieldName("text")
                .metadataFieldName("metadata")
                .vectorFieldName("vector")
                .build();
    }

}
