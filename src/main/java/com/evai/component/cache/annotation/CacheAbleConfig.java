package com.evai.component.cache.annotation;



import com.evai.component.cache.enums.KeyFormat;

import java.lang.annotation.*;

/**
 * @author crh
 * @date 2019-06-17
 * @description 缓存注解类全局配置
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheAbleConfig {

    /**
     * 缓存名称
     */
    String keyName() default "";

    /**
     * 取类的SimpleName作为缓存名称，和keyName二选一，如果有keyName，默认选keyName
     */
    Class<?> keyNameClass() default void.class;

    /**
     * 类名格式转换，只有在keyNameClass有值得时候生效
     */
    KeyFormat keyNameFormat() default KeyFormat.UNDERLINE;

    /**
     * 缓存名称前缀
     */
    String keyNamePrefix() default "";

    /**
     * 缓存名称后缀
     */
    String keyNameSuffix() default "";

    /**
     * 缓存过期随机时间，可以自定义随机时间范围
     */
    int[] expired() default {60, 90};

    /**
     * 异步更新缓存时间阈值，当过期时间小于该值时，会查询数据库最新的数据同步到缓存中
     */
    int asyncSeconds() default 30;

}
