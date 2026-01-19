-- ===============================================================================================
-- Spring Batch Core Tables (SQL Server)
-- ===============================================================================================

IF OBJECT_ID('BATCH_STEP_EXECUTION_CONTEXT', 'U') IS NOT NULL DROP TABLE BATCH_STEP_EXECUTION_CONTEXT;
IF OBJECT_ID('BATCH_JOB_EXECUTION_CONTEXT', 'U') IS NOT NULL DROP TABLE BATCH_JOB_EXECUTION_CONTEXT;
IF OBJECT_ID('BATCH_STEP_EXECUTION', 'U') IS NOT NULL DROP TABLE BATCH_STEP_EXECUTION;
IF OBJECT_ID('BATCH_JOB_EXECUTION_PARAMS', 'U') IS NOT NULL DROP TABLE BATCH_JOB_EXECUTION_PARAMS;
IF OBJECT_ID('BATCH_JOB_EXECUTION', 'U') IS NOT NULL DROP TABLE BATCH_JOB_EXECUTION;
IF OBJECT_ID('BATCH_JOB_INSTANCE', 'U') IS NOT NULL DROP TABLE BATCH_JOB_INSTANCE;

-- Drop existing sequences if they exist
IF OBJECT_ID('BATCH_STEP_EXECUTION_SEQ', 'SO') IS NOT NULL DROP SEQUENCE BATCH_STEP_EXECUTION_SEQ;
IF OBJECT_ID('BATCH_JOB_EXECUTION_SEQ', 'SO') IS NOT NULL DROP SEQUENCE BATCH_JOB_EXECUTION_SEQ;
IF OBJECT_ID('BATCH_JOB_SEQ', 'SO') IS NOT NULL DROP SEQUENCE BATCH_JOB_SEQ;
GO

-- Create timestamp-based sequences
-- Using Unix timestamp (milliseconds since 1970-01-01) as starting point
-- Format: 1737173745123 (13 digits)
DECLARE @timestamp BIGINT = DATEDIFF_BIG(MILLISECOND, '1970-01-01', GETUTCDATE());

-- Job Instance ID Sequence (starts from current timestamp)
EXEC('CREATE SEQUENCE BATCH_JOB_SEQ START WITH ' + @timestamp + ' INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807');

-- Job Execution ID Sequence (starts from current timestamp)
EXEC('CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ START WITH ' + @timestamp + ' INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807');

-- Step Execution ID Sequence (starts from current timestamp)
EXEC('CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ START WITH ' + @timestamp + ' INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807');
GO

CREATE TABLE BATCH_JOB_INSTANCE  (
                                     JOB_INSTANCE_ID BIGINT  NOT NULL PRIMARY KEY ,
                                     VERSION BIGINT ,
                                     JOB_NAME NVARCHAR(100) NOT NULL,
                                     JOB_KEY NVARCHAR(32) NOT NULL,
                                     CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
) ;

CREATE TABLE BATCH_JOB_EXECUTION  (
                                      JOB_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
                                      VERSION BIGINT  ,
                                      JOB_INSTANCE_ID BIGINT NOT NULL,
                                      CREATE_TIME DATETIME NOT NULL,
                                      START_TIME DATETIME DEFAULT NULL ,
                                      END_TIME DATETIME DEFAULT NULL ,
                                      STATUS NVARCHAR(10) ,
                                      EXIT_CODE NVARCHAR(2500) ,
                                      EXIT_MESSAGE NVARCHAR(2500) ,
                                      LAST_UPDATED DATETIME,
                                      CONSTRAINT JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID)
                                          REFERENCES BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
) ;

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS  (
                                             JOB_EXECUTION_ID BIGINT NOT NULL ,
                                             PARAMETER_NAME NVARCHAR(100) NOT NULL ,
                                             PARAMETER_TYPE NVARCHAR(100) NOT NULL ,
                                             PARAMETER_VALUE NVARCHAR(2500) ,
                                             IDENTIFYING CHAR(1) NOT NULL ,
                                             CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID)
                                                 REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
) ;

CREATE TABLE BATCH_STEP_EXECUTION  (
                                       STEP_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
                                       VERSION BIGINT NOT NULL,
                                       STEP_NAME NVARCHAR(100) NOT NULL,
                                       JOB_EXECUTION_ID BIGINT NOT NULL,
                                       CREATE_TIME DATETIME NOT NULL,
                                       START_TIME DATETIME DEFAULT NULL ,
                                       END_TIME DATETIME DEFAULT NULL ,
                                       STATUS NVARCHAR(10) ,
                                       COMMIT_COUNT BIGINT ,
                                       READ_COUNT BIGINT ,
                                       FILTER_COUNT BIGINT ,
                                       WRITE_COUNT BIGINT ,
                                       READ_SKIP_COUNT BIGINT ,
                                       WRITE_SKIP_COUNT BIGINT ,
                                       PROCESS_SKIP_COUNT BIGINT ,
                                       ROLLBACK_COUNT BIGINT ,
                                       EXIT_CODE NVARCHAR(2500) ,
                                       EXIT_MESSAGE NVARCHAR(2500) ,
                                       LAST_UPDATED DATETIME,
                                       CONSTRAINT JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID)
                                           REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
) ;

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT  (
                                               STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
                                               SHORT_CONTEXT NVARCHAR(2500) NOT NULL,
                                               SERIALIZED_CONTEXT NVARCHAR(MAX) ,
                                               CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID)
                                                   REFERENCES BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
) ;

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT  (
                                              JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
                                              SHORT_CONTEXT NVARCHAR(2500) NOT NULL,
                                              SERIALIZED_CONTEXT NVARCHAR(MAX) ,
                                              CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID)
                                                  REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
) ;

-- ===============================================================================================
-- Performance Optimization Indexes
-- Based on Codex analysis - Expected improvement: 10x-100x query speedup
-- ===============================================================================================

-- Optimize BATCH_JOB_EXECUTION queries
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_JOB_EXECUTION_INSTANCE_ID' AND object_id = OBJECT_ID('BATCH_JOB_EXECUTION'))
    CREATE NONCLUSTERED INDEX IX_JOB_EXECUTION_INSTANCE_ID ON BATCH_JOB_EXECUTION(JOB_INSTANCE_ID);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_JOB_EXECUTION_STATUS_STARTTIME' AND object_id = OBJECT_ID('BATCH_JOB_EXECUTION'))
    CREATE NONCLUSTERED INDEX IX_JOB_EXECUTION_STATUS_STARTTIME
    ON BATCH_JOB_EXECUTION(STATUS, START_TIME DESC) INCLUDE (JOB_EXECUTION_ID, JOB_INSTANCE_ID);

-- Optimize BATCH_STEP_EXECUTION queries (MOST CRITICAL)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_STEP_EXECUTION_JOB_EXECUTION_ID' AND object_id = OBJECT_ID('BATCH_STEP_EXECUTION'))
    CREATE NONCLUSTERED INDEX IX_STEP_EXECUTION_JOB_EXECUTION_ID ON BATCH_STEP_EXECUTION(JOB_EXECUTION_ID);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_STEP_EXECUTION_STATUS' AND object_id = OBJECT_ID('BATCH_STEP_EXECUTION'))
    CREATE NONCLUSTERED INDEX IX_STEP_EXECUTION_STATUS
    ON BATCH_STEP_EXECUTION(JOB_EXECUTION_ID, STATUS) INCLUDE (STEP_EXECUTION_ID, STEP_NAME, START_TIME);

-- Optimize BATCH_JOB_EXECUTION_PARAMS queries
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_JOB_EXECUTION_PARAMS_EXECUTION_ID' AND object_id = OBJECT_ID('BATCH_JOB_EXECUTION_PARAMS'))
    CREATE NONCLUSTERED INDEX IX_JOB_EXECUTION_PARAMS_EXECUTION_ID ON BATCH_JOB_EXECUTION_PARAMS(JOB_EXECUTION_ID);

-- Optimize BATCH_JOB_INSTANCE queries
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_JOB_INSTANCE_NAME' AND object_id = OBJECT_ID('BATCH_JOB_INSTANCE'))
    CREATE NONCLUSTERED INDEX IX_JOB_INSTANCE_NAME ON BATCH_JOB_INSTANCE(JOB_NAME) INCLUDE (JOB_INSTANCE_ID);

-- Optimize ExecutionContext queries
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_JOB_EXECUTION_CONTEXT_EXECUTION_ID' AND object_id = OBJECT_ID('BATCH_JOB_EXECUTION_CONTEXT'))
    CREATE NONCLUSTERED INDEX IX_JOB_EXECUTION_CONTEXT_EXECUTION_ID ON BATCH_JOB_EXECUTION_CONTEXT(JOB_EXECUTION_ID);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_STEP_EXECUTION_CONTEXT_STEP_EXECUTION_ID' AND object_id = OBJECT_ID('BATCH_STEP_EXECUTION_CONTEXT'))
    CREATE NONCLUSTERED INDEX IX_STEP_EXECUTION_CONTEXT_STEP_EXECUTION_ID ON BATCH_STEP_EXECUTION_CONTEXT(STEP_EXECUTION_ID);

-- ===============================================================================================
-- Demo Business Tables
-- ===============================================================================================

IF OBJECT_ID('DEMO_USER', 'U') IS NOT NULL DROP TABLE DEMO_USER;

CREATE TABLE DEMO_USER (
                           ID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                           USERNAME NVARCHAR(100) NOT NULL,
                           EMAIL NVARCHAR(100),
                           STATUS NVARCHAR(20) DEFAULT 'ACTIVE',
                           CREATE_TIME DATETIME DEFAULT GETDATE(),
                           UPDATE_TIME DATETIME DEFAULT GETDATE()
);

-- ===============================================================================================
-- 表和字段描述 (Table and Column Descriptions)
-- ===============================================================================================

-- BATCH_JOB_INSTANCE 表描述
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Job实例表：存储Job的唯一实例信息，一个Job配置对应一个实例', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_INSTANCE';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Job实例ID（主键）', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_INSTANCE', @level2type = N'COLUMN', @level2name = N'JOB_INSTANCE_ID';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'版本号：用于乐观锁控制', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_INSTANCE', @level2type = N'COLUMN', @level2name = N'VERSION';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Job名称', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_INSTANCE', @level2type = N'COLUMN', @level2name = N'JOB_NAME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Job唯一标识Key：由Job名称和参数生成的哈希值', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_INSTANCE', @level2type = N'COLUMN', @level2name = N'JOB_KEY';

-- BATCH_JOB_EXECUTION 表描述
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Job执行表：记录每次Job的执行情况，一个实例可以有多次执行（失败重试）', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Job执行ID（主键）', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION', @level2type = N'COLUMN', @level2name = N'JOB_EXECUTION_ID';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'版本号：用于乐观锁控制', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION', @level2type = N'COLUMN', @level2name = N'VERSION';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'关联的Job实例ID', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION', @level2type = N'COLUMN', @level2name = N'JOB_INSTANCE_ID';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'创建时间', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION', @level2type = N'COLUMN', @level2name = N'CREATE_TIME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'开始执行时间', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION', @level2type = N'COLUMN', @level2name = N'START_TIME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'执行结束时间', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION', @level2type = N'COLUMN', @level2name = N'END_TIME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'执行状态：COMPLETED/FAILED/STARTED等', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION', @level2type = N'COLUMN', @level2name = N'STATUS';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'退出码', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION', @level2type = N'COLUMN', @level2name = N'EXIT_CODE';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'退出消息：记录异常信息或执行结果', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION', @level2type = N'COLUMN', @level2name = N'EXIT_MESSAGE';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'最后更新时间', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION', @level2type = N'COLUMN', @level2name = N'LAST_UPDATED';

-- BATCH_JOB_EXECUTION_PARAMS 表描述
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Job执行参数表：存储每次Job执行时传入的参数', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION_PARAMS';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'关联的Job执行ID', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION_PARAMS', @level2type = N'COLUMN', @level2name = N'JOB_EXECUTION_ID';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'参数名称', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION_PARAMS', @level2type = N'COLUMN', @level2name = N'PARAMETER_NAME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'参数类型：STRING/LONG/DATE等', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION_PARAMS', @level2type = N'COLUMN', @level2name = N'PARAMETER_TYPE';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'参数值', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION_PARAMS', @level2type = N'COLUMN', @level2name = N'PARAMETER_VALUE';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'是否为标识参数：Y/N，标识参数用于区分不同的Job实例', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION_PARAMS', @level2type = N'COLUMN', @level2name = N'IDENTIFYING';

-- BATCH_STEP_EXECUTION 表描述
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Step执行表：记录每个Step的执行情况和统计信息', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Step执行ID（主键）', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'STEP_EXECUTION_ID';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'版本号：用于乐观锁控制', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'VERSION';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Step名称', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'STEP_NAME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'关联的Job执行ID', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'JOB_EXECUTION_ID';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'创建时间', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'CREATE_TIME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'开始执行时间', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'START_TIME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'执行结束时间', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'END_TIME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'执行状态：COMPLETED/FAILED/STARTED等', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'STATUS';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'提交次数：事务提交的次数', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'COMMIT_COUNT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'读取记录数', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'READ_COUNT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'过滤记录数：被Processor过滤掉的记录数', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'FILTER_COUNT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'写入记录数', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'WRITE_COUNT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'读取跳过数：读取时发生异常跳过的记录数', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'READ_SKIP_COUNT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'写入跳过数：写入时发生异常跳过的记录数', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'WRITE_SKIP_COUNT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'处理跳过数：处理时发生异常跳过的记录数', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'PROCESS_SKIP_COUNT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'回滚次数：事务回滚的次数', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'ROLLBACK_COUNT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'退出码', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'EXIT_CODE';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'退出消息：记录异常信息或执行结果', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'EXIT_MESSAGE';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'最后更新时间', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION', @level2type = N'COLUMN', @level2name = N'LAST_UPDATED';

-- BATCH_STEP_EXECUTION_CONTEXT 表描述
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Step执行上下文表：存储Step执行过程中的上下文数据，用于断点续传', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION_CONTEXT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'关联的Step执行ID', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION_CONTEXT', @level2type = N'COLUMN', @level2name = N'STEP_EXECUTION_ID';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'短上下文：存储简短的上下文信息', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION_CONTEXT', @level2type = N'COLUMN', @level2name = N'SHORT_CONTEXT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'序列化上下文：存储完整的序列化上下文数据', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_STEP_EXECUTION_CONTEXT', @level2type = N'COLUMN', @level2name = N'SERIALIZED_CONTEXT';

-- BATCH_JOB_EXECUTION_CONTEXT 表描述
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'Job执行上下文表：存储Job执行过程中的上下文数据，用于Step间数据共享', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION_CONTEXT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'关联的Job执行ID', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION_CONTEXT', @level2type = N'COLUMN', @level2name = N'JOB_EXECUTION_ID';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'短上下文：存储简短的上下文信息', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION_CONTEXT', @level2type = N'COLUMN', @level2name = N'SHORT_CONTEXT';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'序列化上下文：存储完整的序列化上下文数据', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'BATCH_JOB_EXECUTION_CONTEXT', @level2type = N'COLUMN', @level2name = N'SERIALIZED_CONTEXT';

-- DEMO_USER 表描述
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'演示用户表：用于批处理任务的示例业务表', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'用户ID（主键，自增）', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', @level2type = N'COLUMN', @level2name = N'ID';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'用户登录名', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', @level2type = N'COLUMN', @level2name = N'USERNAME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'用户邮箱地址', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', @level2type = N'COLUMN', @level2name = N'EMAIL';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'用户状态：PENDING/ACTIVE/INACTIVE等', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', @level2type = N'COLUMN', @level2name = N'STATUS';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'记录创建时间', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', @level2type = N'COLUMN', @level2name = N'CREATE_TIME';
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'记录更新时间', @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', @level2type = N'COLUMN', @level2name = N'UPDATE_TIME';
