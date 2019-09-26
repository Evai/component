package com.evai.component.mybatis.split;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author Evai
 * @date 2019-08-25
 * description
 */
@Data
@Accessors(chain = true)
public class SqlCondition {
    /**
     * 主键值
     */
    private Serializable id;
    /**
     * 排序字段
     */
    private String sortField;
    /**
     * 排序字段值
     */
    private Serializable sortFieldValue;
    /**
     * 查询指定字段
     */
    private String fieldSelect;
    /**
     * 是否升序，默认降序
     */
    private boolean isAsc;
    /**
     * 分页大小
     */
    private int pageSize;
    /**
     * 去重字段
     */
    private String distinct;
    /**
     * 分组字段
     */
    private String[] groupBy;

    public SqlCondition groupBy(String... groupBy) {
        this.groupBy = groupBy;
        return this;
    }

}
