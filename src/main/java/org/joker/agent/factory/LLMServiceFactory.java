package org.joker.agent.factory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.joker.agent.model.LLMModelConfig;
import org.joker.agent.model.ProviderConfig;
import org.springframework.stereotype.Component;

/**
 * LLM服务工厂，用于创建LLM客户端
 */
@Component
public class LLMServiceFactory {

    /**
     * 获取流式LLM客户端
     *
     * @param config 模型配置
     * @return 流式聊天语言模型
     */
    public StreamingChatModel getStreamingClient(LLMModelConfig config) {
        ProviderConfig providerConfig = new ProviderConfig(config.getApiKey(), config.getBaseUrl(),
                config.getModelEndpoint(), config.getProtocol());

        return LLMProviderService.getStream(config.getProtocol(), providerConfig);
    }

    /**
     * 获取标准LLM客户端
     *
     * @param config 模型配置
     * @return 流式聊天语言模型
     */
    public ChatModel getStrandClient(LLMModelConfig config) {
        ProviderConfig providerConfig = new ProviderConfig(config.getApiKey(), config.getBaseUrl(),
                config.getModelEndpoint(), config.getProtocol());


        return LLMProviderService.getStrand(config.getProtocol(), providerConfig);
    }
}
