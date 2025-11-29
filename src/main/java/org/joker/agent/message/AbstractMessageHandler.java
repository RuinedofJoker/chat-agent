package org.joker.agent.message;

import cn.hutool.core.collection.CollectionUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joker.agent.context.AgentPromptTemplates;
import org.joker.agent.context.ChatContext;
import org.joker.agent.dto.AgentChatResponse;
import org.joker.agent.dto.MemoryResult;
import org.joker.agent.dto.ModelCallInfo;
import org.joker.agent.dto.ToolCallInfo;
import org.joker.agent.enums.ExecutionPhase;
import org.joker.agent.enums.MessageType;
import org.joker.agent.enums.Role;
import org.joker.agent.factory.LLMServiceFactory;
import org.joker.agent.model.AgentEntity;
import org.joker.agent.model.MessageEntity;
import org.joker.agent.repository.SessionRepository;
import org.joker.agent.service.MemoryExtractorService;
import org.joker.agent.service.MemoryService;
import org.joker.agent.service.MessageService;
import org.joker.agent.tool.BuiltInToolRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class AbstractMessageHandler {

    /**
     * 连接超时时间（毫秒）
     */
    protected static final long CONNECTION_TIMEOUT = 3000000L;

    @Autowired
    protected LLMServiceFactory llmServiceFactory;
    @Autowired
    protected MessageService messageService;
    @Autowired
    protected BuiltInToolRegistry builtInToolRegistry;
    @Autowired
    protected MemoryExtractorService memoryExtractorService;
    @Autowired
    protected MemoryService memoryService;
    @Autowired
    protected SessionRepository sessionRepository;
    // 无需事件或单独服务，直接调用异步方法
    // 记忆注入常量（默认开启）
    private static final String MEMORY_SECTION_TITLE = "[记忆要点]";
    private static final int MEMORY_TOP_K = 5;


    /**
     * 处理对话的模板方法
     *
     * @param chatContext 对话环境
     * @param transport   消息传输实现
     * @param <T>         连接类型
     * @return 连接对象
     */
    public <T> T chat(ChatContext chatContext, MessageTransport<T> transport) {
        // 创建连接
        T connection = transport.createConnection(CONNECTION_TIMEOUT);

        // 调用对话开始钩子
        onChatStart(chatContext);

        // 创建消息实体
        MessageEntity llmMessageEntity = createLlmMessage(chatContext);
        MessageEntity userMessageEntity = createUserMessage(chatContext);

        // 调用用户消息处理完成钩子
        onUserMessageProcessed(chatContext, userMessageEntity);

        // 初始化聊天内存
        MessageWindowChatMemory memory = initMemory();

        // 构建历史消息
        buildHistoryMessage(chatContext, memory);

        // 根据子类决定是否需要工具
        ToolProvider toolProvider = provideTools(chatContext);

        // 流式流程
        processStreamingChat(chatContext, connection, transport, userMessageEntity, llmMessageEntity, memory,
                toolProvider);

        return connection;
    }

    /**
     * 追踪钩子方法 - 对话开始时调用 子类可以覆盖此方法实现追踪逻辑
     *
     * @param chatContext 对话上下文
     */
    protected void onChatStart(ChatContext chatContext) {
        // 默认空实现，子类可选择性覆盖
    }

    /**
     * 追踪钩子方法 - 用户消息处理完成时调用
     *
     * @param chatContext 对话上下文
     * @param userMessage 用户消息实体
     */
    protected void onUserMessageProcessed(ChatContext chatContext, MessageEntity userMessage) {
        // 默认空实现，子类可选择性覆盖
    }

    /**
     * 追踪钩子方法 - 模型调用完成时调用
     *
     * @param chatContext   对话上下文
     * @param chatResponse  模型响应
     * @param modelCallInfo 模型调用信息
     */
    protected void onModelCallCompleted(ChatContext chatContext, ChatResponse chatResponse,
                                        ModelCallInfo modelCallInfo) {
        // 默认空实现，子类可选择性覆盖
    }

    /**
     * 追踪钩子方法 - 工具调用完成时调用
     *
     * @param chatContext  对话上下文
     * @param toolCallInfo 工具调用信息
     */
    protected void onToolCallCompleted(ChatContext chatContext, ToolCallInfo toolCallInfo) {
        // 默认空实现，子类可选择性覆盖
    }

    /**
     * 追踪钩子方法 - 对话完成时调用
     *
     * @param chatContext  对话上下文
     * @param success      是否成功
     * @param errorMessage 错误信息（成功时为null）
     */
    protected void onChatCompleted(ChatContext chatContext, boolean success, String errorMessage) {
        // 对话完成钩子：成功时进行记忆抽取（异步）；RAG/公开访问跳过
        if (!success || chatContext == null)
            return;

        String sessionId = chatContext.getSessionId();
        String userText = StringUtils.defaultString(chatContext.getUserMessage(), "").trim();
        if (StringUtils.isBlank(sessionId) || StringUtils.isBlank(userText))
            return;

        // 直接调用异步方法，避免阻塞主流程
        try {
            memoryExtractorService.extractAndPersistAsync(sessionId, userText);
        } catch (Exception ignore) {
            // 异步任务调度异常不影响主流程
        }
    }

    /**
     * 追踪钩子方法 - 发生异常时调用
     *
     * @param chatContext 对话上下文
     * @param errorPhase  错误阶段
     * @param throwable   异常信息
     */
    protected void onChatError(ChatContext chatContext, ExecutionPhase errorPhase, Throwable throwable) {
        // 默认空实现，子类可选择性覆盖
    }

    /**
     * 子类可以覆盖这个方法提供工具
     */
    protected ToolProvider provideTools(ChatContext chatContext) {
        return null; // 默认不提供工具
    }

    /**
     * 流式聊天处理
     */
    protected <T> void processStreamingChat(ChatContext chatContext, T connection, MessageTransport<T> transport,
                                            MessageEntity userEntity, MessageEntity llmEntity, MessageWindowChatMemory memory,
                                            ToolProvider toolProvider) {

        // 获取流式LLM客户端
        StreamingChatModel streamingClient = llmServiceFactory.getStreamingClient(chatContext.getLlmModelConfig());

        // 创建流式Agent
        Agent agent = buildStreamingAgent(streamingClient, memory, toolProvider, chatContext.getAgent());

        // 使用现有的流式处理逻辑
        processChat(agent, connection, transport, chatContext, userEntity, llmEntity);
    }

    /**
     * 保存用户、摘要消息记录和更新活跃消息
     *
     * @param chatContext 对话环境
     * @param userEntity  此次的用户消息
     */
    private void saveMessageAndUpdateContext(ChatContext chatContext, MessageEntity userEntity) {
        MessageEntity summary = this.getSummaryFromHistory(chatContext.getMessageHistory());
        if (summary != null) {
            // 不重置 created_at 字段
            messageService.saveMessage(Collections.singletonList(summary));
        }
    }

    /**
     * 子类实现具体的聊天处理逻辑
     */
    protected <T> void processChat(Agent agent, T connection, MessageTransport<T> transport, ChatContext chatContext,
                                   MessageEntity userEntity, MessageEntity llmEntity) {

        // 保存用户消息和摘要
        this.saveMessageAndUpdateContext(chatContext, userEntity);

        AtomicReference<StringBuilder> messageBuilder = new AtomicReference<>(new StringBuilder());
        TokenStream tokenStream = agent.chat(chatContext.getUserMessage());

        // 记录调用开始时间
        long startTime = System.currentTimeMillis();

        tokenStream.onError(throwable -> {
            // 直接发送错误消息，transport内部处理连接异常
            transport.sendMessage(connection,
                    AgentChatResponse.buildEndMessage(throwable.getMessage(), MessageType.TEXT));

            // 调用错误处理钩子
            onChatError(chatContext, ExecutionPhase.MODEL_CALL, throwable);
            onChatCompleted(chatContext, false, throwable.getMessage());
        });

        // 部分响应处理
        tokenStream.onPartialResponse(reply -> {
            messageBuilder.get().append(reply);
            // 删除换行后消息为空字符串
            if (messageBuilder.get().toString().trim().isEmpty()) {
                return;
            }

            // 直接发送消息，transport内部处理连接异常
            transport.sendMessage(connection, AgentChatResponse.build(reply, MessageType.TEXT));
        });

        // 完整响应处理
        tokenStream.onCompleteResponse(chatResponse -> {

            this.setMessageTokenCount(chatContext.getMessageHistory(), userEntity, llmEntity, chatResponse);

            // 按仅用户抽取策略，不记录AI文本

            messageService.updateMessage(userEntity);
            // 保存AI消息
            messageService.saveMessage(Collections.singletonList(llmEntity));

            // 发送结束消息
            transport.sendEndMessage(connection, AgentChatResponse.buildEndMessage(MessageType.TEXT));

            // 调用模型调用完成钩子
            long latency = System.currentTimeMillis() - startTime;
            ModelCallInfo modelCallInfo = buildModelCallInfo(chatContext, chatResponse, latency, true);
            onModelCallCompleted(chatContext, chatResponse, modelCallInfo);

            // 调用对话完成钩子
            onChatCompleted(chatContext, true, null);

            smartRenameSession(chatContext);
        });

        // 错误处理
        // tokenStream.onError(throwable -> handleError(
        // connection, transport, chatContext,
        // messageBuilder.toString(), llmEntity, throwable));

        // 工具执行处理
        tokenStream.onToolExecuted(toolExecution -> {
            if (!messageBuilder.get().isEmpty()) {
                transport.sendMessage(connection, AgentChatResponse.buildEndMessage(MessageType.TEXT));
                llmEntity.setContent(messageBuilder.get().toString());
                messageService.saveMessage(Collections.singletonList(llmEntity));
                messageBuilder.set(new StringBuilder());
            }
            String message = "执行工具：" + toolExecution.request().name();
            MessageEntity toolMessage = createLlmMessage(chatContext);
            toolMessage.setMessageType(MessageType.TOOL_CALL);
            toolMessage.setContent(message);
            messageService.saveMessage(Collections.singletonList(toolMessage));

            // 直接发送工具调用消息
            transport.sendMessage(connection, AgentChatResponse.buildEndMessage(message, MessageType.TOOL_CALL));

            // 调用工具调用完成钩子
            ToolCallInfo toolCallInfo = buildToolCallInfo(toolExecution);
            onToolCallCompleted(chatContext, toolCallInfo);
        });

        // 启动流处理
        tokenStream.start();
    }

    @Nullable
    private MessageEntity getSummaryFromHistory(List<MessageEntity> historyMessages) {
        // List<MessageEntity> list = historyMessages.stream().filter(MessageEntity::isSummaryMessage).toList();
        if (historyMessages.isEmpty()) {
            return null;
        }
        return historyMessages.getFirst().isSummaryMessage() ? historyMessages.getFirst() : null;
    }

    /**
     * 根据历史消息的本体token算出本次消息的本体token
     *
     * @param historyMessages 历史消息列表
     * @param userEntity      用户请求消息实体
     * @param llmEntity       llm回复消息实体
     * @param chatResponse    llm响应
     */
    private void setMessageTokenCount(List<MessageEntity> historyMessages, MessageEntity userEntity,
                                      MessageEntity llmEntity, ChatResponse chatResponse) {
        llmEntity.setTokenCount(chatResponse.tokenUsage().outputTokenCount());
        llmEntity.setBodyTokenCount(chatResponse.tokenUsage().outputTokenCount());
        llmEntity.setContent(chatResponse.aiMessage().text());
        int bodyTokenSum = 0;
        if (CollectionUtil.isNotEmpty(historyMessages)) {
            bodyTokenSum = historyMessages.stream().mapToInt(MessageEntity::getBodyTokenCount).sum();
        }
        userEntity.setTokenCount(chatResponse.tokenUsage().inputTokenCount());
        userEntity.setBodyTokenCount(chatResponse.tokenUsage().inputTokenCount() - bodyTokenSum);
    }

    /**
     * 初始化内存
     */
    protected MessageWindowChatMemory initMemory() {
        return MessageWindowChatMemory.builder().maxMessages(1000).chatMemoryStore(new InMemoryChatMemoryStore())
                .build();
    }

    /**
     * 构建流式Agent
     */
    protected Agent buildStreamingAgent(StreamingChatModel model, MessageWindowChatMemory memory,
                                        ToolProvider toolProvider, AgentEntity agent) {

        // 通过内置工具注册器获取所有适用的内置工具
        Map<ToolSpecification, ToolExecutor> builtInTools = builtInToolRegistry.createToolsForAgent(agent);

        AiServices<Agent> agentService = AiServices.builder(Agent.class).streamingChatModel(model).chatMemory(memory);

        // 添加内置工具（如RAG等）
        if (builtInTools != null && !builtInTools.isEmpty()) {
            agentService.tools(builtInTools);
        }

        // 添加外部工具提供者
        if (toolProvider != null) {
            agentService.toolProvider(toolProvider);
        }

        return agentService.build();
    }

    /**
     * 创建用户消息实体
     */
    protected MessageEntity createUserMessage(ChatContext environment) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setRole(Role.USER);
        messageEntity.setContent(environment.getUserMessage());
        messageEntity.setSessionId(environment.getSessionId());
        messageEntity.setFileUrls(environment.getFileUrls());
        return messageEntity;
    }

    /**
     * 创建LLM消息实体
     */
    protected MessageEntity createLlmMessage(ChatContext environment) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setRole(Role.ASSISTANT);
        messageEntity.setSessionId(environment.getSessionId());
        messageEntity.setModel(environment.getLlmModelConfig().getModelId());
        return messageEntity;
    }

    /**
     * 构建历史消息到内存中
     */
    protected void buildHistoryMessage(ChatContext chatContext, MessageWindowChatMemory memory) {
        String summary = Optional.ofNullable(this.getSummaryFromHistory(chatContext.getMessageHistory()))
                .map(MessageEntity::getContent).orElse("");
        if (StringUtils.isNotEmpty(summary)) {
            // 添加为AI消息，但明确标识这是摘要
            memory.add(new AiMessage(summary));
        }

        String presetToolPrompt = "";
        // 设置预先工具设置的参数到系统提示词中
        Map<String, Map<String, Map<String, String>>> toolPresetParams = chatContext.getAgent().getToolPresetParams();
        if (toolPresetParams != null) {
            presetToolPrompt = AgentPromptTemplates.generatePresetToolPrompt(toolPresetParams);
        }

        // 读取长期记忆，组装为要点，直接合入系统提示词尾部
        String memorySection = buildMemorySection(chatContext);
        String fullSystemPrompt = chatContext.getAgent().getSystemPrompt() + "\n" + presetToolPrompt
                + (memorySection.isEmpty() ? "" : ("\n" + memorySection));

        memory.add(new SystemMessage(fullSystemPrompt));
        List<MessageEntity> messageHistory = chatContext.getMessageHistory();
        for (MessageEntity messageEntity : messageHistory) {
            // 注意不要重复发送摘要消息
            if (messageEntity.isUserMessage()) {
                List<String> fileUrls = messageEntity.getFileUrls();
                for (String fileUrl : fileUrls) {
                    memory.add(UserMessage.from(ImageContent.from(fileUrl)));
                }
                if (!StringUtils.isEmpty(messageEntity.getContent())) {
                    memory.add(new UserMessage(messageEntity.getContent()));
                }
            } else if (messageEntity.isAIMessage()) {
                memory.add(new AiMessage(messageEntity.getContent()));
            } else if (messageEntity.isSystemMessage()) {
                memory.add(new SystemMessage(messageEntity.getContent()));
            }
        }
    }

    /**
     * 构造“记忆要点”片段，合入系统提示词尾部
     */
    private String buildMemorySection(ChatContext chatContext) {
        try {
            int topK = MEMORY_TOP_K;
            String title = MEMORY_SECTION_TITLE;
            // 必须有用户消息和 sessionId 才进行召回
            if (chatContext == null || !StringUtils.isNotBlank(chatContext.getSessionId())
                    || !StringUtils.isNotBlank(chatContext.getUserMessage())) {
                return "";
            }
            var results = memoryService.searchRelevant(chatContext.getSessionId(), chatContext.getUserMessage(),
                    topK);
            if (results == null || results.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(title).append('\n');
            int idx = 0;
            for (MemoryResult r : results) {
                if (r == null || r.getText() == null)
                    continue;
                String text = r.getText().replaceAll("\n+", " ");
                sb.append("- [").append(r.getType() != null ? r.getType().name() : "FACT").append("] ").append(text)
                        .append("\n");
                if (++idx >= topK)
                    break;
            }
            return sb.toString();
        } catch (Exception e) {
            // 召回异常不影响主流程
            return "";
        }
    }

    // 智能重命名会话
    protected void smartRenameSession(ChatContext chatContext) {
        Thread thread = new Thread(() -> {
            // 获取会话 id
            String sessionId = chatContext.getSessionId();
            // 是否是首次对话
            boolean isFirstConversation = messageService.isFirstConversation(sessionId);
            // 如果首次对话，则重命名会话
            if (isFirstConversation) {
                ChatModel strandClient = llmServiceFactory.getStrandClient(chatContext.getLlmModelConfig());
                ArrayList<ChatMessage> chatMessages = new ArrayList<>();
                chatMessages.add(new SystemMessage(AgentPromptTemplates.getStartConversationPrompt()));
                chatMessages.add(new UserMessage(chatContext.getUserMessage()));
                ChatResponse chat = strandClient.chat(chatMessages);
                String sessionTitle = chat.aiMessage().text();
                sessionRepository.updateSession(chatContext.getSessionId(), sessionTitle);
            }
        });
        thread.start();
    }

    /**
     * 生成幂等性请求ID
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 请求ID
     */
    private String generateRequestId(String sessionId, String userId) {
        long timestamp = System.currentTimeMillis();
        return String.format("billing_%s_%s_%d", sessionId, userId, timestamp);
    }

    /**
     * 构建模型调用信息
     *
     * @param chatContext  对话上下文
     * @param chatResponse 模型响应
     * @param callTime     调用耗时（毫秒）
     * @param success      是否成功
     * @return 模型调用信息
     */
    protected ModelCallInfo buildModelCallInfo(ChatContext chatContext, ChatResponse chatResponse, long callTime,
                                               boolean success) {
        return ModelCallInfo.builder().modelEndpoint(chatContext.getLlmModelConfig().getModelEndpoint())
                .inputTokens(chatResponse.tokenUsage().inputTokenCount())
                .outputTokens(chatResponse.tokenUsage().outputTokenCount()).callTime((int) callTime).success(success)
                .build();
    }

    /**
     * 构建工具调用信息
     *
     * @param toolExecution 工具执行信息
     * @return 工具调用信息
     */
    protected ToolCallInfo buildToolCallInfo(ToolExecution toolExecution) {
        return ToolCallInfo.builder().toolName(toolExecution.request().name())
                .requestArgs(toolExecution.request().arguments()).responseData(toolExecution.result()).success(true) // 此时表示工具执行成功
                .build();
    }
}
