package org.joker.agent.controller;

import jakarta.annotation.Resource;
import org.joker.agent.dto.ChatRequest;
import org.joker.agent.dto.NewSessionDTO;
import org.joker.agent.model.SessionEntity;
import org.joker.agent.service.AiChatService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AIChatController {

    @Resource
    private AiChatService aiChatService;

    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody @Validated ChatRequest chatRequest) {
        return aiChatService.chat(chatRequest);
    }

    @PostMapping("/createSession")
    public SessionEntity createSession(@RequestBody NewSessionDTO session) {
        return aiChatService.createSession(session);
    }

    @GetMapping("/queryAllSessions")
    public List<SessionEntity> queryAllSessions() {
        return aiChatService.queryAllSessions();
    }

}

