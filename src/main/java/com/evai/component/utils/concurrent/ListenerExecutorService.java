package com.evai.component.utils.concurrent;

import com.evai.component.utils.RandomUtil;
import com.evai.component.utils.SleepUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
    private ExecutorService executorService;
    /**
     * 成功回调
     */
    private Consumer<Object> vConsumer;
    /**
     * 失败回调
     */
    private Consumer<Throwable> throwableConsumer;

    public ListenerExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public ListenerExecutorService onSuccess(Consumer<Object> consumer) {
        if (consumer != null) {
            this.vConsumer = consumer;
        }
        return this;
    }

    public ListenerExecutorService onFailure(Consumer<Throwable> consumer) {
        if (consumer != null) {
            this.throwableConsumer = consumer;
        }
        return this;
    }

    public void submit(Callable<Object> callable) {
        this.executorService.execute(new CallbackListener(this.executorService.submit(callable), vConsumer, throwableConsumer));
    }

    public void execute(Runnable runnable) {
        this.executorService.execute(new CallbackListener(this.executorService.submit(runnable), vConsumer, throwableConsumer));
    }

    public void shutdown() {
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
    }

    @AllArgsConstructor
    private static final class CallbackListener implements Runnable {
        final Future<?> future;
        final Consumer<Object> vConsumer;
        final Consumer<Throwable> throwableConsumer;

        @Override
        public void run() {
            Throwable cause;
            try {
                Object value = future.get();
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
            }
            if (throwableConsumer != null) {
                throwableConsumer.accept(cause);
            }
        }

    }

    public static void main(String[] args) {
        log.info("start");
        long start = System.currentTimeMillis();
        ExecutorService executorService = ThreadPoolUtil.newFixedThreadPool(Runtime
                .getRuntime()
                .availableProcessors(), "test");
        ListenerExecutorService listenerExecutorService = new ListenerExecutorService(executorService)
                .onSuccess(v -> log.info("执行成功: {}", v))
                .onFailure(e -> log.warn(e.toString()));
        for (int i = 0; i < 50; i++) {
            listenerExecutorService.submit(() -> {
                int n = RandomUtil.getRandomInt(100);
                SleepUtil.seconds(RandomUtil.getRandomInt(0, 1));
                if (n % 2 == 0) {
                    int j = n / 0;
                }
                return n;
            });
        }
        listenerExecutorService.shutdown();
        log.info("end, time={}", System.currentTimeMillis() - start);
    }

}
