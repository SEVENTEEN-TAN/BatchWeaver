# Demo Jobs 测试用例文档

本文档包含 BatchWeaver Demo Jobs 的完整测试用例，覆盖 4 种执行模式、多数据源、条件流、数据传递等场景。

---

## 目录

1. [multiDsDemoJob - 多数据源全模式测试](#1-multidsDemojob---多数据源全模式测试)
2. [failureRecoveryDemoJob - 失败恢复测试](#2-failurerecoverydemojob---失败恢复测试)
3. [skipFailDemoJob - 容错执行测试](#3-skipfaildemojob---容错执行测试)
4. [dataTransferDemoJob - 跨库数据传递测试](#4-datatransferdemojob---跨库数据传递测试)
5. [conditionalFlowDemoJob - 条件流测试](#5-conditionalflowdemojob---条件流测试)
6. [验证 SQL 脚本](#验证-sql-脚本)

---

## 1. multiDsDemoJob - 多数据源全模式测试

**设计目标**：每个 Step 操作不同数据源，覆盖 4 种执行模式

### 测试用例

| 编号 | 测试场景 | 执行命令 | 预期结果 |
|------|---------|---------|---------|
| M-01 | STANDARD 正常执行 | `java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=multiDsDemoJob` | 4 库各有数据: db1:10, db2:8, db3:6, db4:4 |
| M-02 | STANDARD Step3 失败 | `java -jar ... jobName=multiDsDemoJob simulateFail=step3` | Step3 FAILED, db3 回滚: db1:10, db2:8, db3:0, db4:0 |
| M-03 | RESUME 从 Step3 续传 | `java -jar ... jobName=multiDsDemoJob _mode=RESUME id=<M-02的ID>` | 从 Step3 续传成功: db3:6, db4:4 补充 |
| M-04 | SKIP_FAIL Step2 跳过 | `java -jar ... jobName=multiDsDemoJob _mode=SKIP_FAIL simulateFail=step2` | Job COMPLETED, db2:0, db3:6, db4:4 |
| M-05 | ISOLATED 单独执行 Step3 | `java -jar ... jobName=multiDsDemoJob _mode=ISOLATED _target_steps=db3TransformStep` | 仅 db3 有数据变化 |
| M-06 | ISOLATED 执行多个 Step | `java -jar ... jobName=multiDsDemoJob _mode=ISOLATED _target_steps=db2ProcessStep,db3TransformStep` | db2:8, db3:6 |

### 验证方法

```sql
-- 验证多数据源数据分布
SELECT 'db1' as DB, COUNT(*) as CNT FROM BatchWeaverDB.dbo.DEMO_USER
UNION ALL SELECT 'db2', COUNT(*) FROM DB2_Business.dbo.DEMO_USER
UNION ALL SELECT 'db3', COUNT(*) FROM DB3_Business.dbo.DEMO_USER
UNION ALL SELECT 'db4', COUNT(*) FROM DB4_Business.dbo.DEMO_USER;

-- 预期结果（M-01 成功后）：
-- db1: 10, db2: 8, db3: 6, db4: 4
```

---

## 2. failureRecoveryDemoJob - 失败恢复测试

**设计目标**：验证多数据源场景下的事务回滚和断点续传

### 测试用例

| 编号 | 测试场景 | 执行命令 | 预期结果 |
|------|---------|---------|---------|
| F-01 | STANDARD 正常执行 | `java -jar ... jobName=failureRecoveryDemoJob` | 4 库各有 5 条记录 |
| F-02 | STANDARD Step3 失败 | `java -jar ... jobName=failureRecoveryDemoJob simulateFail=step3` | db3 回滚: db1:5, db2:5, db3:0, db4:0 |
| F-03 | RESUME 续传 | `java -jar ... jobName=failureRecoveryDemoJob _mode=RESUME id=<F-02的ID>` | db3/db4 补充数据 |

### 验证方法

```sql
-- 验证事务回滚（F-02 后）
SELECT 'db1' as DB, COUNT(*) as CNT FROM BatchWeaverDB.dbo.DEMO_USER
UNION ALL SELECT 'db2', COUNT(*) FROM DB2_Business.dbo.DEMO_USER
UNION ALL SELECT 'db3', COUNT(*) FROM DB3_Business.dbo.DEMO_USER
UNION ALL SELECT 'db4', COUNT(*) FROM DB4_Business.dbo.DEMO_USER;

-- 预期结果：db1:5, db2:5, db3:0, db4:0
-- db3 为 0 说明事务正确回滚
```

---

## 3. skipFailDemoJob - 容错执行测试

**设计目标**：验证 SKIP_FAIL 模式下多数据源的部分失败容忍

### 测试用例

| 编号 | 测试场景 | 执行命令 | 预期结果 |
|------|---------|---------|---------|
| S-01 | SKIP_FAIL 正常执行 | `java -jar ... jobName=skipFailDemoJob _mode=SKIP_FAIL` | 4 库各有 5 条数据 |
| S-02 | SKIP_FAIL Step2 跳过 | `java -jar ... jobName=skipFailDemoJob _mode=SKIP_FAIL simulateFail=step2` | db2:0, 其他正常 |
| S-03 | SKIP_FAIL 多 Step 跳过 | `java -jar ... jobName=skipFailDemoJob _mode=SKIP_FAIL simulateFail=step2,step3` | db2:0, db3:0, 其他正常 |
| S-04 | ISOLATED 修补失败 Step | `java -jar ... jobName=skipFailDemoJob _mode=ISOLATED _target_steps=db2ParseStep` | 独立执行 Step2 |

### 验证方法

```sql
-- 验证 SKIP_FAIL 模式（S-02 后）
SELECT 'db1' as DB, COUNT(*) as CNT FROM BatchWeaverDB.dbo.DEMO_USER
UNION ALL SELECT 'db2', COUNT(*) FROM DB2_Business.dbo.DEMO_USER
UNION ALL SELECT 'db3', COUNT(*) FROM DB3_Business.dbo.DEMO_USER
UNION ALL SELECT 'db4', COUNT(*) FROM DB4_Business.dbo.DEMO_USER;

-- 预期结果：db1:5, db2:0, db3:5, db4:5
-- db2 为 0 说明 Step2 被跳过并回滚

-- 检查元数据确认 Job 状态为 COMPLETED
SELECT je.STATUS, je.EXIT_CODE FROM BATCH_JOB_EXECUTION je
WHERE je.JOB_EXECUTION_ID = <ID>;
-- 预期：STATUS=COMPLETED
```

---

## 4. dataTransferDemoJob - 跨库数据传递测试

**设计目标**：验证 ExecutionContext 在跨数据源 Step 间的数据共享

### 测试用例

| 编号 | 测试场景 | 执行命令 | 预期结果 |
|------|---------|---------|---------|
| D-01 | STANDARD 正常执行 | `java -jar ... jobName=dataTransferDemoJob` | db2 有 ENRICHED 记录, db1 状态变为 ACTIVE |
| D-02 | STANDARD Step2 失败 | `java -jar ... jobName=dataTransferDemoJob simulateFail=step2` | ExecutionContext 已保存 userIds |
| D-03 | RESUME 恢复上下文 | `java -jar ... jobName=dataTransferDemoJob _mode=RESUME id=<D-02的ID>` | 从 userIds 继续处理 |

### 验证方法

```sql
-- 验证数据传递成功（D-01 后）
-- db1 状态应为 ACTIVE
SELECT STATUS, COUNT(*) FROM BatchWeaverDB.dbo.DEMO_USER GROUP BY STATUS;
-- 预期：ACTIVE: 5

-- db2 应有 ENRICHED 记录
SELECT STATUS, COUNT(*) FROM DB2_Business.dbo.DEMO_USER GROUP BY STATUS;
-- 预期：ENRICHED: 5

-- 验证 ExecutionContext 保存（D-02 后）
SELECT SHORT_CONTEXT FROM BATCH_JOB_EXECUTION_CONTEXT
WHERE JOB_EXECUTION_ID = <D-02的ID>;
-- 预期：包含 userIds 数据
```

---

## 5. conditionalFlowDemoJob - 条件流测试

**设计目标**：验证条件流的分支逻辑与执行模式的兼容性

### 测试用例

| 编号 | 测试场景 | 执行命令 | 预期结果 |
|------|---------|---------|---------|
| C-01 | 成功分支 | `java -jar ... jobName=conditionalFlowDemoJob flow=success` | 执行 init→success→common, STATUS=SUCCESS |
| C-02 | 失败分支 | `java -jar ... jobName=conditionalFlowDemoJob flow=fail` | 执行 init→failure→common, STATUS=FAILED_HANDLED |
| C-03 | SKIP_FAIL 被拒绝 | `java -jar ... jobName=conditionalFlowDemoJob _mode=SKIP_FAIL` | 日志提示条件流不支持 SKIP_FAIL |
| C-04 | ISOLATED 执行单个 Step | `java -jar ... jobName=conditionalFlowDemoJob _mode=ISOLATED _target_steps=successPathStep` | 仅 successPathStep 执行 |

### 验证方法

```sql
-- 验证成功分支（C-01 后）
SELECT STATUS, COUNT(*) FROM BatchWeaverDB.dbo.DEMO_USER GROUP BY STATUS;
-- 预期：SUCCESS: 5

-- 验证失败分支（C-02 后）
SELECT STATUS, COUNT(*) FROM BatchWeaverDB.dbo.DEMO_USER GROUP BY STATUS;
-- 预期：FAILED_HANDLED: 5

-- 验证执行路径（检查 Step 执行记录）
SELECT STEP_NAME, STATUS FROM BATCH_STEP_EXECUTION
WHERE JOB_EXECUTION_ID = <ID> ORDER BY STEP_EXECUTION_ID;
-- C-01 预期：initStep, successPathStep, commonStep
-- C-02 预期：initStep, failurePathStep, commonStep
```

---

## 验证 SQL 脚本

### 通用查询

```sql
-- 1. 查看最近的 Job 执行状态
SELECT TOP 10
    je.JOB_EXECUTION_ID,
    ji.JOB_NAME,
    je.STATUS,
    je.EXIT_CODE,
    je.START_TIME,
    je.END_TIME
FROM BATCH_JOB_EXECUTION je
JOIN BATCH_JOB_INSTANCE ji ON je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
ORDER BY je.JOB_EXECUTION_ID DESC;

-- 2. 查看某次执行的所有 Step 状态
SELECT 
    STEP_NAME,
    STATUS,
    EXIT_CODE,
    COMMIT_COUNT,
    READ_COUNT,
    WRITE_COUNT
FROM BATCH_STEP_EXECUTION 
WHERE JOB_EXECUTION_ID = <替换为实际ID>
ORDER BY STEP_EXECUTION_ID;

-- 3. 查看 Job 执行参数
SELECT PARAMETER_NAME, PARAMETER_VALUE
FROM BATCH_JOB_EXECUTION_PARAMS
WHERE JOB_EXECUTION_ID = <替换为实际ID>;

-- 4. 验证多数据源数据分布（汇总）
SELECT 'db1' as DB, COUNT(*) as CNT FROM BatchWeaverDB.dbo.DEMO_USER
UNION ALL SELECT 'db2', COUNT(*) FROM DB2_Business.dbo.DEMO_USER
UNION ALL SELECT 'db3', COUNT(*) FROM DB3_Business.dbo.DEMO_USER
UNION ALL SELECT 'db4', COUNT(*) FROM DB4_Business.dbo.DEMO_USER;

-- 5. 各库状态分布
SELECT 'db1' as DB, STATUS, COUNT(*) as CNT FROM BatchWeaverDB.dbo.DEMO_USER GROUP BY STATUS
UNION ALL SELECT 'db2', STATUS, COUNT(*) FROM DB2_Business.dbo.DEMO_USER GROUP BY STATUS
UNION ALL SELECT 'db3', STATUS, COUNT(*) FROM DB3_Business.dbo.DEMO_USER GROUP BY STATUS
UNION ALL SELECT 'db4', STATUS, COUNT(*) FROM DB4_Business.dbo.DEMO_USER GROUP BY STATUS
ORDER BY DB, STATUS;
```

---

## 快速测试脚本

### Windows (test-all-demos.bat)

```batch
@echo off
setlocal

set JAR=target\batch-weaver-0.0.1-SNAPSHOT.jar

echo ========================================
echo Testing Demo Jobs
echo ========================================

echo.
echo [1/5] multiDsDemoJob - STANDARD
java -jar %JAR% jobName=multiDsDemoJob
timeout /t 2 /nobreak > nul

echo.
echo [2/5] failureRecoveryDemoJob - STANDARD
java -jar %JAR% jobName=failureRecoveryDemoJob
timeout /t 2 /nobreak > nul

echo.
echo [3/5] skipFailDemoJob - SKIP_FAIL
java -jar %JAR% jobName=skipFailDemoJob _mode=SKIP_FAIL
timeout /t 2 /nobreak > nul

echo.
echo [4/5] dataTransferDemoJob - STANDARD
java -jar %JAR% jobName=dataTransferDemoJob
timeout /t 2 /nobreak > nul

echo.
echo [5/5] conditionalFlowDemoJob - flow=success
java -jar %JAR% jobName=conditionalFlowDemoJob flow=success
timeout /t 2 /nobreak > nul

echo.
echo ========================================
echo All Demo Jobs tested!
echo ========================================
pause
```

---

## 场景覆盖矩阵

| 场景 | multiDsDemoJob | failureRecoveryDemoJob | skipFailDemoJob | dataTransferDemoJob | conditionalFlowDemoJob |
|-----|:---:|:---:|:---:|:---:|:---:|
| **STANDARD 模式** | ✅ M-01 | ✅ F-01 | - | ✅ D-01 | ✅ C-01/C-02 |
| **RESUME 模式** | ✅ M-03 | ✅ F-03 | - | ✅ D-03 | - |
| **SKIP_FAIL 模式** | ✅ M-04 | - | ✅ S-01/S-02/S-03 | - | ❌ C-03 |
| **ISOLATED 模式** | ✅ M-05/M-06 | - | ✅ S-04 | - | ✅ C-04 |
| **多数据源** | ✅ | ✅ | ✅ | ✅ | - |
| **条件流** | - | - | - | - | ✅ |
| **ExecutionContext** | - | - | - | ✅ | - |
| **事务回滚** | ✅ | ✅ | ✅ | - | - |
