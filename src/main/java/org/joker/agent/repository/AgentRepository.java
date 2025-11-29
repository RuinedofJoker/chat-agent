package org.joker.agent.repository;

import org.joker.agent.model.AgentEntity;
import org.joker.agent.model.SessionEntity;
import org.springframework.stereotype.Repository;

@Repository
public class AgentRepository extends BaseRepository<String, AgentEntity> {

    @Override
    protected String getKey(AgentEntity agentEntity) {
        return agentEntity.getId();
    }

    @Override
    protected void setKey(String id, AgentEntity agentEntity) {
        agentEntity.setId(id);
    }
}
