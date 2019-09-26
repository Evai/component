package com.evai.component.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evai.component.mybatis.BaseEntity;
import com.evai.component.mybatis.split.SplitTable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 *
 * </p>
 *
 * @author crh
 * @since 2019-04-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_user")
@SplitTable(database = "test_db", tableName = "t_user", tableMaxCapacity = 100)
public class User extends BaseEntity {

    private String username;

    private String phone;

    private Integer gender;

    private String password;

    private String salt;

    private Integer age;

}
