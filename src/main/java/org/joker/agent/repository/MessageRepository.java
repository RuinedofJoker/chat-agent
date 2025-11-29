package org.joker.agent.repository;

import org.joker.agent.model.MessageEntity;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository extends BaseRepository<String, MessageEntity> {
    @Override
    protected String getKey(MessageEntity messageEntity) {
        return messageEntity.getId();
    }

    @Override
    protected void setKey(String id, MessageEntity messageEntity) {
        messageEntity.setId(id);
    }
}
