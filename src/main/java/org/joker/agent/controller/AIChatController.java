package org.joker.agent.controller;

import jakarta.annotation.Resource;
import org.joker.agent.dto.ChatRequest;
import org.joker.agent.dto.NewSessionDTO;
import org.joker.agent.model.MessageEntity;
import org.joker.agent.model.SessionEntity;
import org.joker.agent.service.AiChatService;
import org.joker.agent.service.ChatSessionManager;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AIChatController {

    @Resource
    private AiChatService aiChatService;

    @Resource
    private ChatSessionManager chatSessionManager;

    /**
     * 发起聊天
     */
    @PostMapping("/chat")
    public void chat(@RequestBody @Validated ChatRequest chatRequest) {
        // 调用服务处理聊天，但不返回SseEmitter
        aiChatService.chat(chatRequest);
    }

    /**
     * SSE流式接口 - 获取指定session的流式消息
     * 前端通过EventSource连接此接口接收AI回复
     */
    @GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String sessionId) {
        // 从会话管理器中获取SseEmitter
        ChatSessionManager.SessionInfo sessionInfo = chatSessionManager.getActiveSessions().get(sessionId);

        if (sessionInfo != null) {
            // 如果会话存在，返回其SseEmitter
            return sessionInfo.getEmitter();
        }

        // 如果会话不存在，创建一个等待的SseEmitter
        // 最多等待30秒，让POST /chat有时间创建会话
        SseEmitter emitter = new SseEmitter(30000L);

        emitter.onCompletion(() -> {
            chatSessionManager.removeSession(sessionId);
        });

        emitter.onTimeout(() -> {
            chatSessionManager.removeSession(sessionId);
        });

        emitter.onError((throwable) -> {
            chatSessionManager.removeSession(sessionId);
        });

        return emitter;
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
