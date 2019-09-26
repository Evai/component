package com.evai.component.utils.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author: Evai
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
    public static final int CORE_POOL_SIZE = Runtime
            .getRuntime()
            .availableProcessors();
    /**
     * 最大线程数
     * 当线程数>=corePoolSize，且任务队列已满时。线程池会创建新线程来处理任务
     * 当线程数=maxPoolSize，且任务队列已满时，线程池会拒绝处理任务而抛出异常
     */
    public static final int MAX_POOL_SIZE = Integer.MAX_VALUE;
    /**
     * 线程空闲时间
     * 当线程空闲时间达到keepAliveTime时，线程会退出，直到线程数量=corePoolSize
     * 如果allowCoreThreadTimeout=true，则会直到线程数量=0
     */
    public static final long KEEP_ALIVE_TIME = 60L;
    /**
     * 任务队列容量（阻塞队列）
     * 当核心线程数达到最大时，新任务会放在队列中排队等待执行
     */
    public static final int QUEUE_SIZE = 1024;


    private ThreadPoolUtil() {
    }

    /**
     * 自定义拒绝策略
     */
    public static class CustomPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.info("任务: {}，参数: {}", r, executor.toString());
        }
    }

    /**
     * 自定义线程池
     *
     * @param threadPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @param handler
     * @return
     */
    public static ExecutorService newThreadPoolExecutor(String threadPrefix, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        ThreadFactory threadFactory = setThreadFactory(threadPrefix);
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * 创建一个缓存线程池
     */
    public static ExecutorService newCachedThreadPool(String threadPrefix) {
        ThreadFactory threadFactory = setThreadFactory(threadPrefix);
        return Executors.newCachedThreadPool(threadFactory);
    }

    /**
     * 创建一个指定工作线程数量的线程池
     *
     * @param nThreads
     * @param threadPrefix
     * @return
     */
    public static ExecutorService newFixedThreadPool(int nThreads, String threadPrefix) {
        ThreadFactory threadFactory = setThreadFactory(threadPrefix);
        return Executors.newFixedThreadPool(nThreads, threadFactory);
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
     * @throws InterruptedException
     */
    public static void await(List<Future> futures, long millis) throws InterruptedException {
        if (futures == null || futures.size() == 0) {
            return;
        }
        boolean isDone = false;
        do {
            for (Future future : futures) {
                if (!(isDone = future.isDone())) {
                    Thread.sleep(millis);
                    break;
                }
            }
        } while (!isDone);
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorService service = newCachedThreadPool("demo");
        Future future = service.submit(() -> {
            System.out.println("任务一开始");
            Thread.sleep(3000);
            System.out.println("任务一结束");
            return null;
        });
        Future future2 = service.submit(() -> {
            System.out.println("任务二开始");
            Thread.sleep(2000);
            System.out.println("任务二结束");
            return null;
        });
        List<Future> futures = new ArrayList<>(Arrays.asList(future, future2));
        await(futures, 10000);
        System.out.println("end");
        service.shutdown();
    }
}
