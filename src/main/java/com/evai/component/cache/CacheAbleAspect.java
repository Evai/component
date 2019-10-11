package com.evai.component.cache;

import com.evai.component.cache.annotation.CacheAble;
import com.evai.component.cache.enums.KeyFormat;
import com.evai.component.cache.exception.IllegalAnnotationException;
import com.evai.component.cache.exception.IllegalKeyIdException;
import com.evai.component.cache.utils.CacheKeyUtil;
import com.evai.component.utils.BeanUtil;
import com.evai.component.utils.CommonUtil;
import com.evai.component.utils.JacksonUtil;
import com.fasterxml.jackson.databind.JavaType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author crh
 * @date 2019/6/19
 * @description 通用缓存切面
 */
@Aspect
@Component
@Slf4j
@Order(50)
@AllArgsConstructor
public class CacheAbleAspect {

    private final CacheComponent cacheComponent;
    private final CacheKeyUtil cacheKeyUtil;

    @Around(value = "@annotation(cacheAble)")
    public Object around(ProceedingJoinPoint pjp, CacheAble cacheAble) throws Throwable {
        int asyncSeconds = cacheAble.asyncSeconds();
        int[] expired = cacheAble.expired();

        Signature signature = pjp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;

        int expiredSeconds = cacheKeyUtil.randomExpired(expired);

        // 方法返回类型
        Class returnType = methodSignature.getReturnType();

        JavaType javaType;
        if (Map.class.equals(returnType) || List.class.equals(returnType) || Set.class.equals(returnType)) {
            Type[] types = BeanUtil.getMethodGenericClass(methodSignature);
            Class[] classes = BeanUtil.toArray(types, Class.class);
            javaType = JacksonUtil.getJavaType(returnType, classes);
        } else {
            javaType = JacksonUtil.getJavaType(returnType);
        }

        return cacheComponent.getCache(assembleFinalCacheKey(pjp, cacheAble, methodSignature), expiredSeconds, asyncSeconds, javaType, () -> this.proceed(pjp));
    }

    /**
     * 执行业务逻辑
     *
     * @param pjp
     * @return
     */
    private Object proceed(ProceedingJoinPoint pjp) {
        try {
            return pjp.proceed();
        } catch (Throwable throwable) {
            CommonUtil.doThrow(throwable);
            return null;
        }
    }

    /**
     * 组合为最终的缓存key
     *
     * @param pjp
     * @param cacheAble
     * @param methodSignature
     * @return
     * @throws IllegalAccessException
     */
    private String assembleFinalCacheKey(ProceedingJoinPoint pjp, CacheAble cacheAble, MethodSignature methodSignature) throws IllegalAccessException {
        // 去除所有空格
        String keyId = cacheAble.keyId().replaceAll(CacheConstant.REG_SPACE, "");
        if (StringUtils.isBlank(keyId)) {
            throw new IllegalAnnotationException("缓存id不能为空");
        }
        // 完整的缓存keyName
        String cacheKey = cacheKeyUtil.getCacheKeyName(cacheAble, pjp);
        // 最终完整的缓存keyId
        keyId = assembleCacheKeyId(pjp, keyId, methodSignature);
        // 最终完整的key，cacheName + cacheKeyId
        String finalKey = cacheKey + keyId;
        log.debug("assembleFinalCacheKey finalKey: [{}]", finalKey);
        return finalKey;
    }

    /**
     * 组合缓存id
     *
     * @param pjp
     * @param keyId
     * @param methodSignature
     * @return String
     * @throws IllegalAccessException
     */
    private String assembleCacheKeyId(ProceedingJoinPoint pjp, String keyId, MethodSignature methodSignature) throws IllegalAccessException {
        String[] parameterNames = methodSignature.getParameterNames();

        // 表达式解析
        Object[] args = pjp.getArgs();
        // 分割keyId
        String[] keyIdExp = keyId.split(CacheConstant.REG_PLUS);

        StringBuilder keyIdValue = new StringBuilder(StringUtils.EMPTY);
        // #user.id + #id
        for (String target : keyIdExp) {
            if (StringUtils.isNotBlank(target)) {
                analyzeKeyIdExp(target, parameterNames, args, keyIdValue);
                keyIdValue.append(CacheConstant.AND);
            }
        }
        // 去除末尾连接符
        if (keyIdValue.charAt(keyIdValue.length() - 1) == CacheConstant.AND) {
            keyIdValue = new StringBuilder(keyIdValue.substring(0, keyIdValue.length() - 1));
        }
        // 转为MD5
        keyIdValue.replace(0, keyIdValue.length(), CommonUtil.getStringMD5(keyIdValue.toString()));
        return keyIdValue.toString();
    }

    /**
     * 解析 keyId 表达式
     *
     * @param target
     * @param parameterNames
     * @param args
     * @param keyIdValue
     * @throws IllegalAccessException
     */
    private void analyzeKeyIdExp(String target, String[] parameterNames, Object[] args, StringBuilder keyIdValue) throws IllegalAccessException {
        // #user.id + id
        if (target.charAt(0) != CacheConstant.EL) {
            // 不是表达式，直接赋值
            keyIdValue.append(target);
            return;
        }
        // 参数名称，去掉#
        target = target.substring(1);
        // user.id
        String[] targetExp = target.split(CacheConstant.REG_DOT);
        if (targetExp.length >= CacheConstant.KEY_LEN) {
            String targetObj = targetExp[0];
            String targetField = targetExp[1];
            // 获取方法参数名称, user
            int index = ArrayUtils.indexOf(parameterNames, targetObj);
            if (index < 0) {
                throw new IllegalArgumentException("未找到对应的方法参数名称，当前表达式为: #" + target);
            }
            // 获取对象的属性, id
            Object object = args[index];
            Object value = cacheKeyUtil.getFieldValue(object, targetObj, targetField);
            String valueStr = BeanUtil.beanToString(value);
            keyIdValue.append(valueStr);
        } else {
            // id 或 user
            int index = ArrayUtils.indexOf(parameterNames, target);
            if (index < 0) {
                throw new IllegalArgumentException("未找到对应的方法参数名称，当前表达式为: #" + target);
            }
            // 直接得到方法参数对象
            Object obj = args[index];
            String valueStr = BeanUtil.beanToString(obj);
            keyIdValue.append(valueStr);
        }
    }

    /**
     * 校验keyId是否为空
     *
     * @param field
     * @param value
     */
    private void checkNullKeyId(String field, String value) {
        if (StringUtils.isBlank(value) || StringUtils.equals(CacheConstant.NULL, value)) {
            throw new IllegalKeyIdException("keyId不能为空，当前字段为: [" + field + "], 值为: [" + value + "]");
        }
    }

    /**
     * 设置缓存名称
     *
     * @param cacheAble
     * @return
     */
    private String setKeyName(CacheAble cacheAble) {
        KeyFormat keyFormat = cacheAble.keyNameFormat();
        // 生成缓存key
        String key = BeanUtil.formatKey(cacheAble.keyNameClass(), keyFormat);
        return cacheKeyUtil.setPrefix(cacheAble.keyNamePrefix(), key);
    }

}
