-- =============================================
-- BatchWeaver SQL Server 2022 初始化脚本
-- =============================================

-- 1. 创建数据库
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'BatchWeaverDB')
BEGIN
    CREATE DATABASE BatchWeaverDB;
    PRINT 'Database BatchWeaverDB created successfully.';
END
ELSE
BEGIN
    PRINT 'Database BatchWeaverDB already exists.';
END
GO

-- 2. 使用数据库
USE BatchWeaverDB;
GO

-- 3. 创建业务表
IF OBJECT_ID('USER_DATA', 'U') IS NOT NULL
BEGIN
    DROP TABLE USER_DATA;
    PRINT 'Table USER_DATA dropped.';
END
GO

CREATE TABLE USER_DATA (
    ID BIGINT IDENTITY(1,1) NOT NULL,
    NAME NVARCHAR(100),
    EMAIL NVARCHAR(100),
    CREATED_DATE DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT PK_USER_DATA PRIMARY KEY (ID)
);
GO

PRINT 'Table USER_DATA created successfully.';
GO

-- 4. 创建索引
CREATE INDEX IX_USER_DATA_EMAIL ON USER_DATA(EMAIL);
CREATE INDEX IX_USER_DATA_NAME ON USER_DATA(NAME);
GO

PRINT 'Indexes created successfully.';
GO

-- 5. 插入测试数据（可选）
INSERT INTO USER_DATA (NAME, EMAIL) VALUES 
    (N'Alice', N'alice@example.com'),
    (N'Bob', N'bob@example.com'),
    (N'Charlie', N'charlie@example.com');
GO

PRINT 'Test data inserted successfully.';
GO

-- 6. 验证
SELECT COUNT(*) AS TotalRecords FROM USER_DATA;
GO

PRINT 'Initialization completed successfully!';
GO
