package com.evai.component.cache.lock;

import com.evai.component.cache.exception.GetLockFailedException;

import java.util.function.Supplier;

/**
 * @author crh
 * @date 2019/6/19
 * @description redis分布式锁
 */
public interface CacheLock {

    /**
     * 加锁并自动释放锁，尝试等待时间
     *
     * @param key
     * @param waitTime
     * @param expired  单位秒
     * @param supplier
     * @return R
     * @throws GetLockFailedException
     */
    <R> R tryLock(String key, long waitTime, long expired, Supplier<R> supplier) throws GetLockFailedException;

    /**
     * 加锁并自动释放锁，无等待时间
     *
     * @param key
     * @param expired  单位秒
     * @param supplier
     * @return R
     * @throws GetLockFailedException
     */
    <R> R tryLock(String key, long expired, Supplier<R> supplier) throws GetLockFailedException;

    void tryLock(String key, long expired, Runnable runnable) throws GetLockFailedException;

    /**
     * 续期锁，成功获取到锁后才执行业务逻辑，否则抛异常
     * 主线程执行业务逻辑，子线程续期
     *
     * @param key
     * @param expired
     * @param supplier
     * @param <T>
     * @return
     * @throws GetLockFailedException
     */
    <T> T renewalLock(String key, long expired, Supplier<T> supplier) throws GetLockFailedException;

    /**
     * @param key
     * @param supplier
     * @param <T>
     * @return
     * @see CacheLock#renewalLock(String, long, Supplier)
     * 增强点：无需指定过期时间，使用默认的过期时间
     */
    <T> T renewalLock(String key, Supplier<T> supplier) throws GetLockFailedException;

}
