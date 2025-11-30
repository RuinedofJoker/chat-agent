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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/ai")
public class AIChatController {

    @Resource
    private AiChatService aiChatService;
    
    // 存储sessionId到SseEmitter的映射
    private static final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 发起聊天（POST）- 创建SseEmitter连接
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody @Validated ChatRequest chatRequest) {
        String sessionId = chatRequest.getSessionId();
        
        // 创建SseEmitter
        SseEmitter emitter = aiChatService.chat(chatRequest);
        
        // 存储到映射中
        emitters.put(sessionId, emitter);
        
        // 添加清理回调
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(e -> emitters.remove(sessionId));
        
        // 立即返回会话ID
        return Map.of("sessionId", sessionId);
    }

    /**
     * 接收SSE流（GET）- EventSource连接
     */
    @GetMapping("/stream/{sessionId}")
    public SseEmitter stream(@PathVariable String sessionId) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            return emitter;
        }
        // 如果没有活动的连接，返回一个空的SseEmitter
        return new SseEmitter();
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
