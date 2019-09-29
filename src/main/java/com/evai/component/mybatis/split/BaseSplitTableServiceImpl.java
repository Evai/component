package com.evai.component.mybatis.split;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evai.component.mybatis.BaseEntity;
import com.evai.component.mybatis.MybatisBatchExecutor;
import com.evai.component.mybatis.TransactionService;
import com.evai.component.utils.BeanUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author crh
 * @date 2019-05-30
 * @description 分库分表抽象类
 */
@Slf4j
public abstract class BaseSplitTableServiceImpl<M extends SplitTableMapper<T>, T extends BaseEntity> implements BaseSplitTableService<T> {

    @Autowired
    protected MybatisBatchExecutor mybatisBatchExecutor;
    @Autowired
    protected TransactionService transactionService;
    @Autowired
    protected M baseMapper;

    /**
     * 分表分割符号
     *
     * @return String
     */
    protected String getSplit() {
        return "_split_";
    }

    /**
     * 缓存所有主表对应的分表列表
     */
    private final static Map<String, SoftReference<List<String>>> SOFT_REFERENCE_MAP = new ConcurrentHashMap<>();

    /**
     * 数据库名称
     */
    private String getDatabase() {
        return getSplitTable().database();
    }

    /**
     * 单表最大数据量
     */
    private int getMaxTableCount() {
        return getSplitTable().tableMaxCapacity();
    }

    /**
     * 实体类对应的表名
     */
    private String getBaseTableName() {
        return getSplitTable().tableName();
    }

    /**
     * 主键
     */
    private String getPrimaryKey() {
        return getSplitTable().primaryKey();
    }

    private SplitTable getSplitTable() {
        Class<T> clz = getEntityClass();
        SplitTable splitTable = clz.getDeclaredAnnotation(SplitTable.class);
        if (splitTable == null) {
            throw new RuntimeException("@SplitTable annotation not found for entity class");
        }
        return splitTable;
    }

    @Override
    public boolean insert(T entity) {
        return insertBatch(Collections.singletonList(entity));
    }

    @Override
    public boolean insertBatch(List<T> entityList) {
        return insertBatch(entityList, 100);
    }

    @Override
    public boolean insertBatch(List<T> entityList, int batchSize) {
        if (CollectionUtils.isEmpty(entityList)) {
            return false;
        }
        for (T entity : entityList) {
            entity.setId(IdWorker.getId());
        }
        BiFunction<String, List<T>, Boolean> fn = (table, list) -> {
            transactionService.executeRequired(() -> mybatisBatchExecutor.batch(getMapperClass(), list, batchSize, (m, e) -> m.insert(table, e)));
            return true;
        };
        return checkAndCreateTable(entityList, fn);
    }

    @Override
    public boolean updateById(T entity) {
        return updateBatchById(Collections.singletonList(entity));
    }

    @Override
    public boolean updateBatchById(List<T> entityList) {
        return updateBatchById(entityList, 100);
    }

    @Override
    public boolean updateBatchById(List<T> entityList, int batchSize) {
        if (CollectionUtils.isEmpty(entityList)) {
            return false;
        }
        Function<Map<String, List<T>>, Boolean> fn = (map) -> {
            for (Map.Entry<String, List<T>> entry : map.entrySet()) {
                transactionService.executeRequired(() -> mybatisBatchExecutor.batch(getMapperClass(), entry.getValue(), batchSize, (m, e) -> m.updateById(entry.getKey(), e)));
            }
            return true;
        };
        return operateBatchById(entityList, fn);
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null || id <= 0) {
            return false;
        }
        List<Long> tableIdList = getTableIdList();
        String routeTableName = routeTableNameById(tableIdList, id);
        baseMapper.deleteById(routeTableName, id);
        return true;
    }

    @Override
    public boolean deleteBatchById(Collection<Long> ids) {
        return deleteBatchById(ids, 100);
    }

    @Override
    public boolean deleteBatchById(Collection<Long> ids, int batchSize) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        Function<Map<String, Set<Long>>, Boolean> fn = (map) -> {
            for (Map.Entry<String, Set<Long>> entry : map.entrySet()) {
                mybatisBatchExecutor.batch(getMapperClass(), entry.getValue(), batchSize, (m, e) -> m.deleteById(entry.getKey(), e));
            }
            return true;
        };
        List<Long> tableIdList = getTableIdList();
        Map<String, Set<Long>> map = Maps.newHashMapWithExpectedSize(tableIdList.size());
        for (Long id : ids) {
            if (id != null && id <= 0L) {
                throw new RuntimeException("id必须大于0");
            }
            String routeTableName = routeTableNameById(tableIdList, id);
            Set<Long> set = map.get(routeTableName);
            if (set == null) {
                set = new HashSet<>();
            }
            set.add(id);
            map.put(routeTableName, set);
        }
        return fn.apply(map);
    }

    private boolean operateBatchById(Collection<T> entityList, Function<Map<String, List<T>>, Boolean> fn) {
        List<Long> tableIdList = getTableIdList();
        Map<String, List<T>> map = Maps.newHashMapWithExpectedSize(tableIdList.size());
        for (T entity : entityList) {
            Long id = entity.getId();
            if (id != null && id <= 0L) {
                throw new RuntimeException("id必须大于0");
            }
            String routeTableName = routeTableNameById(tableIdList, id);
            List<T> list = map.get(routeTableName);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(entity);
            map.put(routeTableName, list);
        }
        return fn.apply(map);
    }

    private boolean checkAndCreateTable(List<T> entityList, BiFunction<String, List<T>, Boolean> fn) {
        long id = entityList.get(0).getId();
        List<Long> tableIdList = getTableIdList();
        String routeTableName = routeTableNameById(tableIdList, id);
        int count = countForTable(routeTableName);
        // 当前表剩余容量
        int retainCount = getMaxTableCount() - count;
        return createAndInsertBatch(entityList, retainCount, routeTableName, fn);
    }

    private boolean createAndInsertBatch(List<T> entityList, int retainCount, String routeTableName, BiFunction<String, List<T>, Boolean> fn) {
        if (retainCount <= 0) {
            // 如果当前表容量不足，新建表
            routeTableName = getBaseTableName() + getSplit() + entityList.get(0).getId();
            if (!isExistForTable(routeTableName)) {
                createTable(routeTableName);
            }
            return createAndInsertBatch(entityList, getMaxTableCount(), routeTableName, fn);
        }
        // 如果当前列表数量大于表的剩余容量，先把列表前部分数据插入到旧表，再创建新表插入余下的数据
        if (entityList.size() > retainCount) {
            List<T> firstList = entityList.subList(0, retainCount);
            // 插入旧表
            fn.apply(routeTableName, firstList);
            long id = entityList.get(retainCount).getId();
            routeTableName = getBaseTableName() + getSplit() + id;
            if (!isExistForTable(routeTableName)) {
                createTable(routeTableName);
            }
            // 剩余列表
            List<T> retainList = entityList.subList(retainCount, entityList.size());
            return createAndInsertBatch(retainList, getMaxTableCount(), routeTableName, fn);
        }
        return fn.apply(routeTableName, entityList);
    }

    private boolean isExistForTable(String tableName) {
        int count = baseMapper.checkTable(getDatabase(), tableName);
        return count > 0;
    }

    private int countForTable(String tableName) {
        return baseMapper.countTable(getDatabase() + "." + tableName);
    }

    private String showCreateTable() {
        Map<String, String> map = baseMapper.showCreateTable(getBaseTableName());
        return map.get("Create Table");
    }

    private void createTable(String tableName) {
        String sql = showCreateTable();
        sql = sql.replace(getBaseTableName(), tableName);
        baseMapper.updateSQL(sql);
        List<String> tableList = getTableList();
        tableList.add(tableName);
        SOFT_REFERENCE_MAP.put(getBaseTableName(), new SoftReference<>(tableList));
    }

    private List<String> getTableList() {
        SoftReference<List<String>> softReference = SOFT_REFERENCE_MAP.get(getBaseTableName());
        List<String> tableList;
        if (softReference == null || (tableList = softReference.get()) == null) {
            tableList = Lists.newArrayList(getBaseTableName());
            tableList.addAll(baseMapper.getTableList(getDatabase(), getBaseTableName() + getSplit() + "%"));
            SOFT_REFERENCE_MAP.put(getBaseTableName(), new SoftReference<>(tableList));
        }
        return tableList;
    }

    @Override
    public T getOne(T entity, SqlCondition sqlCondition) {
        if (entity == null) {
            return null;
        }
        List<String> tableList = getTableList();
        List<T> list = new CopyOnWriteArrayList<>();
        tableList.parallelStream()
                .forEach(s -> {
                    list.add(this.baseMapper.getOne(s, entity, sqlCondition));
                });
        if (list.size() < 1) {
            return null;
        }
        return list.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparing(BaseEntity::getId))
                .orElse(null);
    }

    @Override
    public T getOne(T entity) {
        return getOne(entity, null);
    }

    @Override
    public T getOneById(Long id, SqlCondition sqlCondition) {
        if (id == null || id <= 0) {
            return null;
        }
        Class<T> clz = getEntityClass();
        try {
            T entity = clz.newInstance();
            entity.setId(id);
            List<Long> tableIdList = getTableIdList();
            String routeTableName = routeTableNameById(tableIdList, entity.getId());
            return this.baseMapper.getOne(routeTableName, entity, sqlCondition);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("getOneById error", e);
            return null;
        }
    }

    @Override
    public T getOneById(Long id) {
        return getOneById(id, null);
    }

    @SuppressWarnings("unchecked")
    private Class<M> getMapperClass() {
        return (Class<M>) ((ParameterizedType) (this.getClass().getGenericSuperclass())).getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked")
    private Class<T> getEntityClass() {
        return (Class<T>) ((ParameterizedType) (this.getClass().getGenericSuperclass())).getActualTypeArguments()[1];
    }

    @Override
    public List<T> getList(@Param("entity") T entity, SqlCondition sqlCondition) {
        if (entity == null) {
            return Collections.emptyList();
        }
        Function<String, List<T>> fn = (tableName) -> this.baseMapper.getList(tableName, entity, sqlCondition);
        return getList(fn);
    }

    @Override
    public List<T> getList(@Param("entity") T entity) {
        return getList(entity, null);
    }

    @Override
    public <R> List<R> getList(Function<String, List<R>> fn) {
        if (fn == null) {
            return Collections.emptyList();
        }
        List<List<R>> lists = this.getWithList(fn);
        List<R> list = new ArrayList<>();
        for (List<R> ts : lists) {
            list.addAll(ts);
        }
        return list;
    }

    @Override
    public <R> List<R> getWithList(Function<String, R> fn) {
        if (fn == null) {
            return Collections.emptyList();
        }
        List<String> tableList = getTableList();
        return tableList.parallelStream()
                .map(fn)
                .collect(Collectors.toList());
    }

    @Override
    public int count(T entity) {
        if (entity == null) {
            return 0;
        }
        List<String> tableList = getTableList();
        AtomicInteger count = new AtomicInteger();
        tableList.parallelStream()
                .forEach(s -> count.addAndGet(this.baseMapper.count(s, entity)));
        return count.get();
    }

    @Override
    public int count(Function<String, Integer> fn) {
        if (fn == null) {
            return 0;
        }
        List<String> tableList = getTableList();
        AtomicInteger count = new AtomicInteger();
        tableList.parallelStream()
                .forEach(s -> count.addAndGet(fn.apply(s)));
        return count.get();
    }

    @Override
    public SplitPageVo<T> getPage(T entity, SqlCondition sqlCondition) {
        if (entity == null || sqlCondition.getPageSize() <= 0) {
            return SplitPageVo.empty();
        }
        return getPage(sqlCondition, ((page, tableName) -> {
            if (sqlCondition.getSortField() == null || sqlCondition.getSortField().length() == 0) {
                sqlCondition.setSortField(getPrimaryKey());
            }
            if (sqlCondition.getSortField().equals(getPrimaryKey()) || sqlCondition.getSortFieldValue() == null) {
                return baseMapper.getPage(tableName, entity, sqlCondition);
            }
            return baseMapper.getPageUnion(tableName, entity, sqlCondition);
        }));
    }

    /**
     * @param sqlCondition
     * @param biFunction
     * @param <R>
     * @return
     */
    @Override
    public <R extends BaseEntity> SplitPageVo<R> getPage(SqlCondition sqlCondition, BiFunction<Page<?>, String, List<R>> biFunction) {
        if (biFunction == null || sqlCondition.getPageSize() <= 0) {
            return SplitPageVo.empty();
        }

        List<Long> tableIdList = getTableIdList();
        String tableName;
        if (getPrimaryKey().equals(sqlCondition.getSortField()) && sqlCondition.getSortFieldValue() instanceof Long) {
            tableName = routeTableNameById(tableIdList, (Long) sqlCondition.getSortFieldValue());
        } else {
            tableName = getBaseTableName();
        }
        List<String> tableList = getTableList();
        Page<?> page = new Page<>(1, sqlCondition.getPageSize());
        // 如果不是主表，直接从该表后面的所有表查询
        if (!tableName.equals(getBaseTableName())) {
            for (int i = 0; i < tableList.size(); i++) {
                if (tableList.get(i).equals(tableName)) {
                    tableList = tableList.subList(i, tableList.size());
                    break;
                }
            }
        }

        List<R> list = new CopyOnWriteArrayList<>();
        tableList.parallelStream()
                .forEach(s -> list.addAll(biFunction.apply(page, s)));
        return sortSplitPageVo(list, sqlCondition);
    }

    /**
     * 设置主键id，返回参数排序并封装
     *
     * @param list
     * @param <R>
     * @return
     */
    @SuppressWarnings("unchecked")
    private <R extends BaseEntity> SplitPageVo<R> sortSplitPageVo(List<R> list, SqlCondition sqlCondition) {
        if (CollectionUtils.isEmpty(list)) {
            return SplitPageVo.empty();
        }
        SplitPageVo<R> splitPageVo = new SplitPageVo<>();
        // 排序
        list.sort((o1, o2) -> {
            Class<?> clz1 = o1.getClass();
            if (sqlCondition.getSortField() == null || sqlCondition.getSortField().equals(getPrimaryKey())) {
                if (sqlCondition.isAsc()) {
                    return o1.getId().compareTo(o2.getId());
                }
                return o2.getId().compareTo(o1.getId());
            }
            try {
                Field field = getField(clz1, sqlCondition.getSortField());
                field.setAccessible(true);
                Object value1 = field.get(o1);
                Object value2 = field.get(o2);
                if (value1 instanceof Comparable) {
                    int r;
                    if (sqlCondition.isAsc()) {
                        r = ((Comparable) value1).compareTo(value2);
                    } else {
                        r = ((Comparable) value2).compareTo(value1);
                    }
                    if (r == 0) {
                        return o1.getId().compareTo(o2.getId());
                    }
                    return r;
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                //
            }
            return 0;
        });
        if (list.size() > sqlCondition.getPageSize()) {
            list = list.subList(0, sqlCondition.getPageSize());
        }
        splitPageVo.setList(list);
        splitPageVo.setSortField(sqlCondition.getSortField());
        R entity = list.get(list.size() - 1);
        wrapperSplitPageVo(entity, splitPageVo, sqlCondition.getSortField());
        return splitPageVo;
    }

    private Field getField(Class clz, String fieldName) throws NoSuchFieldException {
        List<Field> fieldList = BeanUtil.getAllFields(clz);
        for (Field field : fieldList) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        throw new NoSuchFieldException("not found field with fieldName: " + fieldName);
    }

    /**
     * 获取类中主键字段
     *
     * @param entity
     * @return
     */
    private <R> void wrapperSplitPageVo(R entity, SplitPageVo<?> splitPageVo, String sortField) {
        if (entity == null) {
            return;
        }
        List<Field> fieldList = BeanUtil.getAllFields(entity.getClass());
        for (Field field : fieldList) {
            if (field.getName().equals(getPrimaryKey())) {
                splitPageVo.setId((Long) getFieldValue(entity, field));
            }
            if (field.getName().equals(sortField)) {
                Object value = getFieldValue(entity, field);
                splitPageVo.setSortFieldLastValue((Serializable) value);
            }
        }
    }

    private Object getFieldValue(Object obj, Field field) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 获取表的后缀id
     *
     * @param tableName
     * @return
     */
    private Long getTableNameSuffixId(String tableName) {
        long tableNameSuffixId = 0L;
        try {
            tableNameSuffixId = Long.valueOf(tableName.substring(tableName.lastIndexOf(getSplit()) + getSplit().length()));
        } catch (NumberFormatException e) {
            // main table
        }
        return tableNameSuffixId;
    }

    /**
     * 获取默认表名的所有后缀id列表，升序
     *
     * @return
     */
    private List<Long> getTableIdList() {
        List<String> tableList = getTableList();
        if (CollectionUtils.isEmpty(tableList)) {
            return Collections.emptyList();
        }
        return tableList.stream()
                .map(this::getTableNameSuffixId)
                .filter(e -> e > 0)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 路由方法，查询指定主键存在哪张表，并获取该表的表名
     *
     * @param tableIdList 所有表名后缀id列表
     * @param id          当前主键
     * @return
     */
    private String routeTableNameById(List<Long> tableIdList, Long id) {
        // 默认表名
        String tableName = getBaseTableName();
        if (CollectionUtils.isEmpty(tableIdList) || id == null || id <= 0) {
            return tableName;
        }
        int lastIndex = -1;
        for (int i = 0; i < tableIdList.size(); i++) {
            if (id + 1 < tableIdList.get(i)) {
                // 如果已经不是第一个元素，取上一张表，否则直接取默认表名
                if (i - 1 >= 0) {
                    tableName = assembleTableName(tableIdList, i - 1);
                }
                break;
            }
            // 如果id比最后一张表id还大，返回最后一张表
            if (i == tableIdList.size() - 1) {
                lastIndex = i;
            }
        }

        if (lastIndex > -1) {
            tableName = assembleTableName(tableIdList, lastIndex);
        }
        return tableName;
    }

    private String assembleTableName(List<Long> tableIdList, int index) {
        return getBaseTableName() + getSplit() + tableIdList.get(index);
    }

}
