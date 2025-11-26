-- =============================================
-- 插入测试数据（DML）
-- =============================================

USE BatchWeaverDB;
GO

PRINT 'Inserting test data...';
GO

-- 插入业务表测试数据
INSERT INTO USER_DATA (NAME, EMAIL) VALUES 
    (N'Alice', N'alice@example.com'),
    (N'Bob', N'bob@example.com'),
    (N'Charlie', N'charlie@example.com');
GO

PRINT 'Test data inserted successfully.';
GO

-- 验证数据
SELECT COUNT(*) AS TotalRecords FROM USER_DATA;
GO

SELECT * FROM USER_DATA;
GO
