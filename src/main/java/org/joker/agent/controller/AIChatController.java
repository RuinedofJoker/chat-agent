package org.joker.agent.controller;

import jakarta.annotation.Resource;
import org.joker.agent.dto.ChatRequest;
import org.joker.agent.dto.NewSessionDTO;
import org.joker.agent.model.MessageEntity;
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

    /**
     * 发起聊天
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody @Validated ChatRequest chatRequest) {
        return aiChatService.chat(chatRequest);
    }

    /**
     * 创建一个新的session
     */
    @PostMapping("/createSession")
    public SessionEntity createSession(@RequestBody NewSessionDTO session) {
        return aiChatService.createSession(session);
    }

    /**
     * 查询历史的所有session
     */
    @GetMapping("/queryAllSessions")
    public List<SessionEntity> queryAllSessions() {
        return aiChatService.queryAllSessions();
    }

    /**
     * 查询session下所有的历史消息
     */
    @GetMapping("/queryHistoryMessages/{sessionId}")
    public List<MessageEntity> queryHistoryMessages(@PathVariable String sessionId) {
        return aiChatService.queryHistoryMessages(sessionId);
    }

}

