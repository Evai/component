package com.evai.component.mybatis;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;

/**
 * @author crh
 * @date 2019-06-01
 * description 批量处理自定义sql执行器
 * 如果需要获取 Insert 的自增id，不要使用该类，使用默认的 SIMPLE 模式，该类只执行大批量相同的sql操作
 * 默认不开启事务，可配合事务执行，事务放在执行方法外层
 */
@Slf4j
@Component
@AllArgsConstructor
public class MybatisBatchExecutor {

    private final SqlSessionFactory sqlSessionFactory;

    public <T, E> void batch(Class<T> clazz, Collection<E> entityList, BiConsumer<T, E> biConsumer) {
        batch(clazz, entityList, 100, biConsumer);
    }

    /**
     * demo: mybatisBatchExecutor.batch(UserMapper.class, updateList, (UserMapper::updateById));
     *
     * @param clazz      BaseMapper类
     * @param entityList 批量执行的集合
     * @param batchSize  一次批量处理多少个sql
     * @param biConsumer 执行方法
     * @param <M>
     * @param <E>
     */
    public <M, E> void batch(Class<M> clazz, Collection<E> entityList, int batchSize, BiConsumer<M, E> biConsumer) {
        try (SqlSession batchSqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            int i = 0;
            for (Iterator<E> iter = entityList.iterator(); iter.hasNext(); ++i) {
                E entity = iter.next();
                biConsumer.accept(batchSqlSession.getMapper(clazz), entity);
                if (i >= 1 && i % batchSize == 0) {
                    batchSqlSession.flushStatements();
                }
            }
            batchSqlSession.flushStatements();
        }
    }

}
