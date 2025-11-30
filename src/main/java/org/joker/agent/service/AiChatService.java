package org.joker.agent.service;

import cn.hutool.core.bean.BeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.joker.agent.context.ChatContext;
import org.joker.agent.dto.ChatRequest;
import org.joker.agent.dto.NewSessionDTO;
import org.joker.agent.dto.TokenOverflowConfig;
import org.joker.agent.dto.TokenProcessResult;
import org.joker.agent.enums.MessageType;
import org.joker.agent.enums.Role;
import org.joker.agent.enums.TokenOverflowStrategyEnum;
import org.joker.agent.factory.MessageHandlerFactory;
import org.joker.agent.factory.MessageTransportFactory;
import org.joker.agent.message.AbstractMessageHandler;
import org.joker.agent.message.MessageTransport;
import org.joker.agent.model.*;
import org.joker.agent.repository.AgentRepository;
import org.joker.agent.repository.MessageRepository;
import org.joker.agent.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private MessageTransportFactory transportFactory;
    @Autowired
    private MessageHandlerFactory messageHandlerFactory;
    @Autowired
    private ChatSessionManager chatSessionManager;
    @Autowired
    private TokenMessageService tokenMessageService;

    public SseEmitter chat(ChatRequest chatRequest) {
        // 获取聊天上下文
        ChatContext chatContext = createChatContext(chatRequest);

        // 初始化上下文
        setupContextAndHistory(chatContext, chatRequest);

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

    /**
     * 设置上下文和历史消息
     *
     * @param environment 对话环境
     */
    private void setupContextAndHistory(ChatContext environment, ChatRequest chatRequest) {
        String sessionId = environment.getSessionId();

        // 获取活跃消息(包括摘要)
        List<MessageEntity> messageEntities = messageRepository.selectList(messageEntity -> StringUtils.equals(sessionId, messageEntity.getSessionId()) && Boolean.TRUE.equals(messageEntity.getIsActive()));
        messageEntities.sort(Comparator.comparing(MessageEntity::getCreatedAt));

        // 应用Token溢出策略, 上下文历史消息以token策略返回的为准
        messageEntities = applyTokenOverflowStrategy(environment, messageEntities);

        // 特殊处理当前对话的文件，因为在后续的对话中无法发送文件
        List<String> fileUrls = chatRequest.getFileUrls();
        if (!fileUrls.isEmpty()) {
            MessageEntity messageEntity = new MessageEntity();
            messageEntity.setRole(Role.USER);
            messageEntity.setFileUrls(fileUrls);
            messageEntities.add(messageEntity);
        }

        environment.setMessageHistory(messageEntities);
    }

    /**
     * 应用Token溢出策略，返回处理后的历史消息
     *
     * @param environment     对话环境
     * @param messageEntities 消息实体列表
     */
    private List<MessageEntity> applyTokenOverflowStrategy(ChatContext environment, List<MessageEntity> messageEntities) {

        LLMModelConfig llmModelConfig = environment.getLlmModelConfig();

        // 处理Token溢出
        TokenOverflowStrategyEnum strategyType = llmModelConfig.getStrategyType();

        // Token处理
        List<TokenMessage> tokenMessages = messageEntities.stream().map(message -> {
            TokenMessage tokenMessage = new TokenMessage();
            tokenMessage.setId(message.getId());
            tokenMessage.setRole(message.getRole().name());
            tokenMessage.setContent(message.getContent());
            tokenMessage.setTokenCount(message.getTokenCount());
            tokenMessage.setBodyTokenCount(message.getBodyTokenCount());
            tokenMessage.setCreatedAt(message.getCreatedAt());
            return tokenMessage;
        }).collect(Collectors.toList());
        ;

        // 构造Token配置
        TokenOverflowConfig tokenOverflowConfig = new TokenOverflowConfig();
        tokenOverflowConfig.setStrategyType(strategyType);
        tokenOverflowConfig.setMaxTokens(llmModelConfig.getMaxTokens());
        tokenOverflowConfig.setSummaryThreshold(llmModelConfig.getSummaryThreshold());
        tokenOverflowConfig.setReserveRatio(llmModelConfig.getReserveRatio());

        // 设置提供商配置
        tokenOverflowConfig.setProviderConfig(new ProviderConfig(llmModelConfig.getApiKey(),
                llmModelConfig.getBaseUrl(), llmModelConfig.getModelId(), llmModelConfig.getProtocol()));

        // 处理Token
        TokenProcessResult result = tokenMessageService.processMessages(tokenMessages, tokenOverflowConfig);
        List<TokenMessage> retainedMessages = new ArrayList<>(tokenMessages);
        TokenMessage newSummaryMessage = null;
        // 更新上下文
        if (result.isProcessed()) {
            retainedMessages = result.getRetainedMessages();
            // 统一对 活跃消息进行时间升序排序
            List<String> retainedMessageIds = retainedMessages.stream()
                    .sorted(Comparator.comparing(TokenMessage::getCreatedAt)).map(TokenMessage::getId)
                    .toList();
            if (strategyType == TokenOverflowStrategyEnum.SUMMARIZE
                    && !retainedMessages.isEmpty() && retainedMessages.getFirst().getRole().equals(Role.SUMMARY.name())) {
                newSummaryMessage = retainedMessages.getFirst();
            }

            messageRepository.update(messageEntity -> retainedMessageIds.contains(messageEntity.getId()), messageEntity -> messageEntity.setIsActive(false));
        }
        Set<String> retainedMessageIdSet = retainedMessages.stream().map(TokenMessage::getId)
                .collect(Collectors.toSet());
        // 从messageEntity中过滤出保留的消息，防止Entity字段丢失
        List<MessageEntity> newHistoryMessages = messageEntities.stream()
                .filter(message -> retainedMessageIdSet.contains(message.getId()) && !message.isSummaryMessage())
                .collect(Collectors.toList());
        if (newSummaryMessage != null) {
            newHistoryMessages.addFirst(summaryMessageToEntity(newSummaryMessage, environment.getSessionId()));
        }
        return newHistoryMessages;
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
        agentEntity.setSystemPrompt(session.getSystemPrompt());
        agentEntity.setMultiModal(session.getMultiModal());
        agentEntity.setAgentModelConfig(session.getAgentModelConfig());
        agentEntity.setEmbeddingModelConfig(session.getEmbeddingModelConfig());

        agentRepository.insert(agentEntity);
        sessionRepository.insert(sessionEntity);

        return sessionEntity;
    }

    public List<SessionEntity> queryAllSessions() {
        return sessionRepository.selectList(sessionEntity -> true).stream().sorted((d1, d2) -> d2.getCreateTime().compareTo(d1.getCreateTime())).toList();
    }

    public List<MessageEntity> queryHistoryMessages(String sessionId) {
        List<MessageEntity> messageEntities = messageRepository.selectList(messageEntity -> StringUtils.equals(sessionId, messageEntity.getSessionId()) && Boolean.TRUE.equals(messageEntity.getIsActive()));
        messageEntities.sort(Comparator.comparing(MessageEntity::getCreatedAt));
        return messageEntities;
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

    private MessageEntity summaryMessageToEntity(TokenMessage tokenMessage, String sessionId) {
        MessageEntity messageEntity = new MessageEntity();
        BeanUtil.copyProperties(tokenMessage, messageEntity);
        messageEntity.setRole(Role.fromCode(tokenMessage.getRole()));
        messageEntity.setSessionId(sessionId);
        messageEntity.setMessageType(MessageType.TEXT);
        return messageEntity;
    }

}
