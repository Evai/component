package com.evai.component.demo.service;

import com.evai.component.cache.annotation.CacheAbleEntity;
import com.evai.component.cache.enums.CacheAction;
import com.evai.component.demo.entity.User;
import com.evai.component.cache.annotation.CacheAble;
import com.evai.component.demo.dao.UserMapper;
import com.evai.component.mybatis.split.BaseSplitTableServiceImpl;
import com.evai.component.mybatis.split.SqlCondition;
import com.evai.component.utils.SleepUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author crh
 * @date 2019-09-23
 * @description
 */
@Service
public class UserServiceImpl extends BaseSplitTableServiceImpl<UserMapper, User> implements UserService {

    @Override
    @CacheAble(keyId = "list")
    public List<User> getList() {
        SleepUtil.seconds(1);
        return getPage(new User(), new SqlCondition().setPageSize(20)).getList();
    }

    @Override
    @CacheAbleEntity(keyId = "#username", action = CacheAction.SELECT)
    public User cacheGetOneByUsername(String username) {
        SleepUtil.seconds(1);
        return getOne(new User().setUsername(username));
    }


}
