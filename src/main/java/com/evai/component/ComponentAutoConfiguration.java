package com.evai.component;

import com.evai.component.cache.*;
import com.evai.component.cache.lock.CacheLock;
import com.evai.component.cache.lock.RedisLock;
import com.evai.component.cache.utils.CacheKeyUtil;
import com.evai.component.utils.concurrent.ThreadPoolUtil;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Evai
 * @date 2019-10-27
 * description
 */
@Configuration
@ConditionalOnClass
@EnableConfigurationProperties(CacheProperties.class)
public class ComponentAutoConfiguration {

    private final RedisTemplate<String, String> redisTemplate;

    private final RedissonClient redissonClient;

    public ComponentAutoConfiguration(RedisTemplate<String, String> redisTemplate, RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    @Bean
    @ConditionalOnMissingBean(CacheProperties.class)
    public CacheProperties cacheProperties() {
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setKeyNamePrefix(CacheConstant.CACHE_PREFIX);
        return cacheProperties;
    }

    @Bean
    public ExecutorService cacheExecutor(CacheProperties cacheProperties) {
        return ThreadPoolUtil.newThreadPoolExecutor("redis-cacheExecutor", ThreadPoolUtil.CORE_POOL_SIZE, ThreadPoolUtil.CORE_POOL_SIZE * 2, 30L, TimeUnit.SECONDS, 500, new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    public CacheKeyUtil cacheKeyUtil(CacheProperties cacheProperties) {
        return new CacheKeyUtil(cacheProperties);
    }

    @Bean
    public CacheLock redisLock() {
        return new RedisLock(redissonClient, redisTemplate);
    }

    @Bean
    public RedisService redisService() {
        return new RedisServiceImpl(redisTemplate);
    }

    @Bean
    public CacheComponent cacheComponent(RedisService redisService, CacheProperties cacheProperties, CacheKeyUtil cacheKeyUtil, CacheLock cacheLock, ExecutorService cacheExecutor) {
        return new CacheComponent(redisService, cacheProperties, redisTemplate, cacheKeyUtil, cacheLock, cacheExecutor);
    }

    @Bean
    public CacheAbleAspect cacheAbleAspect(CacheComponent cacheComponent, CacheKeyUtil cacheKeyUtil) {
        return new CacheAbleAspect(cacheComponent, cacheKeyUtil);
    }

    @Bean
    public CacheAbleEntityAspect cacheAbleEntityAspect(CacheComponent cacheComponent, CacheKeyUtil cacheKeyUtil) {
        return new CacheAbleEntityAspect(cacheComponent, cacheKeyUtil);
    }

}
