package org.joker.agent.controller;

import jakarta.annotation.Resource;
import org.joker.agent.dto.ChatRequest;
import org.joker.agent.service.AiChatService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai/chat")
public class AIChatController {

    @Resource
    private AiChatService aiChatService;

    @PostMapping
    public SseEmitter chat(@RequestBody @Validated ChatRequest chatRequest) {
        return aiChatService.chat(chatRequest);
    }
}

