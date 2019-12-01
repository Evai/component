package com.evai.component.cache.utils;


import com.evai.component.cache.CacheProperties;
import com.evai.component.cache.CacheConstant;
import com.evai.component.cache.CacheKeyDTO;
import com.evai.component.cache.annotation.CacheAble;
import com.evai.component.cache.annotation.CacheAbleConfig;
import com.evai.component.cache.annotation.CacheAbleEntity;
import com.evai.component.cache.enums.KeyFormat;
import com.evai.component.cache.exception.IllegalAnnotationException;
import com.evai.component.cache.exception.IllegalFieldException;
import com.evai.component.cache.exception.IllegalGenericTypeException;
import com.evai.component.cache.exception.IllegalKeyIdException;
import com.evai.component.utils.BeanUtil;
import com.evai.component.utils.CommonUtil;
import com.evai.component.utils.RandomUtil;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

/**
 * @author Evai
 * @date 2019-06-23
 * description
 */
@Slf4j
@AllArgsConstructor
public class CacheKeyUtil {

    private final CacheProperties cacheProperties;

    /**
     * 生成keyName
     *
     * @param prefix
     * @param suffix
     * @param keyNameClass
     * @param keyFormat
     * @return
     */
    public String generateKeyName(String prefix, String suffix, Class keyNameClass, KeyFormat keyFormat) {
        String keyName = BeanUtil.formatKey(keyNameClass, keyFormat) + CacheConstant.COLON;
        keyName = setPrefix(prefix, keyName);
        return setSuffix(suffix, keyName);
    }

    /**
     * 生成keyId
     *
     * @param keyObj
     * @return
     */
    public String generateKeyId(Object keyObj) {
        String keyId = BeanUtil.beanToString(keyObj);
        return CommonUtil.getStringMD5(keyId);
    }

    /**
     * 组合缓存key
     *
     * @param prefix
     * @param suffix
     * @param keyNameClass
     * @param keyFormat
     * @param keyObj
     * @return
     */
    public String assembleKey(String prefix, String suffix, Class keyNameClass, KeyFormat keyFormat, Object keyObj) {
        String keyName = generateKeyName(prefix, suffix, keyNameClass, keyFormat);
        String keyId = generateKeyId(keyObj);
        return keyName + keyId;
    }

    /**
     * 设置缓存名称前缀
     *
     * @param key
     * @return
     */
    public String setPrefix(String prefix, String key) {
        if (StringUtils.isNotBlank(prefix)) {
            // 字符串最后面加冒号
            if (prefix.charAt(prefix.length() - 1) != CacheConstant.COLON) {
                prefix += CacheConstant.COLON;
            }
            key = prefix + key;
        }
        return key;
    }

    /**
     * 设置缓存名称后缀
     *
     * @param key
     * @return
     */
    public String setSuffix(String suffix, String key) {
        if (StringUtils.isNotBlank(suffix)) {
            // 字符串最后面加冒号
            if (suffix.charAt(suffix.length() - 1) != CacheConstant.COLON) {
                suffix += CacheConstant.COLON;
            }
            key += suffix;
        }
        return key;
    }

    /**
     * 生成随机过期时间，防止缓存雪崩
     *
     * @param expired
     * @return
     */
    public int randomExpired(int[] expired) {
        if (expired.length > 1) {
            int start = expired[0];
            int end = expired[1];
            return RandomUtil.getRandomInt(start, end);
        } else if (expired.length == 1 && expired[0] > 0) {
            return expired[0];
        }
        return RandomUtil.getRandomInt(30, 60);
    }

    public String getCacheKeyName(CacheAbleEntity cacheAbleEntity, ProceedingJoinPoint pjp) {
        String cacheKey;
        String keyName = cacheAbleEntity.keyName();
        if (StringUtils.isNotBlank(keyName)) {
            cacheKey = keyName;
        } else if (!void.class.equals(cacheAbleEntity.keyNameClass())) {
            cacheKey = BeanUtil.formatKey(cacheAbleEntity.keyNameClass(), cacheAbleEntity.keyNameFormat());
        } else {
            try {
                // 获取到ServiceImpl的泛型实体类
                Class clazz = getGenericType(pjp.getTarget(), 1);
                cacheKey = BeanUtil.formatKey(clazz, cacheAbleEntity.keyNameFormat());
            } catch (IllegalGenericTypeException e) {
                throw new IllegalAnnotationException("缓存名称未设置");
            }
        }
        return getDefaultCacheKeyName(cacheKey, cacheAbleEntity.keyNamePrefix(), cacheAbleEntity.keyNameSuffix());
    }

    public String getCacheKeyName(CacheAble cacheAble, ProceedingJoinPoint pjp) {
        Class clazz = pjp.getTarget().getClass();
        CacheAbleConfig cacheAbleConfig = (CacheAbleConfig) clazz.getDeclaredAnnotation(CacheAbleConfig.class);
        String cacheKey = null;
        String keyName = cacheAble.keyName();
        if (StringUtils.isNotBlank(keyName)) {
            cacheKey = keyName;
        } else if (!void.class.equals(cacheAble.keyNameClass())) {
            cacheKey = BeanUtil.formatKey(cacheAble.keyNameClass(), cacheAble.keyNameFormat());
        } else {
            if (cacheAbleConfig != null) {
                if (StringUtils.isNotBlank(cacheAbleConfig.keyName())) {
                    cacheKey = cacheAbleConfig.keyName();
                } else if (!void.class.equals(cacheAbleConfig.keyNameClass())) {
                    cacheKey = BeanUtil.formatKey(cacheAbleConfig.keyNameClass(), cacheAbleConfig.keyNameFormat());
                }
            }
            if (cacheKey == null) {
                // 都未设置就默认取实现类名称
                cacheKey = BeanUtil.formatKey(clazz, cacheAble.keyNameFormat());
            }
        }

        String keyNamePrefix = cacheAble.keyNamePrefix();
        String keyNameSuffix = cacheAble.keyNameSuffix();
        if (cacheAbleConfig != null) {
            keyNamePrefix = StringUtils.isNotBlank(keyNamePrefix) ? keyNamePrefix : cacheAbleConfig.keyNamePrefix();
            keyNameSuffix = StringUtils.isNotBlank(keyNameSuffix) ? keyNameSuffix : cacheAbleConfig.keyNameSuffix();
        }

        return getDefaultCacheKeyName(cacheKey, keyNamePrefix, keyNameSuffix);
    }

    private String getDefaultCacheKeyName(String keyName, String keyPrefix, String keySuffix) {
        keyPrefix = StringUtils.isNotBlank(keyPrefix) ? keyPrefix : cacheProperties.getKeyNamePrefix();
        keySuffix = StringUtils.isNotBlank(keySuffix) ? keySuffix : cacheProperties.getKeyNameSuffix();
        return getCacheKeyName(keyName, keyPrefix, keySuffix);
    }

    /**
     * 生成完整的缓存名称
     *
     * @param keyName
     * @param keyPrefix
     * @param keySuffix
     * @return
     */
    public String getCacheKeyName(String keyName, String keyPrefix, String keySuffix) {
        String cacheKey = keyName;

        // 字符串最后面加冒号
        if (cacheKey.charAt(cacheKey.length() - 1) != CacheConstant.COLON) {
            cacheKey += CacheConstant.COLON;
        }
        // 添加前缀
        cacheKey = this.setPrefix(keyPrefix, cacheKey);
        // 添加后缀
        cacheKey = this.setSuffix(keySuffix, cacheKey);
        return cacheKey;
    }

    /**
     * 组合为最终的缓存key
     *
     * @param pjp
     * @param cacheAbleEntity
     * @param methodSignature
     * @return
     * @throws IllegalAccessException
     */
    public CacheKeyDTO assembleFinalCacheKey(ProceedingJoinPoint pjp, CacheAbleEntity cacheAbleEntity, MethodSignature methodSignature) throws IllegalAccessException {
        // 去除所有空格
        String keyId = cacheAbleEntity.keyId().replaceAll(CacheConstant.REG_SPACE, "");
        if (StringUtils.isBlank(keyId)) {
            throw new IllegalAnnotationException("缓存id不能为空");
        }
        // 完整的缓存keyName
        String cacheKey = this.getCacheKeyName(cacheAbleEntity, pjp);

        CacheKeyDTO cacheKeyDTO = new CacheKeyDTO();
        // 最终完整的缓存keyId
        keyId = assembleCacheKeyId(cacheKeyDTO, cacheKey, pjp, keyId, methodSignature);

        // 最终完整的key，cacheName + cacheKeyId
        String finalKey = cacheKey + keyId;
        cacheKeyDTO.setIndexKey(finalKey);
        log.debug("finalKey:【{}】", finalKey);
        return cacheKeyDTO;
    }

    /**
     * 组合缓存id
     *
     * @param cacheKeyDTO
     * @param cacheKey
     * @param pjp
     * @param keyId
     * @param methodSignature
     * @return String
     * @throws IllegalAccessException
     */
    public String assembleCacheKeyId(CacheKeyDTO cacheKeyDTO, String cacheKey, ProceedingJoinPoint pjp, String keyId, MethodSignature methodSignature) throws IllegalAccessException {
        String[] parameterNames = methodSignature.getParameterNames();

        // 表达式解析
        Object[] args = pjp.getArgs();
        // 分割keyId
        String[] keyIdExp = keyId.split(CacheConstant.REG_PLUS);

        StringBuilder keyIdValue = new StringBuilder(StringUtils.EMPTY);
        Map<String, Object> paramMap = Maps.newHashMapWithExpectedSize(keyIdExp.length);
        for (String target : keyIdExp) {
            if (StringUtils.isNotBlank(target)) {
                analyzeKeyIdExp(cacheKeyDTO, cacheKey, methodSignature, target, parameterNames, args, keyIdValue, paramMap);
                keyIdValue.append(CacheConstant.AND);
            }
        }
        if (StringUtils.isBlank(keyIdValue)) {
            throw new IllegalKeyIdException("keyId值不能为空");
        }
        cacheKeyDTO.setParamMap(paramMap);
        // 去除末尾连接符
        if (keyIdValue.charAt(keyIdValue.length() - 1) == CacheConstant.AND) {
            keyIdValue = new StringBuilder(keyIdValue.substring(0, keyIdValue.length() - 1));
        }
        // 转为MD5
        if (keyIdValue.length() > CacheConstant.MD5_LEN) {
            keyIdValue.replace(0, keyIdValue.length(), CommonUtil.getStringMD5(keyIdValue.toString()));
        }
        return keyIdValue.toString();
    }

    /**
     * 解析 keyId 表达式
     *
     * @param cacheKeyDTO
     * @param cacheKey
     * @param methodSignature
     * @param target
     * @param parameterNames
     * @param args
     * @param keyIdValue
     * @param paramMap
     * @throws IllegalAccessException
     */
    public void analyzeKeyIdExp(CacheKeyDTO cacheKeyDTO, String cacheKey, MethodSignature methodSignature, String target, String[] parameterNames, Object[] args, StringBuilder keyIdValue, Map<String, Object> paramMap) throws IllegalAccessException {
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
                throw new IllegalArgumentException("未找到[" + methodSignature.getName() + "]方法参数名称，当前keyId表达式为: #" + target);
            }
            // 获取对象的属性, id
            Object object = args[index];
            Object value = getFieldValue(object, targetObj, targetField);
            // 设置参数
            paramMap.put(targetField, value);

            String valueStr = BeanUtil.beanToString(value);

            keyIdValue.append(targetField)
                    .append(CacheConstant.ASSIGN)
                    .append(valueStr);

            setPrimaryKey(cacheKeyDTO, cacheKey, targetField, valueStr);
        } else {
            // id 或 user
            int index = ArrayUtils.indexOf(parameterNames, target);
            if (index < 0) {
                throw new IllegalArgumentException("未找到[" + methodSignature.getName() + "]方法参数名称，当前keyId表达式为: #" + target);
            }
            // 直接得到方法参数对象
            Object obj = args[index];
            if (obj == null || String.class.equals(obj.getClass()) || BeanUtil.isPrimitive(obj)) {
                // 设置参数
                paramMap.put(target, obj);
                // 判断是否是基本数据类型
                String valueStr = BeanUtil.beanToString(obj);
                keyIdValue.append(target)
                        .append(CacheConstant.ASSIGN)
                        .append(valueStr);
                setPrimaryKey(cacheKeyDTO, cacheKey, target, valueStr);
            } else {
                // 否则取该对象所有属性值作为keyId
                List<Field> fieldList = BeanUtil.getAllFields(obj.getClass());
                for (Field field : fieldList) {
                    // 得到属性值
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    // 设置参数
                    paramMap.put(field.getName(), value);
                    String valueStr = BeanUtil.beanToString(value);
                    keyIdValue.append(field.getName())
                            .append(CacheConstant.ASSIGN)
                            .append(valueStr)
                            .append(CacheConstant.AND);
                    setPrimaryKey(cacheKeyDTO, cacheKey, field.getName(), valueStr);
                }
                // 去除末尾连接符
                if (keyIdValue.charAt(keyIdValue.length() - 1) == CacheConstant.AND) {
                    keyIdValue.substring(0, keyIdValue.length());
                }
            }

        }
    }

    public Object getFieldValue(Object object, String targetObj, String targetField) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(object.getClass(), targetField);
        if (field == null) {
            throw new IllegalFieldException("未找到[" + targetObj + "]的[" + targetField + "]属性");
        }
        // 得到属性值
        field.setAccessible(true);
        return field.get(object);
    }

    /**
     * 设置主键key
     *
     * @param cacheKeyDTO
     * @param cacheKey
     * @param fieldName
     * @param valueStr
     */
    public void setPrimaryKey(CacheKeyDTO cacheKeyDTO, String cacheKey, String fieldName, String valueStr) {
        // 如果属性有id值，放入对象，user:id=1
        if (CacheConstant.PK.equals(fieldName)) {
            cacheKeyDTO.setPrimaryKey(cacheKey + fieldName + CacheConstant.COLON + valueStr);
        }
    }

    public Class getGenericType(Object obj, int index) {
        try {
            return (Class) ((ParameterizedType) obj
                    .getClass()
                    .getGenericSuperclass())
                    .getActualTypeArguments()[index];
        } catch (Exception e) {
            throw new IllegalGenericTypeException("not found generic type");
        }
    }

}
