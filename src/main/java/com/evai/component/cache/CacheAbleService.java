package com.evai.component.cache;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evai.component.cache.annotation.CacheAbleEntity;
import com.evai.component.cache.enums.CacheAction;
import com.evai.component.mybatis.BaseEntity;
import com.evai.component.mybatis.BaseService;
import com.evai.component.mybatis.PrimaryKey;
import com.evai.component.mybatis.utils.ReflectUtil;

import java.io.Serializable;

/**
 * @author crh
 * @date 2019-06-20
 * @description 通用缓存接口
 */
public interface CacheAbleService<T extends BaseEntity> extends BaseService<T> {

    /**
     * 缓存数据，没有从数据库中查询并根据主键放入缓存
     * keyId取 id 值
     *
     * @param id
     * @return
     */
    @CacheAbleEntity(action = CacheAction.SELECT, keyId = "#id")
    default T cacheSelectById(Serializable id) {
        if (id == null) {
            return null;
        }
        return getById(id);
    }

    /**
     * 新增数据，并根据主键删除缓存
     * keyId取 entity 的自增id值
     *
     * @param entity
     * @return
     */
    @CacheAbleEntity(action = CacheAction.INSERT_AUTO, keyId = "#entity.id")
    default boolean cacheInsert(T entity) {
        if (entity == null) {
            return false;
        }
        return save(entity);
    }

    /**
     * 更新数据，并根据主键删除缓存
     * keyId取 entity 的id值
     *
     * @param entity
     * @return
     */
    @CacheAbleEntity(action = CacheAction.DEL, keyId = "#entity.id")
    default boolean cacheUpdateById(T entity) {
        if (entity == null) {
            return false;
        }
        int count = count(new QueryWrapper<T>().eq(CacheConstant.PK, entity.getId()));
        if (count == 0) {
            return false;
        }
        return updateById(entity);
    }

    /**
     * 根据主键删除缓存
     * keyId取 id 值
     *
     * @param id
     * @return
     */
    @CacheAbleEntity(action = CacheAction.DEL, keyId = "#id")
    default boolean cecheDeleteById(Serializable id) {
        if (id == null) {
            return false;
        }
        int count = count(new QueryWrapper<T>().eq(CacheConstant.PK, id));
        if (count == 0) {
            return false;
        }
        return removeById(id);
    }


}
