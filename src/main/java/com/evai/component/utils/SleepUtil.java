package com.evai.component.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author: crh
 * @date: 2019/2/1
 * @description: 休眠工具类
 */
@Slf4j
public class SleepUtil {

    public static void milliseconds(long milliseconds) {
        sleep(milliseconds, TimeUnit.MILLISECONDS);
    }

    public static void seconds(long seconds) {
        sleep(seconds, TimeUnit.SECONDS);
    }

    public static void minutes(long minutes) {
        sleep(minutes, TimeUnit.MINUTES);
    }

    public static void hours(long hours) {
        sleep(hours, TimeUnit.HOURS);
    }

    public static void days(long days) {
        sleep(days, TimeUnit.DAYS);
    }

    private static void sleep(long timeout, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(timeout);
        } catch (InterruptedException e) {
            Thread
                    .currentThread()
                    .interrupt();
        }
    }

}
