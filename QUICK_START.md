# BatchWeaver 快速启动指南

> 5 分钟快速启动 BatchWeaver 项目

---

## 📋 前置要求

- ✅ Java 17+
- ✅ Maven 3.6+
- ✅ SQL Server 2022

---

## 🚀 快速启动步骤

### 1️⃣ 初始化数据库

```bash
# 开发环境：运行完整初始化（包含测试数据）
sqlcmd -S localhost -U sa -P YourStrong@Password -i scripts\init-all.sql

# 生产环境：仅创建表结构（不含测试数据）
sqlcmd -S localhost -U sa -P YourStrong@Password -i scripts\init-ddl-only.sql

# 验证表创建
sqlcmd -S localhost -U sa -P YourStrong@Password -i scripts\verify-tables.sql
```

**预期输出**:
```
✓ USER_DATA 表存在
✓ BATCH_JOB_INSTANCE 表存在
✓ BATCH_JOB_EXECUTION 表存在
✓ BATCH_JOB_EXECUTION_PARAMS 表存在
✓ BATCH_STEP_EXECUTION 表存在
✓ BATCH_JOB_EXECUTION_CONTEXT 表存在
✓ BATCH_STEP_EXECUTION_CONTEXT 表存在
```

### 2️⃣ 配置数据库连接

编辑 `src/main/resources/application.yml`，确认数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB;encrypt=true;trustServerCertificate=true
    username: sa
    password: YourStrong@Password  # 修改为你的密码
```

### 3️⃣ 编译项目

```bash
mvn clean package -DskipTests
```

### 4️⃣ 运行 Job

**方式 1: 使用启动脚本（推荐）**

```bash
# Windows
scripts\run-job.bat jobName=demoJob

# Linux/Mac
./scripts/run-job.sh jobName=demoJob
```

**方式 2: 直接运行 JAR**

```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=demoJob
```

**方式 3: 从 IDE 运行**

直接运行 `BatchApplication.main()` 方法（会自动运行默认 Job）

---

## 🎯 验证运行结果

### 查看 Job 执行记录

```sql
USE BatchWeaverDB;

-- 查看所有 Job 执行记录
SELECT 
    ji.JOB_NAME,
    je.STATUS,
    je.START_TIME,
    je.END_TIME,
    DATEDIFF(SECOND, je.START_TIME, je.END_TIME) AS DURATION_SECONDS
FROM BATCH_JOB_INSTANCE ji
JOIN BATCH_JOB_EXECUTION je ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
ORDER BY je.CREATE_TIME DESC;
```

### 查看 Step 执行详情

```sql
-- 查看最近一次 Job 的 Step 执行情况
SELECT 
    se.STEP_NAME,
    se.STATUS,
    se.READ_COUNT,
    se.WRITE_COUNT,
    se.COMMIT_COUNT,
    DATEDIFF(SECOND, se.START_TIME, se.END_TIME) AS DURATION_SECONDS
FROM BATCH_STEP_EXECUTION se
JOIN BATCH_JOB_EXECUTION je ON se.JOB_EXECUTION_ID = je.JOB_EXECUTION_ID
WHERE je.JOB_EXECUTION_ID = (SELECT MAX(JOB_EXECUTION_ID) FROM BATCH_JOB_EXECUTION);
```

---

## 📚 可用的示例 Job

| Job 名称 | 说明 | 使用场景 |
|---------|------|---------|
| `demoJob` | 简单演示 Job | 快速验证环境 |
| `importJob` | CSV 数据导入 | 文件导入场景 |
| `multiStepJob` | 多步骤处理 | 复杂业务流程 |

### 运行示例

```bash
# 1. 运行演示 Job
scripts\run-job.bat jobName=demoJob

# 2. 运行数据导入 Job
scripts\run-job.bat jobName=importJob

# 3. 运行多步骤 Job
scripts\run-job.bat jobName=multiStepJob

# 4. 断点续传（从失败的 Step 继续）
scripts\run-job.bat jobName=multiStepJob resume=true
```

---

## 🔧 常见问题

### Q1: 数据库连接失败

**错误**: `Cannot open database "BatchWeaverDB"`

**解决**:
1. 确认 SQL Server 正在运行
2. 运行初始化脚本: `sqlcmd -S localhost -U sa -P YourPassword -i scripts\init-sqlserver.sql`
3. 检查 `application.yml` 中的密码是否正确

### Q2: 找不到 JAR 文件

**错误**: `Unable to access jarfile target/batch-weaver-0.0.1-SNAPSHOT.jar`

**解决**: 先编译项目
```bash
mvn clean package -DskipTests
```

### Q3: Job 未找到

**错误**: `Job not found: xxx`

**解决**:
1. 检查 Job 名称是否正确
2. 查看 `src/main/resources/jobs/` 目录下是否有对应的 XML 文件

### Q4: 权限不足（Linux/Mac）

**错误**: `Permission denied`

**解决**: 添加执行权限
```bash
chmod +x scripts/run-job.sh
```

---

## 📖 下一步

- 📘 阅读 [项目文档](doc/INDEX.md) 了解架构设计
- 🔍 查看 [快速参考](doc/QUICK_REFERENCE.md) 学习如何创建自定义 Job
- 🗄️ 了解 [Spring Batch 系统表](doc/SPRING_BATCH_TABLES.md) 的作用
- 🔄 学习 [SQL Server 迁移指南](doc/MIGRATION_H2_TO_SQLSERVER.md)

---

## 🎉 成功！

如果你看到类似以下的输出，说明 Job 运行成功：

```
2024-01-01 10:00:00.123  INFO --- Job: [SimpleJob: [name=demoJob]] launched
2024-01-01 10:00:00.456  INFO --- Executing step: [demoStep]
2024-01-01 10:00:01.789  INFO --- Step: [demoStep] executed in 1s
2024-01-01 10:00:01.890  INFO --- Job: [SimpleJob: [name=demoJob]] completed with status=COMPLETED
```

现在你可以开始使用 BatchWeaver 了！🚀

---

**文档版本**: 1.0  
**最后更新**: 2024-01-01  
**相关文档**: [完整文档索引](doc/INDEX.md)
