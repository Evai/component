package com.evai.component.mybatis;

import java.util.function.Supplier;

/**
 * @author crh
 * @date 2019-05-17
 * @description 事务处理Service
 */
public interface TransactionService {

    /**
     * 如果当前有事务，在当前事务执行，否则开启一个新事务（有返回值）
     * <p>
     * transactionService.submitRequired(() -> {
     * inyUserMemberRecordService.insertBatch(insertList);
     * inyUserMemberService.updateBatchById(updateList);
     * return x;
     * });
     *
     * @param supplier
     * @param <T>
     * @return
     */
    <T> T submitRequired(Supplier<T> supplier);

    /**
     * 开启新事务（有返回值）
     *
     * @param supplier
     * @param <T>
     * @return
     */
    <T> T submitRequiresNew(Supplier<T> supplier);

    /**
     * 如果当前有事务，在当前事务执行，否则开启一个新事务（无返回值）
     * <p>
     * transactionService.executeRequired(() -> {
     * inyUserMemberRecordService.insertBatch(insertList);
     * inyUserMemberService.updateBatchById(updateList);
     * });
     *
     * @param runnable
     */
    void executeRequired(Runnable runnable);

    /**
     * 开启新事务（无返回值）
     *
     * @param runnable
     */
    void executeRequiresNew(Runnable runnable);

}
