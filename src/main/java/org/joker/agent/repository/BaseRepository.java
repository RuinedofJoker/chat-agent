package org.joker.agent.repository;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class BaseRepository<Key, Entity> {

    private final Map<Key, Entity> entityMap = new ConcurrentHashMap<>();

    protected abstract Key getKey(Entity entity);

    protected abstract void setKey(Key id, Entity entity);

    protected void setDefaultKey(Entity entity) {
        try {
            Method setKeyMethod = this.getClass()
                    .getDeclaredMethod("setKey", Object.class, Object.class);

            Class<?> keyType = setKeyMethod.getParameterTypes()[0];

            // 判断 Key 是否为 String 类型
            if (String.class.equals(keyType)) {
                String newId = java.util.UUID.randomUUID().toString();
                @SuppressWarnings("unchecked")
                Key key = (Key) newId;
                setKey(key, entity);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set default key", e);
        }
    }

    protected Entity get(Key id) {
        return entityMap.get(id);
    }

    protected void put(Key id, Entity entity) {
        entityMap.put(id, entity);
    }

    protected void clear() {
        entityMap.clear();
    }

    public Entity selectOne(Predicate<Entity> condition) {
        List<Entity> dataList = selectList(condition);
        if (dataList.size() > 1) {
            throw new IllegalStateException();
        }
        if (dataList.isEmpty()) {
            return null;
        }
        return dataList.getFirst();
    }

    public List<Entity> selectList(Predicate<Entity> condition) {
        return entityMap.values().stream().filter(condition).collect(Collectors.toList());
    }

    public Entity selectById(Key id) {
        return get(id);
    }

    public void insert(Entity entity) {
        if (getKey(entity) == null) {
            setDefaultKey(entity);
        }
        put(getKey(entity), entity);
    }

    public int updateById(Entity update) {
        Key id = getKey(update);
        Entity entity = selectById(id);
        if (entity != null) {
            put(id, update);
            return 1;
        }
        return 0;
    }

    public int updateById(Key id, Consumer<Entity> update) {
        Entity entity = selectById(id);
        if (entity != null) {
            update.accept(entity);
            return 1;
        }
        return 0;
    }

    public int update(Predicate<Entity> condition, Consumer<Entity> update) {
        List<Entity> list = selectList(condition);
        list.forEach(update);
        return list.size();
    }

    public int delete(Predicate<Entity> condition) {
        AtomicInteger count = new AtomicInteger(0);
        entityMap.values().removeIf(entity -> {
            if (condition.test(entity)) {
                count.incrementAndGet();
                return true;
            }
            return false;
        });
        return count.get();
    }

    public long count(Predicate<Entity> condition) {
        return entityMap.entrySet().stream().filter(entry -> condition.test(entry.getValue())).count();
    }

}
