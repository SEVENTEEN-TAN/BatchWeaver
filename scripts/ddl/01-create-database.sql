-- =============================================
-- 创建数据库
-- =============================================

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

USE BatchWeaverDB;
GO

PRINT 'Database initialization completed.';
GO
