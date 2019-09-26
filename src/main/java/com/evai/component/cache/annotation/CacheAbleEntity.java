package com.evai.component.cache.annotation;



import com.evai.component.cache.enums.CacheAction;
import com.evai.component.cache.enums.KeyFormat;

import java.lang.annotation.*;

/**
 * @author crh
 * @date 2019-06-17
 * @description 实体类缓存注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CacheAbleEntity {
    /**
     * 缓存id，如果是以"#"开头，则获取当前方法参数值
     */
    String keyId();

    /**
     * 缓存动作
     */
    CacheAction action();

    /**
     * 缓存名称
     */
    String keyName() default "";

    /**
     * 取类的SimpleName作为缓存名称，和keyName二选一，默认选keyName
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
     * 缓存过期随机时间，默认随机300~600秒之间的数，也可以自定义随机时间范围
     */
    int[] expired() default {300, 600};

    /**
     * 异步更新缓存时间阈值，当过期时间小于该值时，会查询数据库最新的数据同步到缓存中（CacheAction为SELECT有效）
     */
    int asyncSeconds() default 30;

    /**
     * 更新数据的加锁时间，单位秒（CacheAction为UPDATE有效）
     */
    int lockSeconds() default 30;

}
