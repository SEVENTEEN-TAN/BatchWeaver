-- =============================================
-- 创建业务表（DDL）
-- =============================================

USE BatchWeaverDB;
GO

-- 删除现有表（如果存在）
IF OBJECT_ID('USER_DATA', 'U') IS NOT NULL
BEGIN
    DROP TABLE USER_DATA;
    PRINT 'Table USER_DATA dropped.';
END
GO

-- 创建 USER_DATA 表
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

-- 创建索引
CREATE INDEX IX_USER_DATA_EMAIL ON USER_DATA(EMAIL);
CREATE INDEX IX_USER_DATA_NAME ON USER_DATA(NAME);
GO

PRINT 'Business table indexes created successfully.';
GO
