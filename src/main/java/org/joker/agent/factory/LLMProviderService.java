package org.joker.agent.factory;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.joker.agent.enums.ProviderProtocol;
import org.joker.agent.model.ProviderConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class LLMProviderService {

    public static ChatModel getStrand(ProviderProtocol protocol, ProviderConfig providerConfig) {
        ChatModel model = null;
        if (protocol == ProviderProtocol.OPENAI) {
            OpenAiChatModel.OpenAiChatModelBuilder openAiChatModelBuilder = new OpenAiChatModel.OpenAiChatModelBuilder();
            openAiChatModelBuilder.apiKey(providerConfig.getApiKey());
            openAiChatModelBuilder.baseUrl(providerConfig.getBaseUrl());
            openAiChatModelBuilder.customHeaders(providerConfig.getCustomHeaders());
            openAiChatModelBuilder.modelName(providerConfig.getModel());
            openAiChatModelBuilder.timeout(Duration.ofHours(1));
            model = new OpenAiChatModel(openAiChatModelBuilder);
        } else if (protocol == ProviderProtocol.ANTHROPIC) {
            model = AnthropicChatModel.builder().apiKey(providerConfig.getApiKey()).baseUrl(providerConfig.getBaseUrl())
                    .modelName(providerConfig.getModel()).version("2023-06-01").timeout(Duration.ofHours(1)).build();
        }
        return model;
    }

    public static StreamingChatModel getStream(ProviderProtocol protocol, ProviderConfig providerConfig) {
        StreamingChatModel model = null;
        if (protocol == ProviderProtocol.OPENAI) {
            model = new OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder().apiKey(providerConfig.getApiKey())
                    .baseUrl(providerConfig.getBaseUrl()).customHeaders(providerConfig.getCustomHeaders())
                    .modelName(providerConfig.getModel()).timeout(Duration.ofHours(1)).build();
        } else if (protocol == ProviderProtocol.ANTHROPIC) {
            model = AnthropicStreamingChatModel.builder().apiKey(providerConfig.getApiKey())
                    .baseUrl(providerConfig.getBaseUrl()).version("2023-06-01").modelName(providerConfig.getModel())
                    .timeout(Duration.ofHours(1)).build();
        }
        return model;
    }
}
