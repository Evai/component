package com.evai.component.cache;

import com.evai.component.utils.BeanUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
@Slf4j
@AllArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return BeanUtil.stringToBean(this.get(key), clazz);
    }

    @Override
    public Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, timeUnit));
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    @Override
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    @Override
    public ScanCursor<String> scan(Long cursorId, String pattern) {
        Cursor<byte[]> cursor = redisTemplate.execute((RedisCallback<Cursor<byte[]>>) redisConnection -> redisConnection.scan(
                ScanOptions.scanOptions()
                        .count(cursorId == null ? 0L : cursorId)
                        .match(pattern)
                        .build()
        ));
        if (cursor == null || cursor.getCursorId() == 0L) {
            return ScanCursor.EMPTY;
        }

        Set<String> set = new HashSet<>();
        while (cursor.hasNext()) {
            set.add(new String(cursor.next()));
        }
        ScanCursor<String> scanCursor = new ScanCursor<>();
        scanCursor.setCursorId(cursor.getCursorId());
        scanCursor.setItems(set);
        return scanCursor;
    }

    @Override
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    @Override
    public Long unlink(Collection<String> keys) {
        return redisTemplate.unlink(keys);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.execute((RedisCallback<Boolean>) redisConnection -> redisConnection.exists(key.getBytes())));
    }

    @Override
    public Long size(String key) {
        return redisTemplate.opsForList().size(key);
    }

    @Override
    public Long rightPushAll(String key, String... values) {
        return redisTemplate.opsForList().rightPushAll(key, values);
    }

    @Override
    public String leftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public String leftPop(String key, long timeout, TimeUnit unit) {
        return redisTemplate.opsForList().leftPop(key, timeout, unit);
    }

    @Override
    public void multi(Runnable runnable) {
        SessionCallback sessionCallback = new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                redisOperations.multi();
                runnable.run();
                return redisOperations.exec();
            }
        };
        redisTemplate.execute(sessionCallback);
    }
}
