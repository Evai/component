package com.evai.component.utils.concurrent;

import com.evai.component.utils.RandomUtil;
import com.evai.component.utils.SleepUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author crh
 * @date 2019-08-13
 * @description 异步监听回调任务
 */
@Slf4j
public class ListenerExecutorService {
    /**
     * 要执行的线程池
     */
    @Getter
    private ExecutorService executorService;
    /**
     * 成功回调
     */
    private Consumer<Object> vConsumer;
    /**
     * 失败回调
     */
    private Consumer<Throwable> throwableConsumer;

    /**
     * 任务完成回调
     */
    private Runnable finishCallback;

    public ListenerExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * 成功回调
     *
     * @param consumer
     * @return
     */
    public ListenerExecutorService onSuccess(Consumer<Object> consumer) {
        if (consumer != null) {
            this.vConsumer = consumer;
        }
        return this;
    }

    /**
     * 失败回调
     *
     * @param consumer
     * @return
     */
    public ListenerExecutorService onFailure(Consumer<Throwable> consumer) {
        if (consumer != null) {
            this.throwableConsumer = consumer;
        }
        return this;
    }

    /**
     * 任务完成回调
     *
     * @param runnable
     * @return
     */
    public void onFinish(Runnable runnable) {
        if (runnable != null) {
            this.finishCallback = runnable;
        }
    }

    public void submit(Callable<Object> callable) {
        this.executorService.execute(new CallbackListener(callable, vConsumer, throwableConsumer, finishCallback));
    }

    public void shutdown() {
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
    }

    @AllArgsConstructor
    private static final class CallbackListener implements Runnable {
        private final Callable<?> callable;
        private final Consumer<Object> vConsumer;
        private final Consumer<Throwable> throwableConsumer;
        private final Runnable finishCallback;

        @Override
        public void run() {
            Throwable cause;
            try {
                Object value = callable.call();
                if (vConsumer != null) {
                    vConsumer.accept(value);
                }
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                cause = e.getCause();
                if (cause == null) {
                    cause = e;
                }
            } catch (Throwable e) {
                cause = e;
            } finally {
                if (finishCallback != null) {
                    finishCallback.run();
                }
            }
            if (throwableConsumer != null) {
                throwableConsumer.accept(cause);
            }
        }

    }

    public static void main(String[] args) {
//        Thread thread = new Thread(new Task());
//        thread.run();
//        SleepUtil.milliseconds(200);
//        thread.interrupt();
        ListenerExecutorService listenerExecutorService = new ListenerExecutorService(ThreadPoolUtil.newFixedThreadPool("test", 10));
        long start = System.currentTimeMillis();
        for (int j = 0; j < 100; j++) {
            listenerExecutorService
                    .onSuccess(res -> log.info("执行成功: {}", res))
                    .onFailure(err -> log.warn(err.toString()));
            listenerExecutorService.submit(() -> {
                int n = RandomUtil.getRandomInt(100);
                SleepUtil.milliseconds(100);
                if (n % 2 == 0) {
                    int k = n / 0;
                }
                return n;
            });
        }
        listenerExecutorService.shutdown();
        ThreadPoolUtil.await(listenerExecutorService.getExecutorService());
        log.info("end, time={}", System.currentTimeMillis() - start);
    }

    static class Task implements Runnable {

        @Override
        public void run() {
            log.info("start--->{}", Thread.currentThread().getName());

            ExecutorService service = ThreadPoolUtil.newSingleThreadExecutor("single");
            service.execute(() -> {
                long start = System.currentTimeMillis();
                ListenerExecutorService listenerExecutorService = new ListenerExecutorService(ThreadPoolUtil.newFixedThreadPool("test", 50));
                int c = 1;
                for (int i = 0; i < 1; i++) {
                    log.info("执行第{}批任务。。。", c);
//                    CountDownLatch countDownLatch = new CountDownLatch(10);
                    for (int j = 0; j < 500; j++) {
                        listenerExecutorService
                                .onSuccess(res -> log.info("执行成功: {}", res))
                                .onFailure(err -> log.warn(err.toString()));
//                                .onFinish(countDownLatch::countDown);
                        listenerExecutorService.submit(() -> {
                            int n = RandomUtil.getRandomInt(100);
                            SleepUtil.milliseconds(100);
                            if (n % 2 == 0) {
                                int k = n / 0;
                            }
                            return n;
                        });
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        log.info("exit....");
                        break;
                    }
//                    try {
//                        countDownLatch.await();
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
                    c++;
                }
                listenerExecutorService.shutdown();
                ThreadPoolUtil.await(listenerExecutorService.getExecutorService());
                log.info("end, time={}", System.currentTimeMillis() - start);
            });
            service.shutdown();
            log.info("end...");
        }
    }

}
