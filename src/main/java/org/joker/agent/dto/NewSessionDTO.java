package org.joker.agent.dto;

import lombok.Data;
import org.joker.agent.model.LLMModelConfig;

@Data
public class NewSessionDTO {

    /**
     * 欢迎消息
     */
    private String welcomeMessage;

    /**
     * 聊天模型配置
     */
    private LLMModelConfig agentModelConfig;

    /**
     * embedding模型配置
     */
    private LLMModelConfig embeddingModelConfig;

    /**
     * Agent系统提示词
     */
    private String systemPrompt;

    /**
     * 是否支持多模态
     */
    private Boolean multiModal;

    /**
     * 会话标题
     */
    private String title;

}
