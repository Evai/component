package com.evai.component.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author: crh
 * @date: 2019/3/27
 * @description:
 */
public interface BaseService<T> extends IService<T> {

    default PageVo<T> getPage(Page<T> page, QueryWrapper<T> wrapper) {
        IPage<T> iPage = page(page, wrapper);
        return PageVo.setList(iPage);
    }

}
