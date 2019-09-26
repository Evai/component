package com.evai.component.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * @author: Evai
 * @date: 2018/9/15
 */
@Data
public class PageVo<T> {

    /**
     * 返回列表
     */
    private List<T> list;

    /**
     * 每页显示条数
     */
    private long pageSize;

    /**
     * 当前页
     */
    private long currentPage;

    /**
     * 总记录数
     */
    private long totalCount;

    /**
     * 总页数
     */
    private long totalPage;

    private PageVo() {
    }

    public PageVo(IPage<T> iPage) {
        this.list = iPage.getRecords();
        this.pageSize = iPage.getSize();
        this.currentPage = iPage.getCurrent();
        this.totalCount = iPage.getTotal();
        this.totalPage = iPage.getPages();
    }

    public static <T> PageVo<T> setList(IPage<T> iPage) {
        return new PageVo<>(iPage);
    }

}
