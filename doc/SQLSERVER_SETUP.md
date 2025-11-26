# SQL Server 2022 配置指南

> BatchWeaver 使用 SQL Server 2022 作为数据库

## 📋 前置要求

- SQL Server 2022 (Developer/Express/Standard/Enterprise Edition)
- SQL Server Management Studio (SSMS) 或 Azure Data Studio
- 确保 SQL Server 服务正在运行

---

## 🚀 快速开始

### 1. 安装 SQL Server 2022

**下载地址**:
- [SQL Server 2022 Developer Edition](https://www.microsoft.com/sql-server/sql-server-downloads) (免费)
- [SQL Server 2022 Express Edition](https://www.microsoft.com/sql-server/sql-server-downloads) (免费)

**安装步骤**:
1. 下载安装程序
2. 选择"基本"安装类型
3. 接受许可条款
4. 选择安装位置
5. 等待安装完成
6. 记录实例名称（默认为 `MSSQLSERVER`）

### 2. 启用 SQL Server 身份验证

1. 打开 SQL Server Management Studio (SSMS)
2. 连接到本地实例
3. 右键点击服务器 → 属性
4. 选择"安全性"
5. 选择"SQL Server 和 Windows 身份验证模式"
6. 点击确定
7. 重启 SQL Server 服务

### 3. 创建数据库

```sql
-- 创建数据库
CREATE DATABASE BatchWeaverDB;
GO

-- 使用数据库
USE BatchWeaverDB;
GO
```

### 4. 创建登录用户（可选）

如果不想使用 `sa` 账户，可以创建专用用户：

```sql
-- 创建登录
CREATE LOGIN batchweaver WITH PASSWORD = 'YourStrong@Password';
GO

-- 切换到数据库
USE BatchWeaverDB;
GO

-- 创建用户
CREATE USER batchweaver FOR LOGIN batchweaver;
GO

-- 授予权限
ALTER ROLE db_owner ADD MEMBER batchweaver;
GO
```

---

## ⚙️ 配置 BatchWeaver

### 修改 application.yml

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB;encrypt=true;trustServerCertificate=true
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
    username: sa  # 或你创建的用户名
    password: YourStrong@Password  # 修改为你的密码
    druid:
      url: jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB;encrypt=true;trustServerCertificate=true
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
      username: sa
      password: YourStrong@Password
```

### 连接字符串参数说明

| 参数 | 说明 |
|------|------|
| `localhost:1433` | 服务器地址和端口（默认 1433） |
| `databaseName=BatchWeaverDB` | 数据库名称 |
| `encrypt=true` | 启用加密连接 |
| `trustServerCertificate=true` | 信任服务器证书（开发环境） |

---

## 🗄️ 数据库表结构

### 业务表

```sql
-- 用户数据表
CREATE TABLE USER_DATA (
    ID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    NAME NVARCHAR(100),
    EMAIL NVARCHAR(100)
);
```

### Spring Batch 元数据表

Spring Batch 会自动创建以下表：

- `BATCH_JOB_INSTANCE` - Job 实例
- `BATCH_JOB_EXECUTION` - Job 执行记录
- `BATCH_JOB_EXECUTION_PARAMS` - Job 参数
- `BATCH_STEP_EXECUTION` - Step 执行记录
- `BATCH_JOB_EXECUTION_CONTEXT` - Job 执行上下文
- `BATCH_STEP_EXECUTION_CONTEXT` - Step 执行上下文

---

## 🔧 常见配置

### 1. 远程 SQL Server

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://192.168.1.100:1433;databaseName=BatchWeaverDB;encrypt=true;trustServerCertificate=true
    username: your_username
    password: your_password
```

### 2. 使用 Windows 身份验证

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB;integratedSecurity=true
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```

**注意**: 需要将 `mssql-jdbc_auth` DLL 文件添加到系统路径。

### 3. 连接池配置优化

```yaml
spring:
  datasource:
    druid:
      initial-size: 10          # 初始连接数
      min-idle: 10              # 最小空闲连接
      max-active: 50            # 最大活跃连接
      max-wait: 60000           # 获取连接最大等待时间（毫秒）
      validation-query: "SELECT 1"
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
```

---

## 🐛 故障排查

### 问题 1: 无法连接到 SQL Server

**错误信息**: `The TCP/IP connection to the host localhost, port 1433 has failed`

**解决方法**:
1. 确认 SQL Server 服务正在运行
   ```powershell
   Get-Service MSSQLSERVER
   ```

2. 启用 TCP/IP 协议
   - 打开 SQL Server Configuration Manager
   - 展开 "SQL Server 网络配置"
   - 选择实例的协议
   - 右键点击 "TCP/IP" → 启用
   - 重启 SQL Server 服务

3. 检查防火墙
   ```powershell
   New-NetFirewallRule -DisplayName "SQL Server" -Direction Inbound -Protocol TCP -LocalPort 1433 -Action Allow
   ```

### 问题 2: 登录失败

**错误信息**: `Login failed for user 'sa'`

**解决方法**:
1. 确认启用了 SQL Server 身份验证（混合模式）
2. 确认密码正确
3. 确认 `sa` 账户未被禁用
   ```sql
   ALTER LOGIN sa ENABLE;
   ALTER LOGIN sa WITH PASSWORD = 'YourStrong@Password';
   ```

### 问题 3: 数据库不存在

**错误信息**: `Cannot open database "BatchWeaverDB"`

**解决方法**:
```sql
CREATE DATABASE BatchWeaverDB;
```

### 问题 4: SSL 证书错误

**错误信息**: `The driver could not establish a secure connection to SQL Server`

**解决方法**:
在连接字符串中添加 `trustServerCertificate=true`：
```
jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB;encrypt=true;trustServerCertificate=true
```

---

## 📊 性能优化建议

### 1. 索引优化

```sql
-- 为常用查询字段添加索引
CREATE INDEX IX_USER_DATA_EMAIL ON USER_DATA(EMAIL);
CREATE INDEX IX_USER_DATA_NAME ON USER_DATA(NAME);
```

### 2. 统计信息更新

```sql
-- 更新统计信息
UPDATE STATISTICS USER_DATA;
```

### 3. 数据库维护

```sql
-- 重建索引
ALTER INDEX ALL ON USER_DATA REBUILD;

-- 更新统计信息
EXEC sp_updatestats;
```

---

## 🔐 安全建议

### 1. 使用强密码

密码应包含：
- 至少 8 个字符
- 大写字母
- 小写字母
- 数字
- 特殊字符

### 2. 最小权限原则

不要使用 `sa` 账户，创建专用用户并授予最小必要权限：

```sql
-- 创建只读用户
CREATE LOGIN readonly_user WITH PASSWORD = 'ReadOnly@123';
CREATE USER readonly_user FOR LOGIN readonly_user;
GRANT SELECT ON SCHEMA::dbo TO readonly_user;
```

### 3. 启用审计

```sql
-- 启用登录审计
USE master;
GO
ALTER SERVER AUDIT [Audit-Login]
WITH (STATE = ON);
GO
```

---

## 📝 备份与恢复

### 备份数据库

```sql
-- 完整备份
BACKUP DATABASE BatchWeaverDB
TO DISK = 'C:\Backup\BatchWeaverDB.bak'
WITH FORMAT, INIT, NAME = 'Full Backup of BatchWeaverDB';
GO
```

### 恢复数据库

```sql
-- 恢复数据库
RESTORE DATABASE BatchWeaverDB
FROM DISK = 'C:\Backup\BatchWeaverDB.bak'
WITH REPLACE;
GO
```

---

## 🔗 相关资源

- [SQL Server 2022 官方文档](https://docs.microsoft.com/sql/sql-server/)
- [JDBC 驱动文档](https://docs.microsoft.com/sql/connect/jdbc/)
- [Druid 连接池文档](https://github.com/alibaba/druid)
- [Spring Batch 文档](https://spring.io/projects/spring-batch)

---

## 📞 获取帮助

如果遇到问题：
1. 查看本文档的故障排查部分
2. 查看 SQL Server 错误日志
3. 查看应用程序日志
4. 参考 [快速参考文档](QUICK_REFERENCE.md)

---

**文档版本**: 1.0  
**最后更新**: 2024-01-01  
**适用版本**: SQL Server 2022, BatchWeaver 0.0.1-SNAPSHOT
