package org.joker.agent.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ChatRequest {

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不可为空")
    private String message;

    /**
     * 会话ID
     */
    @NotBlank(message = "会话id不可为空")
    private String sessionId;

}
