package com.batchweaver.core.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 事务日志工具类
 * <p>
 * 用于打印事务的生命周期事件，帮助调试多数据源事务问题
 *
 * @author BatchWeaver Team
 * @since 1.0.0
 */
public class TransactionLogger {

    private static final Logger log = LoggerFactory.getLogger(TransactionLogger.class);
    private static final AtomicLong TRANSACTION_COUNTER = new AtomicLong(0);

    /**
     * 事务上下文信息
     */
    public static class TransactionContext {
        private final long transactionId;
        private final String transactionName;
        private final long threadId;
        private final String threadName;
        private final long startTime;

        public TransactionContext(String transactionName) {
            this.transactionId = TRANSACTION_COUNTER.incrementAndGet();
            this.transactionName = transactionName;
            this.threadId = Thread.currentThread().getId();
            this.threadName = Thread.currentThread().getName();
            this.startTime = System.currentTimeMillis();
        }

        public long getTransactionId() {
            return transactionId;
        }

        public String getTransactionName() {
            return transactionName;
        }

        public long getThreadId() {
            return threadId;
        }

        public String getThreadName() {
            return threadName;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * 记录事务开始
     */
    public static TransactionContext logTransactionStart(String transactionName, TransactionDefinition definition) {
        TransactionContext context = new TransactionContext(transactionName);

        log.info("================================================================================");
        log.info("🔄 [事务开始] ID: {}, 名称: {}, 线程: [{}-{}]",
                context.getTransactionId(),
                context.getTransactionName(),
                context.getThreadId(),
                context.getThreadName());
        log.info("   传播行为: {}, 隔离级别: {}, 只读: {}",
                getPropagationName(definition.getPropagationBehavior()),
                getIsolationLevelName(definition.getIsolationLevel()),
                definition.isReadOnly());
        log.info("   活跃事务数: {}", TransactionSynchronizationManager.isActualTransactionActive());
        log.info("================================================================================");

        return context;
    }

    /**
     * 记录事务提交
     */
    public static void logTransactionCommit(TransactionContext context) {
        log.info("================================================================================");
        log.info("✅ [事务提交] ID: {}, 名称: {}, 耗时: {}ms",
                context.getTransactionId(),
                context.getTransactionName(),
                context.getElapsedTime());
        log.info("================================================================================");
    }

    /**
     * 记录事务回滚
     */
    public static void logTransactionRollback(TransactionContext context, Throwable cause) {
        log.warn("================================================================================");
        log.warn("❌ [事务回滚] ID: {}, 名称: {}, 耗时: {}ms",
                context.getTransactionId(),
                context.getTransactionName(),
                context.getElapsedTime());
        if (cause != null) {
            log.warn("   回滚原因: {}", cause.getMessage());
        }
        log.warn("================================================================================");
    }

    /**
     * 记录 SQL 执行
     */
    public static void logSqlExecution(String sql, Object... params) {
        if (log.isDebugEnabled()) {
            log.debug("   📝 [SQL执行] {}", sql);
            if (params != null && params.length > 0) {
                log.debug("   📝 [SQL参数] {}", java.util.Arrays.toString(params));
            }
        }
    }

    /**
     * 记录连接获取
     */
    public static void logConnectionAcquisition(String dataSourceName) {
        log.debug("   🔌 [连接获取] 数据源: {}", dataSourceName);
    }

    /**
     * 记录连接释放
     */
    public static void logConnectionRelease(String dataSourceName) {
        log.debug("   🔌 [连接释放] 数据源: {}", dataSourceName);
    }

    /**
     * 注册事务同步（用于监听事务事件）
     */
    public static void registerTransactionSynchronization(TransactionContext context) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    log.info("   💾 [事务即将提交] ID: {}, 只读: {}", context.getTransactionId(), readOnly);
                }

                @Override
                public void beforeCompletion() {
                    log.debug("   🏁 [事务即将完成] ID: {}", context.getTransactionId());
                }

                @Override
                public void afterCommit() {
                    log.info("   ✅ [事务已提交] ID: {}", context.getTransactionId());
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) {
                        log.debug("   ✅ [事务完成-已提交] ID: {}", context.getTransactionId());
                    } else if (status == STATUS_ROLLED_BACK) {
                        log.warn("   ❌ [事务完成-已回滚] ID: {}", context.getTransactionId());
                    }
                }
            });
        }
    }

    /**
     * 获取传播行为名称
     */
    private static String getPropagationName(int propagationBehavior) {
        return switch (propagationBehavior) {
            case TransactionDefinition.PROPAGATION_REQUIRED -> "REQUIRED";
            case TransactionDefinition.PROPAGATION_REQUIRES_NEW -> "REQUIRES_NEW";
            case TransactionDefinition.PROPAGATION_MANDATORY -> "MANDATORY";
            case TransactionDefinition.PROPAGATION_SUPPORTS -> "SUPPORTS";
            case TransactionDefinition.PROPAGATION_NOT_SUPPORTED -> "NOT_SUPPORTED";
            case TransactionDefinition.PROPAGATION_NEVER -> "NEVER";
            case TransactionDefinition.PROPAGATION_NESTED -> "NESTED";
            default -> "UNKNOWN(" + propagationBehavior + ")";
        };
    }

    /**
     * 获取隔离级别名称
     */
    private static String getIsolationLevelName(int isolationLevel) {
        return switch (isolationLevel) {
            case TransactionDefinition.ISOLATION_DEFAULT -> "DEFAULT";
            case TransactionDefinition.ISOLATION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
            case TransactionDefinition.ISOLATION_READ_COMMITTED -> "READ_COMMITTED";
            case TransactionDefinition.ISOLATION_REPEATABLE_READ -> "REPEATABLE_READ";
            case TransactionDefinition.ISOLATION_SERIALIZABLE -> "SERIALIZABLE";
            default -> "UNKNOWN(" + isolationLevel + ")";
        };
    }
}
