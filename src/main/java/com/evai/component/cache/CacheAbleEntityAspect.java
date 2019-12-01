package com.evai.component.cache;

import com.evai.component.cache.annotation.CacheAbleEntity;
import com.evai.component.cache.enums.CacheAction;
import com.evai.component.cache.utils.CacheKeyUtil;
import com.evai.component.utils.CommonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author crh
 * @date 2019/6/19
 * @description 缓存切面
 */
@Aspect
@Slf4j
@Order(51)
@AllArgsConstructor
public class CacheAbleEntityAspect {

    private final CacheComponent cacheComponent;
    private final CacheKeyUtil cacheKeyUtil;

    @Around(value = "@annotation(cacheAbleEntity)")
    public Object around(ProceedingJoinPoint pjp, CacheAbleEntity cacheAbleEntity) throws Throwable {
        int asyncSeconds = cacheAbleEntity.asyncSeconds();
        int[] expired = cacheAbleEntity.expired();
        int lockSeconds = cacheAbleEntity.lockSeconds();
        CacheAction cacheAction = cacheAbleEntity.action();

        Signature signature = pjp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;

        switch (cacheAction) {
            case INSERT_AUTO:
                return cacheComponent.insertAutoData(pjp, cacheAbleEntity, methodSignature, lockSeconds, () -> proceed(pjp));
            case DEL:
                return cacheComponent.writeData(cacheKeyUtil.assembleFinalCacheKey(pjp, cacheAbleEntity, methodSignature), lockSeconds, () -> proceed(pjp));
            case DEL_PATTERN:
                Object result = pjp.proceed();
                String key = cacheKeyUtil.getCacheKeyName(cacheAbleEntity, pjp);
                cacheComponent.delPattern(key);
                return result;
            case SELECT:
            default:
                // 方法返回类型
                Class returnType = methodSignature.getReturnType();

                int expiredSeconds = cacheKeyUtil.randomExpired(expired);
                return cacheComponent.getEntityCache(cacheKeyUtil.assembleFinalCacheKey(pjp, cacheAbleEntity, methodSignature), expiredSeconds, asyncSeconds, returnType, cacheAbleEntity, () -> proceed(pjp));
        }
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

}
