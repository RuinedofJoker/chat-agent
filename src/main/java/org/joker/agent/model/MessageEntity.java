package org.joker.agent.model;

import lombok.Data;
import org.joker.agent.enums.MessageType;
import org.joker.agent.enums.Role;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MessageEntity {

    /**
     * 消息唯一ID
     */
    private String id;

    /**
     * 所属会话ID
     */
    private String sessionId;

    /**
     * 消息角色 (user, assistant, system)
     */
    private Role role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
     */
    private MessageType messageType = MessageType.TEXT;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * Token数量
     */
    private Integer tokenCount = 0;

    /**
     * 消息本体Token数量
     */
    private Integer bodyTokenCount = 0;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 消息元数据
     */
    private String metadata;

    /**
     * 消息是否活跃
     */
    private Boolean isActive = true;

    private List<String> fileUrls = new ArrayList<>();

    public boolean isUserMessage() {
        return this.role == Role.USER;
    }

    public boolean isAIMessage() {
        return this.role == Role.ASSISTANT;
    }

    public boolean isSystemMessage() {
        return this.role == Role.SYSTEM;
    }

    public boolean isSummaryMessage() {
        return this.role == Role.SUMMARY;
    }

}
