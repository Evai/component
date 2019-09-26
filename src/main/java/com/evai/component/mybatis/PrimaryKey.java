package com.evai.component.mybatis;

import lombok.Data;

import java.io.Serializable;

/**
 * @author crh
 * @date 2019-09-23
 * @description 主键字段和值
 */
@Data
public class PrimaryKey {
    private String key;
    private Serializable value;
}
