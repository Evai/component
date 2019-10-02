package com.evai.component.cache;

import com.evai.component.utils.BeanUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
@Component
@Slf4j
@AllArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;


    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void set(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    public <T> T get(String key, Class<T> clazz) {
        return BeanUtil.stringToBean(this.get(key), clazz);
    }

    public Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, timeUnit));
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 获取keys，指定 pattern
     *
     * @param pattern "keyName*"
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 批量删除key，指定 key 集合
     *
     * @param keys
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.execute((RedisCallback<Boolean>) redisConnection -> redisConnection.exists(key.getBytes())));
    }

    public Long size(String key) {
        return redisTemplate.opsForList().size(key);
    }

    public Long rightPushAll(String key, String... values) {
        return redisTemplate.opsForList().rightPushAll(key, values);
    }

    public String leftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    public String leftPop(String key, long timeout, TimeUnit unit) {
        return redisTemplate.opsForList().leftPop(key, timeout, unit);
    }

}
