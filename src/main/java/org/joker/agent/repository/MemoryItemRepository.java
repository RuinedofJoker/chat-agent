package org.joker.agent.repository;

import org.joker.agent.model.MemoryItemEntity;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryItemRepository extends BaseRepository<String, MemoryItemEntity> {

    @Override
    protected String getKey(MemoryItemEntity memoryItemEntity) {
        return memoryItemEntity.getId();
    }

    @Override
    protected void setKey(String id, MemoryItemEntity memoryItemEntity) {
        memoryItemEntity.setId(id);
    }
}
