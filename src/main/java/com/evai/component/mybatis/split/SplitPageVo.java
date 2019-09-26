package com.evai.component.mybatis.split;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author crh
 * @date 2019-08-16
 * @description 分表分页查询返回对象
 */
@Data
public class SplitPageVo<T> {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 排序字段名称
     */
    private String sortField;
    /**
     * 排序字段最后的值
     */
    private Serializable sortFieldLastValue;

    /**
     * 返回列表
     */
    private List<T> list;

    public static <T> SplitPageVo<T> empty() {
        SplitPageVo<T> splitPageVo = new SplitPageVo<>();
        splitPageVo.setId(0L);
        splitPageVo.setList(Collections.emptyList());
        return splitPageVo;
    }

    public static <T> SplitPageVo<T> list(Long pk, List<T> list) {
        SplitPageVo<T> splitPageVo = new SplitPageVo<>();
        splitPageVo.setId(pk);
        splitPageVo.setList(list);
        return splitPageVo;
    }

}
