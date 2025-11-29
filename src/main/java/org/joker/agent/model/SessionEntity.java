package org.joker.agent.model;

import lombok.Data;

@Data
public class SessionEntity {

    /**
     * 会话唯一ID
     */
    private String id;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 关联的Agent版本ID
     */
    private String agentId;

    /**
     * 会话描述
     */
    private String description;

    /**
     * 会话元数据，可存储其他自定义信息
     */
    private String metadata;

}
