package org.joker.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.joker.agent.exception.BusinessException;
import org.joker.agent.model.AgentEntity;
import org.joker.agent.model.LLMModelConfig;
import org.joker.agent.model.SessionEntity;
import org.joker.agent.repository.AgentRepository;
import org.joker.agent.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ModelConfigResolver {

    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private AgentRepository agentRepository;

    /**
     * 获取嵌入模型配置
     *
     * @param sessionId sessionID
     * @return 嵌入模型配置
     * @throws BusinessException 如果用户未配置嵌入模型或配置无效
     */
    public LLMModelConfig getEmbeddingModelConfig(String sessionId) {
        try {
            SessionEntity sessionEntity = sessionRepository.selectById(sessionId);
            AgentEntity agentEntity = agentRepository.selectById(sessionEntity.getAgentId());
            LLMModelConfig embeddingModelConfig = agentEntity.getEmbeddingModelConfig();
            if (embeddingModelConfig == null) {
                throw new BusinessException(agentEntity.getId() + "未配置Embedding模型");
            }
            return embeddingModelConfig;
        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("sessionId %s 获取嵌入模型配置失败: %s", sessionId, e.getMessage());
            log.error(errorMsg, e);
            throw new BusinessException(errorMsg, e);
        }
    }

    /**
     * 获取聊天模型配置
     *
     * @param sessionId sessionID
     * @return 聊天模型配置
     */
    public LLMModelConfig getChatModelConfig(String sessionId) {
        try {
            SessionEntity sessionEntity = sessionRepository.selectById(sessionId);
            AgentEntity agentEntity = agentRepository.selectById(sessionEntity.getAgentId());
            return agentEntity.getAgentModelConfig();
        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("sessionId %s 获取嵌入模型配置失败: %s", sessionId, e.getMessage());
            log.error(errorMsg, e);
            throw new BusinessException(errorMsg, e);
        }
    }


}
