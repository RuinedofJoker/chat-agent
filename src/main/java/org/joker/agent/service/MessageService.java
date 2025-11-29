package org.joker.agent.service;

import org.apache.commons.lang3.StringUtils;
import org.joker.agent.model.MessageEntity;
import org.joker.agent.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    public boolean isFirstConversation(String sessionId) {
        return messageRepository
                .count((entity) -> StringUtils.equals(sessionId, entity.getSessionId())) <= 3;
    }

    public void saveMessage(List<MessageEntity> messageEntities) {
        messageEntities.forEach(messageEntity -> {
            messageEntity.setId(null);
            if (messageEntity.getCreatedAt() == null) {
                messageEntity.setCreatedAt(LocalDateTime.now());
            }
            messageRepository.insert(messageEntity);
        });
    }

    public void updateMessage(MessageEntity message) {
        messageRepository.updateById(message);
    }

}
