package com.evai.component.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Evai ON 2018/8/24.
 *
 * @author Evai
 */
@Slf4j
public class JacksonUtil {

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final Object[] EMPTY_ARRAY = new Object[0];

    static {
        // 忽略值为NULL的字段
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //取消默认转换时间戳格式
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        //忽略空Bean转json的错误
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        //所有的日期格式统一为 yyyy-MM-dd HH:mm:ss
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        //        objectMapper.setDateFormat(DateFormat.getTimeInstance());
        // 忽略json字符串中存在，但Java对象中不存在对应属性的错误
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * json序列化
     *
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> String objToString(T obj) {
        if (obj == null) {
            return "";
        }
        try {
            return obj instanceof String ? (String) obj : objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("parse object to json error", e);
            return "";
        }
    }

    /**
     * 格式化后的json序列化
     *
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> String objToStringPretty(T obj) {
        if (obj == null) {
            return "";
        }
        try {
            return obj instanceof String ? (String) obj : objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Parse Object to String error", e);
            return "";
        }
    }

    /**
     * json反序列化
     *
     * @param str
     * @param clazz
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T stringToObj(String str, Class<T> clazz) {
        if (StringUtils.isEmpty(str) || clazz == null) {
            return null;
        }
        try {
            return clazz.equals(String.class) ? (T) str : objectMapper.readValue(str, clazz);
        } catch (IOException e) {
            log.warn("Parse String to Object error", e);
            return null;
        }
    }

    /**
     * json反序列化 (指定返回类型)
     *
     * @param str
     * @param typeReference
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T stringToObj(String str, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(str) || typeReference == null) {
            return null;
        }
        try {
            return (T) (typeReference
                    .getType()
                    .equals(String.class) ? str : objectMapper.readValue(str, typeReference));
        } catch (IOException e) {
            log.warn("Parse String to Object error", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T stringToObj(String str, JavaType javaType) {
        if (StringUtils.isBlank(str) || javaType == null) {
            return null;
        }
        try {
            return (T) (javaType.getRawClass().equals(String.class) ? str : objectMapper.readValue(str, javaType));
        } catch (IOException e) {
            log.warn("Parse String to Object error", e);
            return null;
        }
    }

    /**
     * 获取泛型的Collection Type
     *
     * @param clz
     * @param genericElements
     * @return
     */
    public static JavaType getJavaType(Class<?> clz, Class<?>... genericElements) {
        return objectMapper
                .getTypeFactory()
                .constructParametricType(clz, genericElements);
    }

    public static boolean isJson(String str) {
        try {
            objectMapper.readValue(str, Object.class);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] stringToArray(String s, Class<T[]> clazz) {
        T[] arr = stringToObj(s, clazz);
        if (arr == null) {
            return (T[]) EMPTY_ARRAY;
        }
        return arr;
    }

    public static <T> List<T> stringToList(String s, Class<T[]> clazz) {
        T[] arr = stringToObj(s, clazz);
        if (arr == null) {
            return Collections.emptyList();
        }
        Lists.newArrayList(arr);
        return new ArrayList<>(Arrays.asList(arr));
    }

    public static void main(String[] args) {
        String s = "{}";
        System.out.println(isJson(s));
    }

}
