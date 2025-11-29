package org.joker.agent.dto;

import lombok.Data;
import org.joker.agent.enums.MemoryType;

import java.util.List;

/**
 * 记忆检索结果（用于组装上下文）
 */
@Data
public class MemoryResult {
    private String itemId;
    private MemoryType type;
    private String text;
    private Float importance;
    private Double score;
    private List<String> tags;
}


