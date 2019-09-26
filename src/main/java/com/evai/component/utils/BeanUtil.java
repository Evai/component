package com.evai.component.utils;

import com.evai.component.cache.CacheConstant;
import com.evai.component.cache.enums.KeyFormat;
import com.evai.component.utils.JacksonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.CaseFormat;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
public class BeanUtil {
    /**
     * String 转 实体类
     *
     * @param str
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T stringToBean(String str, Class<T> clazz) {
        if (str == null || clazz == null) {
            return null;
        }
        if (String.class == clazz) {
            return clazz.cast(str);
        }
        return JacksonUtil.stringToObj(str, clazz);
    }

    /**
     * String 转 实体类
     *
     * @param str
     * @param typeReference
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T stringToBean(String str, TypeReference<T> typeReference) {
        if (str == null || typeReference == null) {
            return null;
        }
        if (String.class == typeReference.getType()) {
            return (T) str;
        }
        return JacksonUtil.stringToObj(str, typeReference);
    }

    /**
     * 对象 转 String
     *
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> String beanToString(T obj) {
        if (obj == null) {
            return CacheConstant.NULL;
        }
        Class<?> clazz = obj.getClass();
        if (String.class == clazz) {
            return (String) obj;
        } else if (isNumber(clazz)) {
            return String.valueOf(obj);
        } else {
            return JacksonUtil.objToString(obj);
        }
    }

    /**
     * 是否是数字
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> boolean isNumber(Class<T> clazz) {
        if (clazz == null) {
            return false;
        }
        if (byte.class == clazz || short.class == clazz || int.class == clazz || long.class == clazz || float.class == clazz || double.class == clazz) {
            return true;
        }
        return Number.class.equals(clazz.getSuperclass());
    }

    /**
     * 判断一个对象是否是基本类型或基本类型的包装类型
     *
     * @param obj
     * @return
     */
    public static boolean isPrimitive(Object obj) {
        try {
            return ((Class<?>) obj
                    .getClass()
                    .getField("TYPE")
                    .get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取类所有字段（包括父类，不包括子类）
     *
     * @param clazz
     * @return
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        if (null == clazz) {
            return Collections.emptyList();
        }
        List<Field> list = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            // 过滤静态属性
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            // 过滤 transient 关键字修饰的属性
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            list.add(field);
        }
        // 获取父类字段
        Class<?> superClass = clazz.getSuperclass();
        if (Object.class.equals(superClass)) {
            return list;
        }
        list.addAll(getAllFields(superClass));
        return list;
    }

    /**
     * 类名格式转换
     *
     * @param clazz
     * @param keyFormat
     * @return
     */
    public static String formatKey(Class<?> clazz, KeyFormat keyFormat) {
        switch (keyFormat) {
            case HYPHEN:
                return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, clazz.getSimpleName());
            case CAMEL:
                return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, clazz.getSimpleName());
            case UNDERLINE:
            default:
                return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, clazz.getSimpleName());
        }
    }

    /**
     * 获取集合泛型类
     *
     * @param methodSignature
     * @return
     */
    public static Class getMethodGenericClass(MethodSignature methodSignature) {
        return (Class) ((ParameterizedType) methodSignature
                .getMethod()
                .getGenericReturnType()).getActualTypeArguments()[0];
    }

}
