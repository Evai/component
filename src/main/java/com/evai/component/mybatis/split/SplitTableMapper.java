package com.evai.component.mybatis.split;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author crh
 * @date 2019-08-16
 * @description 分库分表mapper
 */
@Mapper
public interface SplitTableMapper<T> {

    /**
     * 自定义更新sql语句
     *
     * @param sql
     */
    @UpdateProvider(type = SQLProvider.class, method = "customSQL")
    void updateSQL(@Param("sql") String sql);

    /**
     * 新增一条记录到指定的表
     *
     * @param entity
     */
    @InsertProvider(type = SQLProvider.class, method = "insert")
    void insert(@Param("tableName") String tableName, @Param("entity") T entity);

    /**
     * 更新一条记录到指定的表
     *
     * @param entity
     */
    @UpdateProvider(type = SQLProvider.class, method = "updateById")
    void updateById(@Param("tableName") String tableName, @Param("entity") T entity);

    /**
     * 删除一条记录到指定的表
     *
     * @param id
     */
    @DeleteProvider(type = SQLProvider.class, method = "deleteById")
    void deleteById(@Param("tableName") String tableName, @Param("id") Long id);

    /**
     * 统计数量
     *
     * @param tableName
     * @param entity
     * @return
     */
    @SelectProvider(type = SQLProvider.class, method = "count")
    int count(@Param("tableName") String tableName, @Param("entity") T entity);

    /**
     * 根据条件查询一条记录
     *
     * @param tableName
     * @param entity
     * @param sqlCondition
     * @return
     */
    @SelectProvider(type = SQLProvider.class, method = "getOne")
    T getOne(@Param("tableName") String tableName, @Param("entity") T entity, @Param("sqlCondition") SqlCondition sqlCondition);

    /**
     * 根据条件查询所有记录
     *
     * @param tableName
     * @param entity
     * @return
     */
    @SelectProvider(type = SQLProvider.class, method = "getList")
    List<T> getList(@Param("tableName") String tableName, @Param("entity") T entity, @Param("sqlCondition") SqlCondition sqlCondition);

    /**
     * 根据条件分页查询
     *
     * @param entity
     * @param sqlCondition
     * @return
     */
    @SelectProvider(type = SQLProvider.class, method = "getPage")
    List<T> getPage(@Param("tableName") String tableName, @Param("entity") T entity, @Param("sqlCondition") SqlCondition sqlCondition);

    /**
     * 如果不是根据主键排序，采用联合查询方式
     *
     * @param tableName
     * @param entity
     * @param sqlCondition
     * @return
     */
    @SelectProvider(type = SQLProvider.class, method = "getPageUnion")
    List<T> getPageUnion(@Param("tableName") String tableName, @Param("entity") T entity, @Param("sqlCondition") SqlCondition sqlCondition);

    /**
     * 查询指定表的总记录数
     *
     * @param tableName
     * @return
     */
    @Select("select count(*) from ${tableName}")
    int countTable(@Param("tableName") String tableName);

    /**
     * 查询指定库的指定表是否存在
     *
     * @param database
     * @param tableName
     * @return
     */
    @Select("SELECT count(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = #{database} AND TABLE_NAME = #{tableName}")
    int checkTable(@Param("database") String database, @Param("tableName") String tableName);

    /**
     * 查询指定表的列表
     *
     * @param database
     * @param tableName
     * @return
     */
    @Select("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = #{database} AND TABLE_NAME LIKE #{tableName} ORDER BY CREATE_TIME ASC")
    List<String> getTableList(@Param("database") String database, @Param("tableName") String tableName);

    /**
     * 查询表结构
     *
     * @param tableName
     * @return
     */
    @Select("SHOW CREATE TABLE ${tableName}")
    Map<String, String> showCreateTable(@Param("tableName") String tableName);

}
