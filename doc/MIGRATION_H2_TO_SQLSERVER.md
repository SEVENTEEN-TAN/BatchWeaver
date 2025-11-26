# 从 H2 迁移到 SQL Server 2022

> 本文档说明如何从 H2 数据库迁移到 SQL Server 2022

## 📋 迁移概述

本项目已从 H2 文件数据库迁移到 SQL Server 2022，以支持：
- 更好的生产环境支持
- 更强的并发性能
- 更完善的企业级特性
- 更好的数据安全性

---

## 🔄 主要变更

### 1. 依赖变更

**pom.xml**:
```xml
<!-- 移除 H2（仅保留测试环境） -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>

<!-- 添加 SQL Server 驱动 -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. 配置变更

**application.yml**:
```yaml
# 旧配置 (H2)
url: jdbc:h2:file:./data/batchdb

# 新配置 (SQL Server)
url: jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB;encrypt=true;trustServerCertificate=true
driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```

### 3. SQL 语法变更

**schema-all.sql**:
```sql
-- H2 语法
DROP TABLE IF EXISTS USER_DATA;
CREATE TABLE USER_DATA (
    ID BIGINT IDENTITY NOT NULL PRIMARY KEY,
    NAME VARCHAR(100),
    EMAIL VARCHAR(100)
);

-- SQL Server 语法
IF OBJECT_ID('USER_DATA', 'U') IS NOT NULL
    DROP TABLE USER_DATA;
GO

CREATE TABLE USER_DATA (
    ID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    NAME NVARCHAR(100),
    EMAIL NVARCHAR(100)
);
GO
```

---

## 🚀 迁移步骤

### 步骤 1: 安装 SQL Server 2022

参考 [SQL Server 配置文档](SQLSERVER_SETUP.md) 完成安装。

### 步骤 2: 创建数据库

运行初始化脚本：
```bash
# 使用 sqlcmd
sqlcmd -S localhost -U sa -P YourStrong@Password -i scripts/init-sqlserver.sql

# 或使用 SSMS
# 打开 scripts/init-sqlserver.sql 并执行
```

### 步骤 3: 导出 H2 数据（如果需要）

如果你有现有的 H2 数据需要迁移：

```sql
-- 在 H2 中导出数据
SELECT * FROM USER_DATA;
-- 保存为 CSV 文件
```

### 步骤 4: 导入数据到 SQL Server

```sql
-- 使用 BULK INSERT
BULK INSERT USER_DATA
FROM 'C:\path\to\data.csv'
WITH (
    FIELDTERMINATOR = ',',
    ROWTERMINATOR = '\n',
    FIRSTROW = 2
);
```

### 步骤 5: 更新配置

编辑 `src/main/resources/application.yml`，更新数据库连接信息。

### 步骤 6: 重新编译

```bash
mvn clean package -DskipTests
```

### 步骤 7: 测试运行

```bash
# 运行测试 Job
scripts\run-job.bat jobName=demoJob
```

---

## 📊 数据类型映射

| H2 类型 | SQL Server 类型 | 说明 |
|---------|----------------|------|
| `VARCHAR(n)` | `NVARCHAR(n)` | 支持 Unicode |
| `BIGINT` | `BIGINT` | 64位整数 |
| `INTEGER` | `INT` | 32位整数 |
| `TIMESTAMP` | `DATETIME2` | 日期时间 |
| `BOOLEAN` | `BIT` | 布尔值 |
| `CLOB` | `NVARCHAR(MAX)` | 大文本 |
| `BLOB` | `VARBINARY(MAX)` | 二进制数据 |

---

## ⚠️ 注意事项

### 1. 自增列语法

**H2**:
```sql
ID BIGINT IDENTITY NOT NULL
```

**SQL Server**:
```sql
ID BIGINT IDENTITY(1,1) NOT NULL
```

### 2. 字符串类型

- 使用 `NVARCHAR` 而不是 `VARCHAR` 以支持 Unicode
- SQL Server 区分大小写取决于排序规则

### 3. GO 语句

SQL Server 使用 `GO` 作为批处理分隔符：
```sql
CREATE TABLE ...;
GO

CREATE INDEX ...;
GO
```

### 4. 日期函数

| H2 | SQL Server |
|-----|-----------|
| `CURRENT_TIMESTAMP` | `GETDATE()` 或 `SYSDATETIME()` |
| `CURRENT_DATE` | `CAST(GETDATE() AS DATE)` |

### 5. 字符串连接

| H2 | SQL Server |
|-----|-----------|
| `'Hello' || ' World'` | `'Hello' + ' World'` 或 `CONCAT('Hello', ' World')` |

---

## 🔧 兼容性处理

### 如果需要同时支持 H2 和 SQL Server

可以使用 Spring Profile 配置：

**application-h2.yml**:
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/batchdb
    driver-class-name: org.h2.Driver
```

**application-sqlserver.yml**:
```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
```

**运行时指定**:
```bash
# 使用 H2
java -jar app.jar --spring.profiles.active=h2

# 使用 SQL Server
java -jar app.jar --spring.profiles.active=sqlserver
```

---

## 🐛 常见问题

### 问题 1: 连接失败

确保：
1. SQL Server 服务正在运行
2. TCP/IP 协议已启用
3. 防火墙允许 1433 端口
4. 用户名密码正确

### 问题 2: 表已存在

```sql
-- 删除所有表
DROP TABLE IF EXISTS USER_DATA;
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_CONTEXT;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_CONTEXT;
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_PARAMS;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION;
DROP TABLE IF EXISTS BATCH_JOB_INSTANCE;
```

### 问题 3: 字符编码问题

使用 `NVARCHAR` 而不是 `VARCHAR` 来支持中文等 Unicode 字符。

---

## 📈 性能对比

| 指标 | H2 | SQL Server 2022 |
|------|-----|----------------|
| 并发连接 | 有限 | 优秀 |
| 事务支持 | 基础 | 完整 |
| 备份恢复 | 文件复制 | 企业级 |
| 监控工具 | 有限 | 丰富 |
| 生产环境 | 不推荐 | 推荐 |

---

## ✅ 迁移检查清单

- [ ] 安装 SQL Server 2022
- [ ] 创建数据库 `BatchWeaverDB`
- [ ] 运行初始化脚本
- [ ] 更新 `pom.xml` 依赖
- [ ] 更新 `application.yml` 配置
- [ ] 更新 `schema-all.sql` 语法
- [ ] 导出 H2 数据（如需要）
- [ ] 导入数据到 SQL Server
- [ ] 重新编译项目
- [ ] 运行测试 Job
- [ ] 验证数据正确性
- [ ] 更新文档

---

## 🔗 相关文档

- [SQL Server 配置指南](SQLSERVER_SETUP.md)
- [快速参考](QUICK_REFERENCE.md)
- [代码架构](CODE_ARCHITECTURE.md)

---

**文档版本**: 1.0  
**最后更新**: 2024-01-01  
**迁移难度**: ⭐⭐ (简单)
