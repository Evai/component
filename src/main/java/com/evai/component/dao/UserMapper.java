package com.evai.component.dao;

import com.evai.component.entity.User;
import com.evai.component.mybatis.split.SplitTableMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author crh
 * @date 2019-09-23
 * @description
 */
@Mapper
public interface UserMapper extends SplitTableMapper<User> {
}
