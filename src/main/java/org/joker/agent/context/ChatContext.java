package org.joker.agent.context;

import lombok.Data;
import org.joker.agent.model.AgentEntity;
import org.joker.agent.model.LLMModelConfig;
import org.joker.agent.model.MessageEntity;

import java.util.List;

@Data
public class ChatContext {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户消息
     */
    private String userMessage;

    /**
     * 智能体实体
     */
    private AgentEntity agent;

    /**
     * 大模型配置
     */
    private LLMModelConfig llmModelConfig;

    /**
     * 历史消息列表
     */
    private List<MessageEntity> messageHistory;

    /**
     * 使用的 mcp server name
     */
    private List<String> mcpServerNames;

    /**
     * 多模态的文件
     */
    private List<String> fileUrls;

    /**
     * 高可用实例ID
     */
    private String instanceId;

}
