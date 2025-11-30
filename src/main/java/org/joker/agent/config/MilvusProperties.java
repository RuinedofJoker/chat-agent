package org.joker.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vector-database.milvus")
@Data
public class MilvusProperties {

    private String host;
    private int port;
    private String databaseName;

}
