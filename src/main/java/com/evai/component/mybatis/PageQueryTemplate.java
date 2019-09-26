package com.evai.component.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * @author: crh
 * @date: 2019-04-13
 * @description: 分页查询工具类
 */
public class PageQueryTemplate<T extends BaseEntity> {

    /**
     * 分页数
     */
    private int pageNo = 1;
    /**
     * 每页数量大小
     */
    private int pageSize = 200;

    public PageQueryTemplate() {
    }

    public PageQueryTemplate(int pageSize) {
        this.pageSize = pageSize;
    }

    public PageQueryTemplate(int pageNo, int pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }


    /**
     * 分页查询sql，然后将结果集汇总到一个数组再执行
     * <p>
     * List<User> list = new PageQueryTemplate<User>(100).getAll(page -> {
     * return UserService.selectPage(page, wrapper);
     * });
     * // 处理业务逻辑...
     *
     * @param fn sql执行语句
     */
    public List<T> getAll(UnaryOperator<Page<T>> fn) {
        // 总数组，分页查询的数据会添加到该数组
        List<T> totalList = new ArrayList<>();
        int pageNo = this.pageNo;
        int pageSize = this.pageSize;
        Page<T> page = new Page<>(pageNo, pageSize);
        for (; ; ) {
            page.setCurrent(pageNo);
            Page<T> tempList = fn.apply(page);
            if (CollectionUtils.isEmpty(tempList.getRecords())) {
                break;
            }
            totalList.addAll(tempList.getRecords());
            pageNo++;
        }
        return totalList;
    }

    /**
     * 分页查询sql，直接执行查询的分页数组业务逻辑
     * <p>
     * new PageQueryTemplate<User>(100).runPage(page -> {
     * return UserService.selectPage(page);
     * }, list -> {
     * // 处理业务逻辑...
     * });
     *
     * @param fn       sql执行语句
     * @param consumer 执行查询的数组逻辑
     */
    public void runPage(UnaryOperator<Page<T>> fn, Consumer<List<T>> consumer) {
        int pageNo = this.pageNo;
        int pageSize = this.pageSize;
        Page<T> page = new Page<>(pageNo, pageSize);
        for (; ; ) {
            page = fn.apply(page);
            if (CollectionUtils.isEmpty(page.getRecords())) {
                break;
            }
            consumer.accept(page.getRecords());
            page.setCurrent(pageNo++);
        }
    }

    /**
     * 根据主键分页查询sql，直接执行查询的分页数组业务逻辑（返回主键）
     * <p>
     * new PageQueryTemplate<User>(100).runPageByPk((page, id) -> {
     * return UserService.getPageByGtPk(page, wrapper, id);
     * }, list -> {
     * // 处理业务逻辑...
     * });
     *
     * @param fn       sql执行语句
     * @param consumer 执行查询的数组逻辑
     */
    public void runPageByPk(BiFunction<IPage<?>, Number, IPage<T>> fn, Consumer<List<?>> consumer) {
        int pageNo = this.pageNo;
        int pageSize = this.pageSize;
        IPage<T> page = new Page<>(pageNo, pageSize);
        Number pk = 0;
        for (; ; ) {
            page = fn.apply(page, pk);
            if (CollectionUtils.isEmpty(page.getRecords())) {
                break;
            }
            consumer.accept(page.getRecords());
            int lastIndex = page
                    .getRecords()
                    .size() - 1;
            pk = page
                    .getRecords()
                    .get(lastIndex)
                    .getId();
        }
    }

    public static int offsetPageNo(int pageNo, int size) {
        return pageNo > 0 ? (pageNo - 1) * size : 0;
    }

}
