package org.joker.agent.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MilvusProperties.class)
public class EmbeddingStoreConfig {

    @Autowired
    private MilvusProperties milvusProperties;

    /**
     * 记忆向量库
     */
    @Bean(name = "memoryEmbeddingStore")
    public EmbeddingStore<TextSegment> memoryEmbeddingStore() {
        MilvusEmbeddingStore.Builder builder = MilvusEmbeddingStore.builder()
                .host(milvusProperties.getHost())
                .port(milvusProperties.getPort())
                .databaseName(milvusProperties.getDatabaseName());
        if (StringUtils.isNotBlank(milvusProperties.getUsername())) {
            builder.username(milvusProperties.getUsername());
        }
        if (StringUtils.isNotBlank(milvusProperties.getPassword())) {
            builder.password(milvusProperties.getPassword());
        }
        return builder.build();
    }

}
