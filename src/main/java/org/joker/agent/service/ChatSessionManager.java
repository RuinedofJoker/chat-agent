package org.joker.agent.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.joker.agent.utils.SseEmitterUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class ChatSessionManager {

    /**
     * 会话信息
     */
    @Data
    public static class SessionInfo {
        private final String sessionId;
        private final SseEmitter emitter;
        private final AtomicBoolean interrupted;
        private final long startTime;

        public SessionInfo(String sessionId, SseEmitter emitter) {
            this.sessionId = sessionId;
            this.emitter = emitter;
            this.interrupted = new AtomicBoolean(false);
            this.startTime = System.currentTimeMillis();
        }

        public boolean isInterrupted() {
            return interrupted.get();
        }

        public void setInterrupted() {
            interrupted.set(true);
        }
    }

    // 使用sessionId作为key，存储正在进行的对话会话
    private final ConcurrentHashMap<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    /** 注册一个新的对话会话
     * @param sessionId 会话ID
     * @param emitter SSE发送器 */
    public void registerSession(String sessionId, SseEmitter emitter) {
        SessionInfo sessionInfo = new SessionInfo(sessionId, emitter);
        activeSessions.put(sessionId, sessionInfo);
        log.info("注册对话会话: sessionId={}", sessionId);

        // 设置SSE完成和超时回调，自动清理会话
        emitter.onCompletion(() -> {
            removeSession(sessionId);
            log.info("对话会话完成: sessionId={}", sessionId);
        });

        emitter.onTimeout(() -> {
            removeSession(sessionId);
            log.warn("对话会话超时: sessionId={}", sessionId);
        });

        emitter.onError((throwable) -> {
            removeSession(sessionId);
            log.error("对话会话错误: sessionId={}, error={}", sessionId, throwable.getMessage());
        });
    }

    /** 移除对话会话
     * @param sessionId 会话ID */
    public void removeSession(String sessionId) {
        SessionInfo removed = activeSessions.remove(sessionId);
        if (removed != null) {
            long duration = System.currentTimeMillis() - removed.getStartTime();
            log.info("移除对话会话: sessionId={}, 持续时间={}ms", sessionId, duration);
        }
    }

    /** 中断指定的对话会话
     * @param sessionId 会话ID
     * @return 是否成功中断（true表示会话存在且成功中断，false表示会话不存在） */
    public boolean interruptSession(String sessionId) {
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo == null) {
            log.warn("尝试中断不存在的会话: sessionId={}", sessionId);
            return false;
        }

        // 设置中断标志
        sessionInfo.setInterrupted();
        log.info("设置会话中断标志: sessionId={}", sessionId);

        // 先从活跃会话中移除，避免重复处理
        activeSessions.remove(sessionId);

        try {
            SseEmitter emitter = sessionInfo.getEmitter();

            // 直接尝试发送中断消息，如果连接已关闭会自动处理
            SseEmitterUtils.safeSend(emitter,
                    SseEmitter.event().name("interrupt").data("{\"interrupted\": true, \"message\": \"对话已被中断\"}"));

            // 安全完成SSE连接
            SseEmitterUtils.safeComplete(emitter);
            log.info("对话会话已中断: sessionId={}", sessionId);
            return true;

        } catch (Exception e) {
            log.error("中断会话时发生错误: sessionId={}, error={}", sessionId, e.getMessage());
            return true;
        }
    }
    /** 获取活跃会话Map（仅供Controller使用） */
    public ConcurrentHashMap<String, SessionInfo> getActiveSessions() {
        return activeSessions;
    }

}
