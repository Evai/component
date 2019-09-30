package com.evai.component.demo.service;

import com.evai.component.demo.entity.User;
import com.evai.component.mybatis.split.BaseSplitTableService;

import java.util.List;

/**
 * @author crh
 * @date 2019-09-23
 * @description
 */
public interface UserService extends BaseSplitTableService<User> {

    List<User> getList();

    User cacheGetOneByUsername(String username);

}
