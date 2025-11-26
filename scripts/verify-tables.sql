-- =============================================
-- 验证 SQL Server 表结构脚本
-- =============================================

USE BatchWeaverDB;
GO

PRINT '========================================';
PRINT '检查所有表';
PRINT '========================================';
GO

-- 1. 列出所有表
SELECT 
    TABLE_SCHEMA,
    TABLE_NAME,
    TABLE_TYPE
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
GO

PRINT '';
PRINT '========================================';
PRINT '检查业务表';
PRINT '========================================';
GO

-- 2. 检查业务表
IF OBJECT_ID('USER_DATA', 'U') IS NOT NULL
    PRINT '✓ USER_DATA 表存在';
ELSE
    PRINT '✗ USER_DATA 表不存在';
GO

-- 3. 检查 Spring Batch 元数据表
PRINT '';
PRINT '========================================';
PRINT '检查 Spring Batch 元数据表';
PRINT '========================================';
GO

IF OBJECT_ID('BATCH_JOB_INSTANCE', 'U') IS NOT NULL
    PRINT '✓ BATCH_JOB_INSTANCE 表存在';
ELSE
    PRINT '✗ BATCH_JOB_INSTANCE 表不存在';

IF OBJECT_ID('BATCH_JOB_EXECUTION', 'U') IS NOT NULL
    PRINT '✓ BATCH_JOB_EXECUTION 表存在';
ELSE
    PRINT '✗ BATCH_JOB_EXECUTION 表不存在';

IF OBJECT_ID('BATCH_JOB_EXECUTION_PARAMS', 'U') IS NOT NULL
    PRINT '✓ BATCH_JOB_EXECUTION_PARAMS 表存在';
ELSE
    PRINT '✗ BATCH_JOB_EXECUTION_PARAMS 表不存在';

IF OBJECT_ID('BATCH_STEP_EXECUTION', 'U') IS NOT NULL
    PRINT '✓ BATCH_STEP_EXECUTION 表存在';
ELSE
    PRINT '✗ BATCH_STEP_EXECUTION 表不存在';

IF OBJECT_ID('BATCH_JOB_EXECUTION_CONTEXT', 'U') IS NOT NULL
    PRINT '✓ BATCH_JOB_EXECUTION_CONTEXT 表存在';
ELSE
    PRINT '✗ BATCH_JOB_EXECUTION_CONTEXT 表不存在';

IF OBJECT_ID('BATCH_STEP_EXECUTION_CONTEXT', 'U') IS NOT NULL
    PRINT '✓ BATCH_STEP_EXECUTION_CONTEXT 表存在';
ELSE
    PRINT '✗ BATCH_STEP_EXECUTION_CONTEXT 表不存在';
GO

-- 4. 检查索引
PRINT '';
PRINT '========================================';
PRINT '检查索引';
PRINT '========================================';
GO

SELECT 
    t.name AS TableName,
    i.name AS IndexName,
    i.type_desc AS IndexType
FROM sys.indexes i
INNER JOIN sys.tables t ON i.object_id = t.object_id
WHERE t.name IN ('USER_DATA', 'BATCH_JOB_INSTANCE', 'BATCH_JOB_EXECUTION', 'BATCH_STEP_EXECUTION')
    AND i.name IS NOT NULL
ORDER BY t.name, i.name;
GO

-- 5. 检查外键约束
PRINT '';
PRINT '========================================';
PRINT '检查外键约束';
PRINT '========================================';
GO

SELECT 
    fk.name AS ForeignKeyName,
    OBJECT_NAME(fk.parent_object_id) AS TableName,
    OBJECT_NAME(fk.referenced_object_id) AS ReferencedTable
FROM sys.foreign_keys fk
WHERE OBJECT_NAME(fk.parent_object_id) LIKE 'BATCH_%'
ORDER BY TableName;
GO

-- 6. 检查数据
PRINT '';
PRINT '========================================';
PRINT '检查数据';
PRINT '========================================';
GO

SELECT 'USER_DATA' AS TableName, COUNT(*) AS RecordCount FROM USER_DATA
UNION ALL
SELECT 'BATCH_JOB_INSTANCE', COUNT(*) FROM BATCH_JOB_INSTANCE
UNION ALL
SELECT 'BATCH_JOB_EXECUTION', COUNT(*) FROM BATCH_JOB_EXECUTION
UNION ALL
SELECT 'BATCH_STEP_EXECUTION', COUNT(*) FROM BATCH_STEP_EXECUTION;
GO

PRINT '';
PRINT '========================================';
PRINT '验证完成！';
PRINT '========================================';
GO
