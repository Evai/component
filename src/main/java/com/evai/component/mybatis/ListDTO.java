package com.evai.component.mybatis;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;

/**
 * @author: Evai
 * @date: 2018/9/15
 */
@Data
public class ListDTO {

    /**
     * 当前页
     */
    @Min(1)
    private Integer pageNum = 1;

    /**
     * 每页显示条数
     */
    @Range(min = 1, max = 100)
    private Integer pageSize = 10;

    /**
     * 升序字段数组
     */
    private String[] asc;

    /**
     * 降序字段数组
     */
    private String[] desc;

}
