package com.evai.component.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * @author: crh
 * @date: 2018/12/24
 * @description: 序列化工具类
 */
@Slf4j
public class SerializeUtil {

    /**
     * 序列化为字节流
     *
     * @return
     */
    public static byte[] serialize(Object object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("serializeByteArray error", e);
        }
        return null;
    }

    /**
     * 返序列化
     *
     * @return
     */
    public static Object unSerialize(byte[] bytes) {
        return unSerialize(bytes, Object.class);
    }

    /**
     * 返序列化
     *
     * @return
     */
    public static <T> T unSerialize(byte[] bytes, Class<T> clz) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes); ObjectInputStream ois = new ObjectInputStream(bais)) {
            Object object = ois.readObject();
            return clz.cast(object);
        } catch (Exception e) {
            log.error("unSerialize error", e);
        }
        return null;
    }

    /**
     * 序列化并写入文件
     *
     * @param object
     * @param file
     */
    public static void writeFile(Object object, File file) {
        try (FileOutputStream fos = new FileOutputStream(file); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(object);
        } catch (Exception e) {
            log.error("write error", e);
        }
    }

    /**
     * 反序列化读取文件
     *
     * @param file
     * @return
     */
    public static Object readFile(File file) {
        try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {
            return ois.readObject();
        } catch (Exception e) {
            log.error("readFile error", e);
        }
        return null;
    }

    public static void main(String[] args) {


    }

}
