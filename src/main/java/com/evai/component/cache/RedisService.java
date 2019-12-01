package com.evai.component.cache;

import com.evai.component.utils.BeanUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author crh
 * @description
 * @date 2019-12-01
 */
public interface RedisService {

    String get(String key);

    void set(String key, String value, long seconds);

    <T> T get(String key, Class<T> clazz);

    Long getExpire(String key, TimeUnit timeUnit);

    boolean expire(String key, long timeout, TimeUnit timeUnit);

    boolean delete(String key);

    /**
     * 获取keys，指定 pattern
     *
     * @param pattern "keyName*"
     */
    @Deprecated
    Set<String> keys(String pattern);

    ScanCursor<String> scan(Long cursorId, String pattern);

    /**
     * 批量删除key，指定 key 集合
     *
     * @param keys
     */
    Long delete(Collection<String> keys);

    Long unlink(Collection<String> keys);

    boolean exists(String key);

    Long size(String key);

    Long rightPushAll(String key, String... values);

    String leftPop(String key);

    String leftPop(String key, long timeout, TimeUnit unit);

    /**
     * 批处理
     *
     * @param runnable
     */
    void multi(Runnable runnable);
}
