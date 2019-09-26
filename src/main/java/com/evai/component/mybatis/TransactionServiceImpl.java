package com.evai.component.mybatis;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.function.Supplier;

/**
 * @author crh
 * @date 2019-05-17
 * @description 事务处理Service
 */
@Service
@AllArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final DataSourceTransactionManager transactionManager;

    @Override
    public <T> T submitRequiresNew(Supplier<T> supplier) {
        return submit(supplier, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public <T> T submitRequired(Supplier<T> supplier) {
        return submit(supplier, TransactionDefinition.PROPAGATION_REQUIRED);
    }


    @Override
    public void executeRequiresNew(Runnable runnable) {
        execute(runnable, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void executeRequired(Runnable runnable) {
        execute(runnable, TransactionDefinition.PROPAGATION_REQUIRED);
    }

    /**
     * 处理事务，返回值
     *
     * @param supplier
     * @param propagationBehavior
     * @param <T>
     * @return
     */
    private <T> T submit(Supplier<T> supplier, int propagationBehavior) {
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setPropagationBehavior(propagationBehavior);
        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);
        try {
            T result = supplier.get();
            transactionManager.commit(transactionStatus);
            return result;
        } catch (Exception e) {
            transactionManager.rollback(transactionStatus);
            throw e;
        }
    }

    /**
     * 处理事务，不返回值
     *
     * @param runnable
     * @param propagationBehavior
     */
    private void execute(Runnable runnable, int propagationBehavior) {
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setPropagationBehavior(propagationBehavior);
        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);
        try {
            runnable.run();
            transactionManager.commit(transactionStatus);
        } catch (Exception e) {
            transactionManager.rollback(transactionStatus);
            throw e;
        }
    }

}
