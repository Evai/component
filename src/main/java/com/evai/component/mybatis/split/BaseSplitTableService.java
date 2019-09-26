package com.evai.component.mybatis.split;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evai.component.mybatis.BaseEntity;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author crh
 * @date 2019-05-30
 * @description 分库分表接口类
 */
public interface BaseSplitTableService<T extends BaseEntity> {
    /**
     * 插入一条记录
     *
     * @param entity
     * @return
     */
    boolean insert(T entity);

    /**
     * 插入多条记录
     *
     * @param list
     * @return
     */
    boolean insertBatch(List<T> list);

    /**
     * @see BaseSplitTableService#insertBatch(List, int)
     *
     * @param list
     * @param batchSize 一次批量处理多少个sql
     * @return
     */
    boolean insertBatch(List<T> list, int batchSize);

    /**
     * 根据id更新一条记录
     *
     * @param entity
     * @return
     */
    boolean updateById(T entity);

    /**
     * 根据id更新多条记录
     *
     * @param list
     * @return
     */
    boolean updateBatchById(List<T> list);

    /**
     * @see BaseSplitTableService#updateBatchById(List, int)
     *
     * @param list
     * @param batchSize 一次批量处理多少个sql
     * @return
     */
    boolean updateBatchById(List<T> list, int batchSize);

    /**
     * 根据id删除记录
     *
     * @param id
     * @return
     */
    boolean deleteById(Long id);

    /**
     * 根据id批量删除记录
     *
     * @param ids
     * @return
     */
    boolean deleteBatchById(Collection<Long> ids);

    /**
     * @see BaseSplitTableService#deleteBatchById(Collection, int)
     *
     * @param ids
     * @param batchSize 一次批量处理多少个sql
     * @return
     */
    boolean deleteBatchById(Collection<Long> ids, int batchSize);

    /**
     * 根据条件查询一条记录
     *
     * @param entity
     * @return
     */
    T getOne(T entity, SqlCondition sqlCondition);

    /**
     * @param entity
     * @return
     * @see BaseSplitTableService#getOne(BaseEntity)
     */
    T getOne(T entity);

    /**
     * 根据id查询一条记录
     *
     * @param id
     * @return
     */
    T getOneById(Long id, SqlCondition sqlCondition);

    /**
     * @param id
     * @return
     * @see BaseSplitTableService#getOneById(Long)
     */
    T getOneById(Long id);

    /**
     * 根据条件查询所有记录
     *
     * @param entity
     * @return
     */
    List<T> getList(T entity, SqlCondition sqlCondition);

    /**
     * @param entity
     * @return
     * @see BaseSplitTableService#getList(BaseEntity)
     */
    List<T> getList(T entity);

    /**
     * 自定义sql，每张表返回的结果封装到List结果集
     *
     * @param fn
     * @param <R>
     * @return
     */
    <R> List<R> getList(Function<String, List<R>> fn);

    /**
     * 根据条件统计所有记录
     *
     * @param entity
     * @return
     */
    int count(T entity);

    /**
     * 自定义sql统计记录
     *
     * @param fn
     * @return
     */
    int count(Function<String, Integer> fn);

    /**
     * 自定义sql，每张表返回的结果封装到List结果集
     *
     * @param fn
     * @param <R>
     * @return
     */
    <R> List<R> getWithList(Function<String, R> fn);

    /**
     * 根据条件分页查询
     *
     * @param entity
     * @param sqlCondition
     * @return
     */
    SplitPageVo<T> getPage(T entity, SqlCondition sqlCondition);

    /**
     * 自定义sql分页查询
     *
     * @param sqlCondition 查询条件
     * @param biFunction   查询业务方法（参数为封装好的page和tableName）
     * @param <R>
     * @return
     */
    <R extends BaseEntity> SplitPageVo<R> getPage(SqlCondition sqlCondition, BiFunction<Page<?>, String, List<R>> biFunction);

}
