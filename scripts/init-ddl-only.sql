-- =============================================
-- BatchWeaver DDL 初始化脚本（仅创建表结构）
-- 适用于生产环境，不包含测试数据
-- =============================================

PRINT '========================================';
PRINT 'BatchWeaver DDL Initialization';
PRINT '========================================';
PRINT '';
GO

-- 1. 创建数据库
PRINT 'Step 1: Creating database...';
:r ddl\01-create-database.sql
PRINT '';
GO

-- 2. 创建业务表
PRINT 'Step 2: Creating business tables...';
:r ddl\02-create-business-tables.sql
PRINT '';
GO

-- 3. 创建 Spring Batch 系统表
PRINT 'Step 3: Creating Spring Batch metadata tables...';
:r ddl\03-create-batch-tables.sql
PRINT '';
GO

-- 验证所有表
PRINT '========================================';
PRINT 'Verification';
PRINT '========================================';
GO

USE BatchWeaverDB;
GO

SELECT 
    TABLE_NAME,
    TABLE_TYPE
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME IN ('USER_DATA', 'BATCH_JOB_INSTANCE', 'BATCH_JOB_EXECUTION', 
                     'BATCH_JOB_EXECUTION_PARAMS', 'BATCH_STEP_EXECUTION',
                     'BATCH_JOB_EXECUTION_CONTEXT', 'BATCH_STEP_EXECUTION_CONTEXT')
ORDER BY TABLE_NAME;
GO

PRINT '';
PRINT '========================================';
PRINT 'DDL initialization completed successfully!';
PRINT 'Tables are ready for use.';
PRINT '========================================';
GO
