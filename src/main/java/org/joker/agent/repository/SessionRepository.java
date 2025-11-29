package org.joker.agent.repository;

import org.joker.agent.model.SessionEntity;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRepository extends BaseRepository<String, SessionEntity> {

    @Override
    protected String getKey(SessionEntity sessionEntity) {
        return sessionEntity.getId();
    }

    @Override
    protected void setKey(String id, SessionEntity sessionEntity) {
        sessionEntity.setId(id);
    }

    public int updateSession(String sessionId, String title) {
        return updateById(sessionId, (sessionEntity -> sessionEntity.setTitle(title)));
    }
}
