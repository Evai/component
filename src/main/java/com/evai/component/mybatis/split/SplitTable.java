package com.evai.component.mybatis.split;


import java.lang.annotation.*;

/**
 * @author crh
 * @date 2019-08-22
 * @description
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SplitTable {

    /**
     * 数据库名
     */
    String database();

    /**
     * 表名
     */
    String tableName();

    /**
     * 主键字段
     */
    String primaryKey() default "id";

    /**
     * 单表最大容量（总条数）
     */
    int tableMaxCapacity() default 5000000;

}
