package com.evai.component.cache;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evai.component.cache.annotation.CacheAbleEntity;
import com.evai.component.cache.enums.KeyFormat;
import com.evai.component.cache.exception.GetLockFailedException;
import com.evai.component.cache.exception.IllegalParamException;
import com.evai.component.cache.lock.RedisLock;
import com.evai.component.cache.utils.CacheKeyUtil;
import com.evai.component.mybatis.BaseEntity;
import com.evai.component.mybatis.utils.ReflectUtil;
import com.evai.component.utils.BeanUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author crh
 * @date 2019-09-23
 * @description
 */
@Slf4j
@Component
@AllArgsConstructor
public class CacheComponent {

    private final RedisService redisService;
    private final CacheKeyConfig cacheKeyConfig;
    private final RedisTemplate<String, String> redisTemplate;
    private final CacheKeyUtil cacheKeyUtil;
    private final RedisLock redisLock;
    /**
     * 异步更新缓存线程池
     */
    private final ExecutorService cacheExecutor;

    /**
     * 自增锁次数并设置过期时间
     *
     * @param key
     * @param step
     * @param seconds
     * @return
     */
    public Long incrByEx(String key, long step, long seconds) {
        return redisTemplate.execute((RedisCallback<Long>) redisConnection ->
                redisConnection.eval(CacheConstant.LuaScript.INCRBY_EXPIRE.getBytes(), ReturnType.INTEGER, 2, key.getBytes(), StringUtils.EMPTY.getBytes(), String.valueOf(step).getBytes(), String.valueOf(seconds).getBytes()));
    }

    /**
     * 自减锁次数，如果小于等于0释放锁
     *
     * @param key
     * @param step
     * @return
     */
    public Long decrByRelease(String key, long step) {
        return redisTemplate.execute((RedisCallback<Long>) redisConnection ->
                redisConnection.eval(CacheConstant.LuaScript.DECRBY_RELEASE.getBytes(), ReturnType.INTEGER, 1, key.getBytes(), String.valueOf(step).getBytes()));
    }

    /**
     * 设置 string 并设置过期时间
     *
     * @param writeKey 锁key
     * @param key      存入数据key
     * @param value    存入数据值
     * @param seconds  过期时间
     * @param <T>
     */
    public <T> Boolean setExWithNotExist(String writeKey, String key, T value, long seconds) {
        String result = BeanUtil.beanToString(value);
        return redisTemplate.execute((RedisCallback<Boolean>) redisConnection ->
                redisConnection.eval(CacheConstant.LuaScript.SET_WITH_NOT_EXIST.getBytes(), ReturnType.BOOLEAN, 2, writeKey.getBytes(), key.getBytes(), String.valueOf(seconds).getBytes(), result.getBytes()));
    }

    /**
     * 新增写锁
     *
     * @param key
     * @param seconds
     * @return
     */
    public Long tryWriteLock(String key, long seconds) {
        return this.incrByEx(key, 1, seconds);
    }

    /**
     * 释放写锁
     *
     * @param key
     * @return
     */
    public Long releaseWriteLock(String key) {
        return this.decrByRelease(key, 1);
    }


    /**
     * 查询缓存，如果key对应的value不存在，存储新的值，并设置过期时间，同步返回最新值
     * 同时检查该缓存是否即将到期，并延长过期时间，防止缓存穿透
     *
     * @param key
     * @param javaType
     * @param seconds
     * @param supplier
     * @param <T>
     * @return
     */
    public <T> T getCache(String key, int seconds, int asyncSeconds, JavaType javaType, Supplier<T> supplier) {
        String value = redisService.get(key);
        // 缓存值不存在或已失效
        if (value == null) {
            T result = supplier.get();
            redisService.set(key, BeanUtil.beanToString(result), seconds);
            return result;
        } else {
            // 如果到期时间 < 设置的到期时间，更新缓存数据，防止缓存穿透
            Long expired = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (expired == null || expired < asyncSeconds) {
                // 异步更新方法
                Runnable runnable = () -> {
                    T result = supplier.get();
                    redisService.set(key, BeanUtil.beanToString(result), seconds);
                };
                asyncUpdateCache(key, runnable);
            }
            // 说明数据库没有该值，无需重复查询数据库
            if (StringUtils.equals(value, CacheConstant.NULL)) {
                return null;
            }
            return BeanUtil.stringToBean(value, javaType);
        }
    }

    /**
     * 查询实体类缓存，如果key对应的value不存在，存储新的值，并设置过期时间，同步返回最新值
     * 同时检查该缓存是否即将到期，并延长过期时间，防止缓存穿透
     *
     * @param clazz
     * @param seconds
     * @param supplier
     * @param <T>
     * @return
     */
    public <T> T getEntityCache(CacheKeyDTO cacheKeyDTO, int seconds, int asyncSeconds, Class<T> clazz, CacheAbleEntity cacheAbleEntity, Supplier<T> supplier) {
        if (!BaseEntity.class.equals(clazz.getSuperclass()) && clazz.getDeclaredAnnotation(TableName.class) == null) {
            throw new IllegalParamException("无效的查询方法返回类型，请指定具体实体类");
        }
        // 通过条件key查询到主键key
        String primaryKey = redisService.get(cacheKeyDTO.getIndexKey());
        // 不存在或已失效
        if (primaryKey == null) {
            return getEntityResult(cacheKeyDTO, seconds, cacheAbleEntity, supplier);
        } else {
            // 说明数据库没有该值，无需重复查询数据库
            if (StringUtils.equals(primaryKey, CacheConstant.NULL)) {
                return null;
            }

            // 如果存在主键key，查询是否有值
            String entityJson = redisService.get(primaryKey);
            // 如果不存在，执行业务逻辑查询数据
            if (entityJson == null) {
                return getEntityResult(cacheKeyDTO, seconds, cacheAbleEntity, supplier);
            }
            // 如果到期时间 < 设置的到期时间，更新缓存数据，防止缓存穿透
            Long primaryKeyExpired = redisService.getExpire(primaryKey, TimeUnit.SECONDS);
            Long conditionKeyExpired = redisService.getExpire(cacheKeyDTO.getIndexKey(), TimeUnit.SECONDS);
            if (conditionKeyExpired == null || conditionKeyExpired < asyncSeconds) {
                redisService.expire(cacheKeyDTO.getIndexKey(), seconds, TimeUnit.SECONDS);
            }
            if (primaryKeyExpired == null || primaryKeyExpired < asyncSeconds) {
                // 异步更新方法
                Runnable runnable = () -> getEntityResult(cacheKeyDTO, seconds, cacheAbleEntity, supplier);
                asyncUpdateCache(primaryKey, runnable);
            }

            // 说明数据库没有该值，无需重复查询数据库
            if (StringUtils.equals(entityJson, CacheConstant.NULL)) {
                return null;
            }

            T entity = BeanUtil.stringToBean(entityJson, clazz);
            if (entity != null) {
                // 如果查询结果实体类中字段值和当前查询字段值不一样，说明已经更改过，重新进行条件查询
                List<Field> fieldList = BeanUtil.getAllFields(entity.getClass());
                for (Field field : fieldList) {
                    Object val = cacheKeyDTO.getParamMap().get(field.getName());
                    field.setAccessible(true);
                    try {
                        if (val != null && !val.equals(field.get(entity))) {
                            return getEntityResult(cacheKeyDTO, seconds, cacheAbleEntity, supplier);
                        }
                    } catch (IllegalAccessException e) {
                        //
                    }
                }
            }
            return entity;
        }
    }

    private void asyncUpdateCache(String key, Runnable runnable) {
        // 先查询是否已经有该key的读锁，如果有直接返回，避免不必要的查询给数据库造成压力
        final String readKey = getReadLockKey(key);
        try {
            redisLock.tryLock(readKey, CacheConstant.SECOND_OF_10, () -> {
                try {
                    // 异步更新值
                    cacheExecutor.execute(runnable);
                } catch (RejectedExecutionException e) {
                    // 当队列任务无法继续执行时，直接让主线程更新缓存
                    runnable.run();
                }
            });
        } catch (GetLockFailedException e) {
            log.warn("getReadLock key: [{}] failed", key);
        }
    }

    private <T> T getEntityResult(CacheKeyDTO cacheKeyDTO, int seconds, CacheAbleEntity cacheAbleEntity, Supplier<T> supplier) {
        T result = supplier.get();
        String primaryKey = null;
        // 这里存条件key，值为主键key
        if (result == null) {
            setNullValue(cacheKeyDTO.getIndexKey());
            return null;
        } else if (cacheKeyDTO.getPrimaryKey() != null) {
            primaryKey = cacheKeyDTO.getPrimaryKey();
        } else {
            Serializable id = ReflectUtil.getPrimaryValue(result);
            if (id != null) {
                String keyPrefix = StringUtils.isNotBlank(cacheAbleEntity.keyNamePrefix()) ? cacheAbleEntity.keyNamePrefix() : cacheKeyConfig.getKeyNamePrefix();
                String keySuffix = StringUtils.isNotBlank(cacheAbleEntity.keyNameSuffix()) ? cacheAbleEntity.keyNameSuffix() : cacheKeyConfig.getKeyNameSuffix();
                KeyFormat keyFormat = cacheAbleEntity.keyNameFormat();
                String cacheKey = BeanUtil.formatKey(result.getClass(), keyFormat);
                // 添加前缀
                cacheKey = cacheKeyUtil.setPrefix(keyPrefix, cacheKey);
                // 添加后缀
                cacheKey = cacheKeyUtil.setSuffix(keySuffix, cacheKey);
                primaryKey = new StringBuilder(cacheKey)
                        .append(CacheConstant.COLON)
                        .append(CacheConstant.PK)
                        .append(CacheConstant.COLON)
                        .append(id)
                        .toString();
            }
        }
        if (primaryKey == null) {
            return result;
        }
        // 判断写锁是否存在，存在则不放入缓存，这里存的是实体类
        Boolean isSuccess = this.setExWithNotExist(getWriteLockKey(primaryKey), primaryKey, result, seconds);
        if (BooleanUtils.isTrue(isSuccess)) {
            // 这里存条件key，值为主键key
            redisService.set(cacheKeyDTO.getIndexKey(), primaryKey, seconds);
        }
        return result;
    }

    private void setNullValue(String key) {
        redisService.set(key, CacheConstant.NULL, CacheConstant.SECOND_OF_10);
    }

    /**
     * 1.新增写锁
     * 2.删除缓存
     * 3.更新数据库
     * 4.释放写锁
     *
     * @param cacheKeyDTO
     * @param lockSeconds 加锁时间，单位秒
     * @param supplier    更新持久层方法
     * @param <T>
     * @return
     */
    public <T> T writeData(CacheKeyDTO cacheKeyDTO, long lockSeconds, Supplier<T> supplier) {
        if (cacheKeyDTO.getPrimaryKey() == null) {
            return supplier.get();
        }
        String primaryKey = cacheKeyDTO.getPrimaryKey();
        String lockKey = getWriteLockKey(primaryKey);
        Long lockNum = this.tryWriteLock(lockKey, lockSeconds);
        log.info("write lock increment key: [{}], lockNum: [{}]", lockKey, lockNum);
        try {
            // 先删除之前的缓存
            this.delete(cacheKeyDTO);
            // 数据库增删改后的值
            return supplier.get();
        } finally {
            lockNum = this.releaseWriteLock(lockKey);
            log.info("write lock decrement key: [{}], lockNum: [{}]", lockKey, lockNum);
        }
    }

    public <T> T insertAutoData(ProceedingJoinPoint pjp, CacheAbleEntity cacheAbleEntity, MethodSignature methodSignature, long lockSeconds, Supplier<T> supplier) throws IllegalAccessException {
        // 先执行插入逻辑后取到自增id
        T result = supplier.get();
        CacheKeyDTO cacheKeyDTO = cacheKeyUtil.assembleFinalCacheKey(pjp, cacheAbleEntity, methodSignature);
        String primaryKey = cacheKeyDTO.getPrimaryKey();
        if (cacheKeyDTO.getPrimaryKey() != null) {
            String lockKey = getWriteLockKey(primaryKey);
            Long lockNum = this.tryWriteLock(lockKey, lockSeconds);
            log.info("write lock increment key: [{}], lockNum: [{}]", lockKey, lockNum);
            // 先删除之前的缓存
            try {
                this.delete(cacheKeyDTO);
            } finally {
                lockNum = this.releaseWriteLock(lockKey);
                log.info("write lock decrement key: [{}], lockNum: [{}]", lockKey, lockNum);
            }
        }
        return result;
    }

    /**
     * 读锁key
     *
     * @param key
     * @return
     */
    private String getReadLockKey(String key) {
        return CacheConstant.READ_LOCK + key;
    }

    /**
     * 写锁key
     *
     * @param key
     * @return
     */
    private String getWriteLockKey(String key) {
        return CacheConstant.WRITE_LOCK + key;
    }

    public void delete(CacheKeyDTO cacheKeyDTO) {
        redisService.delete(Lists.newArrayList(cacheKeyDTO.getPrimaryKey(), cacheKeyDTO.getIndexKey()));
    }

    /**
     * 批量删除指定缓存keyName
     *
     * @param keyNamePrefix
     * @param keyNameSuffix
     * @param keyNameClass
     * @param keyFormat
     */
    public Long deleteByKeyNamePattern(String keyNamePrefix, String keyNameSuffix, Class keyNameClass, KeyFormat keyFormat) {
        String keyName = cacheKeyUtil.generateKeyName(keyNamePrefix, keyNameSuffix, keyNameClass, keyFormat);
        Set<String> keys = redisService.keys(keyName + "*");
        return redisService.delete(keys);
    }

    /**
     * 删除指定缓存key
     *
     * @param keyNamePrefix
     * @param keyNameSuffix
     * @param keyNameClass
     * @param keyFormat
     */
    public Boolean deleteByKey(String keyNamePrefix, String keyNameSuffix, Class keyNameClass, KeyFormat keyFormat, Object keyObj) {
        String key = cacheKeyUtil.assembleKey(keyNamePrefix, keyNameSuffix, keyNameClass, keyFormat, keyObj);
        return redisService.delete(key);
    }


}
