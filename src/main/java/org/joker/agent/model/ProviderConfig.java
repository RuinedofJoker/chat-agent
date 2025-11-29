package org.joker.agent.model;

import lombok.Data;
import org.joker.agent.enums.ProviderProtocol;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProviderConfig {
    /**
     * 密钥
     */
    private final String apiKey;

    /**
     * baseUrl
     */
    private final String baseUrl;

    /**
     * 模型
     */
    private String model;

    private ProviderProtocol protocol;

    private Map<String, String> customHeaders = new HashMap<>();

    public ProviderConfig(String apiKey, String baseUrl, String model, ProviderProtocol protocol) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.protocol = protocol;
    }

    public void addCustomHeaders(String key, String value) {
        customHeaders.put(key, value);
    }
}
