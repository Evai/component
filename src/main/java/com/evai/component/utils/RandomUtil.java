package com.evai.component.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Evai ON 2018/8/25.
 */
public class RandomUtil {


    private static final String BASE_STR = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static ThreadLocalRandom getRandom() {
        return ThreadLocalRandom.current();
    }

    /**
     * 获得一个[0,max)之间的随机整数。
     *
     * @param max
     * @return
     */
    public static int getRandomInt(int max) {
        return getRandom().nextInt(max);
    }

    /**
     * 获得一个[min, max]之间的随机整数
     *
     * @param min
     * @param max
     * @return
     */
    public static int getRandomInt(int min, int max) {
        return getRandom().nextInt(max - min + 1) + min;
    }

    /**
     * 生成随机数字数组
     *
     * @param length
     * @param start
     * @param end
     * @return
     */
    public static Integer[] nextIntArr(int length, int start, int end) {
        Integer[] arr = new Integer[length];
        for (int i = 0; i < length; i++) {
            arr[i] = getRandomInt(start, end);
        }
        return arr;
    }

    /**
     * 生成近乎有序的随机数字数组
     *
     * @param length     数组长度
     * @param swapLength 需要交换的长度
     * @return
     */
    public static Integer[] nextIntArrOrder(int length, int swapLength) {
        Integer[] arr = new Integer[length];
        for (int i = 0; i < length; i++) {
            arr[i] = i;
        }
        //        for (int i = 0; i < swapLength; i++) {
        //            //随机生成两个索引进行交换
        //            int rand1 = (int) (Math.random() * length);
        //            int rand2 = (int) (Math.random() * length);
        //            SortUtil.swap(arr, rand1, rand2);
        //        }
        return arr;
    }

    /**
     * 生成随机字符串
     *
     * @param bound
     * @return
     */
    public static String randStr(int bound) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < bound; i++) {
            str.append(BASE_STR.charAt(getRandomInt(BASE_STR.length())));
        }
        return str.toString();
    }

    public static String randStr(int start, int end) {
        int bound = getRandomInt(start, end);
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < bound; i++) {
            str.append(BASE_STR.charAt(getRandomInt(BASE_STR.length())));
        }
        return str.toString();
    }

    public static void main(String[] args) {
        //        System.out.println(System.currentTimeMillis());
        //        System.out.println(getRandomInt(5, 5));
        System.out.println(randStr(32));
        System.out.println(randStr(5, 10));
        //        System.out.println(System.currentTimeMillis());
    }

}
