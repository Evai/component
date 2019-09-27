package com.evai.component.cache.config;

import com.evai.component.cache.CacheConstant;
import com.evai.component.cache.CacheKeyConfig;
import com.evai.component.utils.concurrent.ThreadPoolUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author crh
 * @date 2019-07-12
 * @description 缓存属性默认配置
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheKeyConfig cacheGenerate() {
        CacheKeyConfig cacheKeyConfig = new CacheKeyConfig();
        cacheKeyConfig.setKeyNamePrefix(CacheConstant.CACHE_PREFIX);
        return cacheKeyConfig;
    }

    @Bean
    public ExecutorService cacheExecutor() {
        return ThreadPoolUtil.newThreadPoolExecutor("redis-cacheExecutor", ThreadPoolUtil.CORE_POOL_SIZE, ThreadPoolUtil.CORE_POOL_SIZE * 2, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(500), new ThreadPoolExecutor.AbortPolicy());
    }

}
