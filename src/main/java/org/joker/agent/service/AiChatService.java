package org.joker.agent.service;

import org.apache.commons.lang3.StringUtils;
import org.joker.agent.context.ChatContext;
import org.joker.agent.dto.ChatRequest;
import org.joker.agent.dto.NewSessionDTO;
import org.joker.agent.factory.MessageHandlerFactory;
import org.joker.agent.factory.MessageTransportFactory;
import org.joker.agent.message.AbstractMessageHandler;
import org.joker.agent.message.MessageTransport;
import org.joker.agent.model.AgentEntity;
import org.joker.agent.model.SessionEntity;
import org.joker.agent.repository.AgentRepository;
import org.joker.agent.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private MessageTransportFactory transportFactory;
    @Autowired
    private MessageHandlerFactory messageHandlerFactory;
    @Autowired
    private ChatSessionManager chatSessionManager;

    public SseEmitter chat(ChatRequest chatRequest) {
        // 获取聊天上下文
        ChatContext chatContext = createChatContext(chatRequest);

        // 获取输方式（协议扩展点）
        MessageTransport<SseEmitter> transport = transportFactory
                .getTransport(MessageTransportFactory.TRANSPORT_TYPE_SSE);

        // 根据请求类型获取适合的消息处理器
        AbstractMessageHandler handler = messageHandlerFactory.getHandler(chatRequest);

        // 处理对话
        SseEmitter emitter = handler.chat(chatContext, transport);

        // 注册会话到会话管理器（支持中断功能）
        chatSessionManager.registerSession(chatRequest.getSessionId(), emitter);

        return emitter;
    }

    public SessionEntity createSession(NewSessionDTO session) {
        String sessionId = "session-" + UUID.randomUUID();

        SessionEntity sessionEntity = new SessionEntity();
        sessionEntity.setId(sessionId);
        if (StringUtils.isBlank(session.getTitle())) {
            sessionEntity.setTitle("新的聊天");
        }
        sessionEntity.setCreateTime(new Date());

        AgentEntity agentEntity = new AgentEntity();
        agentEntity.setId("agent-" + UUID.randomUUID());
        sessionEntity.setAgentId(agentEntity.getId());

        agentRepository.insert(agentEntity);
        sessionRepository.insert(sessionEntity);

        return sessionEntity;
    }

    public List<SessionEntity> queryAllSessions() {
        return sessionRepository.selectList(sessionEntity -> true).stream().sorted((d1, d2) -> d2.getCreateTime().compareTo(d1.getCreateTime())).toList();
    }

    private ChatContext createChatContext(ChatRequest chatRequest) {
        String sessionId = chatRequest.getSessionId();
        SessionEntity session = sessionRepository.selectById(sessionId);
        String agentId = session.getAgentId();
        AgentEntity agent = agentRepository.selectById(agentId);
        ChatContext chatContext = new ChatContext();
        chatContext.setSessionId(chatRequest.getSessionId());
        chatContext.setUserMessage(chatRequest.getMessage());
        chatContext.setAgent(agent);
        chatContext.setLlmModelConfig(agent.getAgentModelConfig());
        return chatContext;
    }

}
