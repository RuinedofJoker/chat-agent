package org.joker.agent.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentEntity {

    private String id;

    /**
     * 聊天模型配置
     */
    private LLMModelConfig agentModelConfig;

    /**
     * embedding模型配置
     */
    private LLMModelConfig embeddingModelConfig;

    /**
     * Agent名称
     */
    private String name;

    /**
     * Agent头像URL
     */
    private String avatar;

    /**
     * Agent描述
     */
    private String description;

    /**
     * Agent系统提示词
     */
    private String systemPrompt;

    /**
     * 欢迎消息
     */
    private String welcomeMessage;

    /**
     * Agent可使用的工具列表
     */
    private List<String> toolIds;

    /**
     * 关联的知识库ID列表
     */
    private List<String> knowledgeBaseIds;

    /**
     * 预先设置工具参数，结构如下： { "<mcpServerName>":{ "toolName":"paranms" } }
     */
    private Map<String, Map<String, Map<String, String>>> toolPresetParams;

    /**
     * 是否支持多模态
     */
    private Boolean multiModal;

}
