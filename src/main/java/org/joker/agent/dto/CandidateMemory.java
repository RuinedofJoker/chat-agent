package org.joker.agent.dto;

import lombok.Data;
import org.joker.agent.enums.MemoryType;

import java.util.List;
import java.util.Map;

/**
 * 记忆候选（抽取器输出）
 */
@Data
public class CandidateMemory {
    private MemoryType type;
    private String text;
    private Float importance;
    private List<String> tags;
    private Map<String, Object> data;
}
