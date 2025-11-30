package org.joker.agent.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Token消息模型 只包含Token计算所需的必要信息
 */
@Data
public class TokenMessage {

    /**
     * 消息ID
     */
    private String id;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息角色
     */
    private String role;

    /**
     * 消息Token数量
     */
    private Integer tokenCount;

    /**
     * 消息本体Token数量
     */
    private Integer bodyTokenCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 默认构造函数
     */
    public TokenMessage() {
        this.createdAt = LocalDateTime.now();
    }

}
