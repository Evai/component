package com.evai.component.utils.concurrent;

import com.evai.component.utils.SleepUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author: crh
 * @date: 2018/9/17
 */
@Slf4j
public class ThreadPoolUtil {
    /**
     * 核心线程数
     * 核心线程会一直存活，及时没有任务需要执行
     * 当线程数小于核心线程数时，即使有线程空闲，线程池也会优先创建新线程处理
     * 设置allowCoreThreadTimeout=true（默认false）时，核心线程会超时关闭
     */
    public static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    /**
     * 最大线程数
     * 当线程数>=corePoolSize，且任务队列已满时。线程池会创建新线程来处理任务
     * 当线程数=maxPoolSize，且任务队列已满时，线程池会拒绝处理任务而抛出异常
     */
    public static final int MAX_POOL_SIZE = 500;
    /**
     * 线程空闲时间
     * 当线程空闲时间达到keepAliveTime时，线程会退出，直到线程数量=corePoolSize
     * 如果allowCoreThreadTimeout=true，则会直到线程数量=0
     */
    public static final long KEEP_ALIVE_TIME = 30L;
    /**
     * 任务队列容量（阻塞队列）
     * 当核心线程数达到最大时，新任务会放在队列中排队等待执行
     */
    public static final int QUEUE_SIZE = 1000;

    /**
     * 默认拒绝策略
     */
    public static final RejectedExecutionHandler DEFAULT_HANDLER = new ThreadPoolExecutor.AbortPolicy();


    private ThreadPoolUtil() {
    }

    /**
     * 自定义拒绝策略
     */
    public static class CustomCallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            log.info("线程池任务已满，当前任务: {}，当前线程: {}", r, e);
            throw new RejectedExecutionException("Task " + r.toString() +
                    " rejected from " +
                    e.toString());
        }
    }

    /**
     * 自定义线程池
     *
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param queueSize
     * @param handler
     * @return
     */
    public static ExecutorService newThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, int queueSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        if (corePoolSize < 1) {
            corePoolSize = 1;
        }
        if (corePoolSize > MAX_POOL_SIZE) {
            corePoolSize = MAX_POOL_SIZE;
        }
        if (maximumPoolSize < 1) {
            maximumPoolSize = 1;
        }
        if (maximumPoolSize > MAX_POOL_SIZE) {
            maximumPoolSize = MAX_POOL_SIZE;
        }
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, createQueue(queueSize), threadFactory, handler);
    }

    /**
     * 自定义线程池
     *
     * @param threadPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param queueSize
     * @param handler
     * @return
     */
    public static ExecutorService newThreadPoolExecutor(String threadPrefix, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, int queueSize, RejectedExecutionHandler handler) {
        ThreadFactory threadFactory = setThreadFactory(threadPrefix);
        return newThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, queueSize, threadFactory, handler);
    }

    private static BlockingQueue<Runnable> createQueue(int queueSize) {
        return queueSize > 0 ? new LinkedBlockingQueue<>(queueSize) : new SynchronousQueue<>();
    }

    public static ExecutorService newCachedThreadPool(String threadPrefix) {
        return newThreadPoolExecutor(threadPrefix, 0, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, 0, DEFAULT_HANDLER);
    }

    /**
     * 创建一个缓存线程池
     */
    public static ExecutorService newCachedThreadPool(String threadPrefix, int maximumPoolSize) {
        return newThreadPoolExecutor(threadPrefix, 1, maximumPoolSize, KEEP_ALIVE_TIME, TimeUnit.SECONDS, QUEUE_SIZE, DEFAULT_HANDLER);
    }

    /**
     * 创建一个缓存线程池
     */
    public static ExecutorService newCachedThreadPool(String threadPrefix, int corePoolSize, int maximumPoolSize) {
        return newThreadPoolExecutor(threadPrefix, corePoolSize, maximumPoolSize, KEEP_ALIVE_TIME, TimeUnit.SECONDS, QUEUE_SIZE, DEFAULT_HANDLER);
    }

    /**
     * 创建一个指定工作线程数量的线程池
     *
     * @param threadPrefix
     * @param nThreads
     * @return
     */
    public static ExecutorService newFixedThreadPool(String threadPrefix, int nThreads) {
        return newThreadPoolExecutor(threadPrefix, nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, QUEUE_SIZE, DEFAULT_HANDLER);
    }

    /**
     * 创建唯一的工作者线程来执行任务
     *
     * @param threadPrefix
     * @return
     */
    public static ExecutorService newSingleThreadExecutor(String threadPrefix) {
        ThreadFactory threadFactory = setThreadFactory(threadPrefix);
        return Executors.newSingleThreadExecutor(threadFactory);
    }

    /**
     * 创建一个定长的线程池，支持定时的以及周期性的任务执行。
     *
     * @param nThreads
     * @param threadPrefix
     * @return
     */
    public static ScheduledExecutorService newScheduledThreadPool(int nThreads, String threadPrefix) {
        if (nThreads < 1) {
            nThreads = 1;
        }
        if (nThreads > MAX_POOL_SIZE) {
            nThreads = MAX_POOL_SIZE;
        }
        ThreadFactory threadFactory = setThreadFactory(threadPrefix);
        return Executors.newScheduledThreadPool(nThreads, threadFactory);
    }

    public static ThreadFactory setThreadFactory(String threadPrefix) {
        return new ThreadFactoryBuilder()
                .setNameFormat(threadPrefix + "-pool-%d")
                .build();
    }

    /**
     * 等待所有任务执行完
     *
     * @param futures
     * @param millis
     */
    public static void await(List<Future> futures, long millis) {
        if (futures == null || futures.size() == 0) {
            return;
        }
        boolean isDone = false;
        do {
            for (Future future : futures) {
                if (!(isDone = future.isDone())) {
                    SleepUtil.milliseconds(millis);
                    break;
                }
            }
        } while (!isDone);
    }

    /**
     * 等待所有任务执行完
     *
     * @param service
     */
    public static void await(ExecutorService service) {
        service.shutdown();
        while (!service.isTerminated()) {
            try {
                boolean isFinish = service.awaitTermination(10, TimeUnit.SECONDS);
                if (isFinish) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static List<Runnable> await(ExecutorService service, long timeout, TimeUnit unit) {
        service.shutdown();
        while (!service.isTerminated()) {
            try {
                boolean isFinish = service.awaitTermination(timeout, unit);
                if (!isFinish) {
                    return service.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return Collections.emptyList();
    }


    /**
     * 计算需要多少线程数
     *
     * @param taskNum        任务数量
     * @param avgTaskSecond  平均每个任务耗时（秒）
     * @param completeSecond 完成所有任务需要的时间
     * @return
     */
    public static int calculatePoolSize(int taskNum, double avgTaskSecond, double completeSecond) {
        return BigDecimal
                .valueOf(taskNum / (completeSecond / avgTaskSecond))
                .setScale(0, BigDecimal.ROUND_UP)
                .intValue();
    }

    /**
     * 计算任务数量
     *
     * @param poolSize       线程数
     * @param avgTaskSecond  平均每个任务耗时（秒）
     * @param completeSecond 完成所有任务需要的时间
     * @return
     */
    public static int calculateTaskNum(int poolSize, double avgTaskSecond, double completeSecond) {
        return BigDecimal
                .valueOf(poolSize * (completeSecond / avgTaskSecond))
                .setScale(0, BigDecimal.ROUND_UP)
                .intValue();
    }

    /**
     * 计算执行完成时间
     *
     * @param poolSize      线程数
     * @param taskNum       任务数量
     * @param avgTaskSecond 平均每个任务耗时（秒）
     * @return
     */
    public static double calculateCompleteSecond(int poolSize, int taskNum, double avgTaskSecond) {
        return BigDecimal
                .valueOf(avgTaskSecond * (double) (taskNum / poolSize))
                .setScale(2, BigDecimal.ROUND_UP)
                .doubleValue();
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        System.out.println(calculatePoolSize(500, 0.1d, 1));
        System.out.println((int) Math.ceil((double) 5001 / QUEUE_SIZE));
        System.out.println(calculateCompleteSecond(10, 100, 0.1d));
    }
}
