package com.evai.component.mybatis.split;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.evai.component.mybatis.BaseEntity;
import com.evai.component.mybatis.PrimaryKey;
import com.evai.component.mybatis.utils.ReflectUtil;
import com.evai.component.utils.BeanUtil;
import com.google.common.base.CaseFormat;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author crh
 * @date 2019-08-20
 * @description
 */
public class SQLProvider extends SQL {

    private final static String PK = "id";

    /**
     * 获取实体类主键字段名称
     *
     * @param entity
     * @return
     */
    public String getPKField(Object entity) {
        PrimaryKey primaryKey = ReflectUtil.getPrimaryKey(entity);
        return primaryKey.getKey();
    }

    public String customSQL(@Param("sql") String sql) {
        return new SQL().toString() + sql;
    }

    public <T extends BaseEntity> String insert(@Param("tableName") String tableName, @Param("entity") T entity) {
        return new SQL() {{
            INSERT_INTO(tableName);
            List<Field> fieldList = BeanUtil.getAllFields(entity.getClass());
            for (Field field : fieldList) {
                String fieldName = field.getName();
                field.setAccessible(true);
                Object value = getFieldValue(field, entity);
                // sql字段下划线
                String sqlField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
                if (!ObjectUtils.isEmpty(value)) {
                    VALUES(sqlField, String.format("#{entity.%s}", fieldName));
                }
            }
        }}.toString();
    }

    public <T extends BaseEntity> String updateById(@Param("tableName") String tableName, @Param("entity") T entity) {
        return new SQL() {{
            UPDATE(tableName);
            List<Field> fieldList = BeanUtil.getAllFields(entity.getClass());
            for (Field field : fieldList) {
                String fieldName = field.getName();
                Object value = getFieldValue(field, entity);
                // sql字段下划线
                String sqlField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
                if (!ObjectUtils.isEmpty(value) && !PK.equals(sqlField)) {
                    SET(String.format("%s = #{entity.%s}", sqlField, fieldName));
                }
            }
            WHERE("id = #{entity.id}");
        }}.toString();
    }

    public String deleteById(@Param("tableName") String tableName, @Param("id") Long id) {
        return new SQL() {{
            DELETE_FROM(tableName);
            WHERE("id = #{id}");
        }}.toString();
    }

    public <T extends BaseEntity> String getOne(@Param("tableName") String tableName, @Param("entity") T entity, @Param("sqlCondition") SqlCondition sqlCondition) {
        return getList(tableName, entity, sqlCondition) + " limit 0, 1";
    }

    public <T extends BaseEntity> String count(@Param("tableName") String tableName, @Param("entity") T entity) {
        return new SQL() {{
            SELECT("count(*)");
            List<Field> fieldList = BeanUtil.getAllFields(entity.getClass());
            for (Field field : fieldList) {
                String fieldName = field.getName();
                // sql字段下划线
                String sqlField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
                Object value = getFieldValue(field, entity);
                wrapperCondition(this.getSelf(), field, fieldName, sqlField, value);
            }
            FROM(tableName);
        }}.toString();
    }

    public <T extends BaseEntity> String getPage(@Param("tableName") String tableName, @Param("entity") T entity, @Param("sqlCondition") SqlCondition sqlCondition) {
        if (sqlCondition == null) {
            sqlCondition = new SqlCondition();
        }
        SqlCondition finalSqlCondition = sqlCondition;
        return new SQL() {{
            String[] fieldSelects = null;
            if (!StringUtils.isEmpty(finalSqlCondition.getFieldSelect())) {
                fieldSelects = finalSqlCondition.getFieldSelect().split(",");
            }
            List<Field> fieldList = BeanUtil.getAllFields(entity.getClass());
            for (Field field : fieldList) {
                String fieldName = field.getName();
                // sql字段下划线
                String sqlField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
                wrapperFieldSelect(this.getSelf(), fieldSelects, sqlField, fieldName);
                if (equalsStr(finalSqlCondition.getSortField(), fieldName)) {
                    if (finalSqlCondition.isAsc()) {
                        if (!ObjectUtils.isEmpty(finalSqlCondition.getSortFieldValue())) {
                            WHERE(String.format("%s > #{sqlCondition.sortFieldValue}", sqlField));
                        }
                        ORDER_BY(String.format("%s ASC", finalSqlCondition.getSortField()));
                    } else {
                        if (!ObjectUtils.isEmpty(finalSqlCondition.getSortFieldValue())) {
                            WHERE(String.format("%s < #{sqlCondition.sortFieldValue}", sqlField));
                        }
                        ORDER_BY(String.format("%s DESC", finalSqlCondition.getSortField()));
                    }
                    if (!PK.equals(fieldName)) {
                        ORDER_BY("id ASC");
                    }
                } else if (!PK.equals(fieldName)) {
                    Object value = getFieldValue(field, entity);
                    wrapperCondition(this.getSelf(), field, fieldName, sqlField, value);
                }
            }
            FROM(tableName);
        }}.toString() + String.format(" limit 0, %d", sqlCondition.getPageSize());
    }

    /**
     * 如果不是根据主键排序，采用联合查询方式
     *
     * @param entity
     * @param sqlCondition
     * @param <T>
     * @return
     */
    public <T extends BaseEntity> String getPageUnion(@Param("tableName") String tableName, @Param("entity") T entity, @Param("sqlCondition") SqlCondition sqlCondition) {
        if (sqlCondition == null) {
            sqlCondition = new SqlCondition();
        }
        SqlCondition finalSqlCondition = sqlCondition;
        return new SQL() {{
            String[] fieldSelects = null;
            if (!StringUtils.isEmpty(finalSqlCondition.getFieldSelect())) {
                fieldSelects = finalSqlCondition.getFieldSelect().split(",");
            }
            List<Field> fieldList = BeanUtil.getAllFields(entity.getClass());
            for (Field field : fieldList) {
                String fieldName = field.getName();
                // sql字段下划线
                String sqlField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
                wrapperFieldSelect(this.getSelf(), fieldSelects, sqlField, fieldName);
                if (PK.equals(fieldName)) {
                    if (!ObjectUtils.isEmpty(finalSqlCondition.getId())) {
                        WHERE("id > #{sqlCondition.id}");
                    }
                } else if (equalsStr(finalSqlCondition.getSortField(), fieldName)) {
                    if (!ObjectUtils.isEmpty(finalSqlCondition.getSortFieldValue())) {
                        WHERE(String.format("%s = #{sqlCondition.sortFieldValue}", sqlField));
                    }
                } else {
                    Object value = getFieldValue(field, entity);
                    wrapperCondition(this.getSelf(), field, fieldName, sqlField, value);
                }
            }
            FROM(tableName);
        }}.toString() + " UNION ALL " + getPage(tableName, entity, sqlCondition);
    }

    private SQL wrapperFieldSelect(SQL sql, String[] fieldSelects, String sqlField, String fieldName) {
        if (fieldSelects != null && fieldSelects.length > 0) {
            for (String s : fieldSelects) {
                if (sqlField.equals(s)) {
                    sql.SELECT(String.format("%s AS %s", sqlField, fieldName));
                    break;
                }
            }
        } else {
            sql.SELECT(String.format("%s AS %s", sqlField, fieldName));
        }
        return sql;
    }

    public <T extends BaseEntity> String getList(@Param("tableName") String tableName, @Param("entity") T entity, @Param("sqlCondition") SqlCondition sqlCondition) {
        if (sqlCondition == null) {
            sqlCondition = new SqlCondition();
        }
        SqlCondition finalSqlCondition = sqlCondition;
        return new SQL() {{
            List<Field> fieldList = BeanUtil.getAllFields(entity.getClass());
            if (!StringUtils.isEmpty(finalSqlCondition.getDistinct())) {
                for (Field field : fieldList) {
                    String fieldName = field.getName();
                    // sql字段下划线
                    String sqlField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
                    if (sqlField.equals(finalSqlCondition.getDistinct())) {
                        SELECT(String.format("DISTINCT %s AS %s", sqlField, fieldName));
                    }
                    Object value = getFieldValue(field, entity);
                    wrapperCondition(this.getSelf(), field, fieldName, sqlField, value);
                }
            } else {
                for (Field field : fieldList) {
                    String fieldName = field.getName();
                    // sql字段下划线
                    String sqlField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
                    SELECT(String.format("%s AS %s", sqlField, fieldName));
                    Object value = getFieldValue(field, entity);
                    wrapperCondition(this.getSelf(), field, fieldName, sqlField, value);
                }
            }
            if (!StringUtils.isEmpty(finalSqlCondition.getGroupBy())) {
                GROUP_BY(finalSqlCondition.getGroupBy());
            }
            FROM(tableName);
        }}.toString();
    }

    private SQL wrapperCondition(SQL sql, Field field, String fieldName, String sqlField, Object value) {
        TableLogic tableLogic = field.getDeclaredAnnotation(TableLogic.class);
        if (tableLogic != null) {
            sql.WHERE(String.format("%s = %d", sqlField, Integer.valueOf(tableLogic.value())));
        } else if (!ObjectUtils.isEmpty(value)) {
            sql.WHERE(String.format("%s = #{entity.%s}", sqlField, fieldName));
        }
        return sql;
    }

    private <T extends BaseEntity> Object getFieldValue(Field field, T entity) {
        field.setAccessible(true);
        Object value = null;
        try {
            value = field.get(entity);
        } catch (IllegalAccessException e) {
            //
        }
        return value;
    }

    private boolean isTrue(Boolean b) {
        return Boolean.TRUE.equals(b);
    }

    private boolean equalsStr(String str1, String str2) {
        return str1 != null && str1.equals(str2);
    }
}
