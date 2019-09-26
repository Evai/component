package com.evai.component.mybatis.utils;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evai.component.mybatis.BaseEntity;
import com.evai.component.mybatis.PrimaryKey;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @author crh
 * @date 2019-09-23
 * @description
 */
public class ReflectUtil {

    private static final String PK = "id";

    public static <T> PrimaryKey getPrimaryKey(T entity) {
        PrimaryKey primaryKey = new PrimaryKey();
        if (entity == null) {
            return primaryKey;
        }
        Class clazz = entity.getClass();
        if (BaseEntity.class.equals(clazz.getSuperclass())) {
            BaseEntity baseEntity = (BaseEntity) entity;
            primaryKey.setKey(PK);
            primaryKey.setValue(baseEntity.getId());
        } else if (clazz.getDeclaredAnnotation(TableName.class) != null) {
            Field field = Arrays.stream(clazz.getDeclaredFields())
                    .filter(e -> e.getDeclaredAnnotation(TableId.class) != null)
                    .findFirst()
                    .orElse(null);
            if (field != null) {
                primaryKey.setKey(field.getName());
                field.setAccessible(true);
                try {
                    primaryKey.setValue((Serializable) field.get(entity));
                } catch (IllegalAccessException e) {
                    //
                }
            }
        }
        return primaryKey;
    }

    public static <T> Serializable getPrimaryValue(T entity) {
        PrimaryKey primaryKey = getPrimaryKey(entity);
        return primaryKey.getValue();
    }

}
