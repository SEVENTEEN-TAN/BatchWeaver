-- ===============================================================================================
-- BatchWeaver Demo 业务表初始化脚本
-- 
-- 使用说明：
-- 1. 此脚本用于创建 Demo Job 所需的业务表（DEMO_USER）
-- 2. 需要在 4 个数据库上分别执行：BatchWeaverDB, DB2_Business, DB3_Business, DB4_Business
-- 3. 元数据表（BATCH_*）只需在 BatchWeaverDB 中创建，请使用 init.sql
--
-- 执行顺序：
-- 1. 先在 BatchWeaverDB 执行 init.sql（创建元数据表 + DEMO_USER）
-- 2. 然后在 DB2_Business, DB3_Business, DB4_Business 分别执行此脚本
-- ===============================================================================================

-- 删除旧表（如果存在）
IF OBJECT_ID('DEMO_USER', 'U') IS NOT NULL DROP TABLE DEMO_USER;

-- 创建 DEMO_USER 表
CREATE TABLE DEMO_USER (
    ID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    USERNAME NVARCHAR(100) NOT NULL,
    EMAIL NVARCHAR(100),
    STATUS NVARCHAR(20) DEFAULT 'ACTIVE',
    CREATE_TIME DATETIME DEFAULT GETDATE(),
    UPDATE_TIME DATETIME DEFAULT GETDATE()
);

-- 添加表描述
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'演示用户表：用于批处理任务的示例业务表', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'DEMO_USER';

-- 添加字段描述
EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'用户ID（主键，自增）', 
    @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', 
    @level2type = N'COLUMN', @level2name = N'ID';

EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'用户登录名', 
    @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', 
    @level2type = N'COLUMN', @level2name = N'USERNAME';

EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'用户邮箱地址', 
    @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', 
    @level2type = N'COLUMN', @level2name = N'EMAIL';

EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'用户状态：PENDING/ACTIVE/PROCESSING/COMPLETED 等', 
    @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', 
    @level2type = N'COLUMN', @level2name = N'STATUS';

EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'记录创建时间', 
    @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', 
    @level2type = N'COLUMN', @level2name = N'CREATE_TIME';

EXEC sp_addextendedproperty @name = N'MS_Description', @value = N'记录更新时间', 
    @level0type = N'SCHEMA', @level0name = N'dbo', @level1type = N'TABLE', @level1name = N'DEMO_USER', 
    @level2type = N'COLUMN', @level2name = N'UPDATE_TIME';

PRINT 'DEMO_USER 表创建成功！';
