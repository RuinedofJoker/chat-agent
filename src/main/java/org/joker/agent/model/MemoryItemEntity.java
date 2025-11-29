package org.joker.agent.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MemoryItemEntity {

    private String id;

    private String sessionId;

    private String type; // 使用字符串存储，取值见 MemoryType

    private String text;

    private Map<String, Object> data;

    private Float importance;

    private List<String> tags;

    private String sourceSessionId;

    private String dedupeHash;

    private Integer status; // 1=active, 0=archived/deleted

    public static final int ACTIVE = 1;
    public static final int INACTIVE = 0;

}
