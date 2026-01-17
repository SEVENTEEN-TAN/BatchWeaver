# 启动前置校验机制

> 本文档描述 BatchWeaver 的启动前置校验机制，包括元数据表检查和执行顺序保证。

---

## 1. 概述

BatchWeaver 在启动时通过 `StartupMetadataListener` 执行前置检查，确保所有必需的 Spring Batch 元数据表存在。如果检查失败，应用将**立即终止**，避免后续执行出现不可预期的错误。

### 设计目标

- ✅ **快速失败（Fail-Fast）**：在应用完全启动后、执行任何 Job 之前检查
- ✅ **明确顺序**：通过 `@Order` 注解保证检查顺序
- ✅ **友好提示**：提供清晰的错误信息和解决方案

---

## 2. 执行顺序

### 2.1 启动流程

```
1. Spring Boot 启动
   ↓
2. 初始化所有 Bean（数据源、事务管理器等）
   ↓
3. 打印 "Started BatchApplication in X seconds"
   ↓
4. 🔹 StartupMetadataListener (@Order = HIGHEST_PRECEDENCE)
   → 检查 6 张元数据表
   → 失败则抛出 IllegalStateException
   ↓
5. 🔹 DynamicJobRunner (@Order = LOWEST_PRECEDENCE)
   → 参数格式校验
   → 执行模式语义校验
   → 执行 Job
```

### 2.2 关键时间点对比

| 时间戳 | 事件 | 说明 |
|--------|------|------|
| 16:41:32.715 | Started BatchApplication | ✅ Spring 容器启动完成 |
| 16:41:32.719 | Checking metadata tables | ✅ 元数据表检查开始 |
| 16:41:32.902 | All metadata tables verified | ✅ 检查通过 |
| 16:41:32.903 | Parameter Format Validation | ✅ Job 参数校验 |

从时间戳可以看出，**StartupMetadataListener 确实在 DynamicJobRunner 之前执行**。

---

## 3. 元数据表清单

StartupMetadataListener 检查以下 6 张表：

| 表名 | 用途 |
|------|------|
| `BATCH_JOB_INSTANCE` | Job 实例（唯一标识一个 Job 的参数组合） |
| `BATCH_JOB_EXECUTION` | Job 执行记录（每次运行的元数据） |
| `BATCH_JOB_EXECUTION_PARAMS` | Job 执行参数 |
| `BATCH_STEP_EXECUTION` | Step 执行记录 |
| `BATCH_STEP_EXECUTION_CONTEXT` | Step 执行上下文 |
| `BATCH_JOB_EXECUTION_CONTEXT` | Job 执行上下文 |

---

## 4. 检查逻辑

### 4.1 成功场景

**日志输出：**

```
========================================
Checking Spring Batch metadata tables...
========================================
✓ metadata table BATCH_JOB_INSTANCE exists
✓ metadata table BATCH_JOB_EXECUTION exists
✓ metadata table BATCH_JOB_EXECUTION_PARAMS exists
✓ metadata table BATCH_STEP_EXECUTION exists
✓ metadata table BATCH_STEP_EXECUTION_CONTEXT exists
✓ metadata table BATCH_JOB_EXECUTION_CONTEXT exists
========================================
All metadata tables verified successfully
========================================
```

**应用行为：** 继续执行，进入 DynamicJobRunner

### 4.2 失败场景

**日志输出：**

```
========================================
Checking Spring Batch metadata tables...
========================================
✓ metadata table BATCH_JOB_INSTANCE exists
✗ metadata table BATCH_JOB_EXECUTION is MISSING
✓ metadata table BATCH_JOB_EXECUTION_PARAMS exists
✗ metadata table BATCH_STEP_EXECUTION is MISSING
========================================
FATAL: Missing 2 metadata table(s): [BATCH_JOB_EXECUTION, BATCH_STEP_EXECUTION]
========================================
Action required:
  1. Execute database initialization script:
     → Run: scripts/init.sql
  2. Or use Spring Batch schema initialization:
     → Add to application.yml:
       spring.batch.jdbc.initialize-schema: always
========================================
```

**应用行为：** 抛出 `IllegalStateException`，应用终止

**异常信息：**
```
java.lang.IllegalStateException: Spring Batch metadata tables are missing: [BATCH_JOB_EXECUTION, BATCH_STEP_EXECUTION]. Please run scripts/init.sql to initialize the database.
```

---

## 5. 如何测试失败场景

### 方法 1：临时重命名表（推荐）

```sql
-- 临时重命名一张表来模拟缺失
EXEC sp_rename 'BATCH_JOB_EXECUTION', 'BATCH_JOB_EXECUTION_BACKUP';

-- 启动应用（会失败）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob

-- 恢复表名
EXEC sp_rename 'BATCH_JOB_EXECUTION_BACKUP', 'BATCH_JOB_EXECUTION';
```

### 方法 2：修改代码中的表名

临时修改 `StartupMetadataListener.java` 中的表名列表，添加一个不存在的表：

```java
private static final String[] TABLES = new String[]{
    "BATCH_JOB_INSTANCE",
    "BATCH_JOB_EXECUTION",
    "BATCH_JOB_EXECUTION_PARAMS",
    "BATCH_STEP_EXECUTION",
    "BATCH_STEP_EXECUTION_CONTEXT",
    "BATCH_JOB_EXECUTION_CONTEXT",
    "NON_EXISTENT_TABLE"  // 添加不存在的表
};
```

### 方法 3：使用测试配置

创建一个专门的测试配置类，覆盖 `JdbcTemplate` 指向一个空数据库。

---

## 6. 配置选项

### 6.1 调整执行顺序

如果需要在其他 `CommandLineRunner` 之前执行：

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE - 10)  // 比默认更高的优先级
public class StartupMetadataListener implements CommandLineRunner {
    // ...
}
```

### 6.2 禁用检查（不推荐）

如果在开发环境中需要临时禁用检查，可以添加条件注解：

```java
@Component
@ConditionalOnProperty(name = "batch.metadata.check.enabled", havingValue = "true", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartupMetadataListener implements CommandLineRunner {
    // ...
}
```

然后在 `application-dev.yml` 中：

```yaml
batch:
  metadata:
    check:
      enabled: false  # 开发环境禁用检查
```

---

## 7. 与参数校验的关系

BatchWeaver 有**两层前置校验**：

### 第一层：元数据表检查（StartupMetadataListener）

- **时机**：应用启动后，执行 Job 前
- **目的**：确保数据库环境就绪
- **失败行为**：抛出 `IllegalStateException`，应用终止

### 第二层：参数校验（DynamicJobRunner）

- **时机**：准备执行 Job 时
- **目的**：校验参数格式和执行模式语义
- **失败行为**：记录错误日志，跳过当前 Job 执行

### 校验层次关系

```
┌─────────────────────────────────────┐
│  StartupMetadataListener            │
│  (基础设施检查)                       │
│  - 元数据表是否存在                   │
│  - 数据库连接是否正常                 │
└─────────────────────────────────────┘
              ↓ 通过
┌─────────────────────────────────────┐
│  JobParametersFormatValidator       │
│  (参数格式检查)                       │
│  - id 是否为正整数                    │
│  - _target_steps 是否非空             │
└─────────────────────────────────────┘
              ↓ 通过
┌─────────────────────────────────────┐
│  ExecutionModeValidator             │
│  (执行模式语义检查)                   │
│  - 历史执行状态是否允许 RESUME        │
│  - Step 名称是否合法                 │
└─────────────────────────────────────┘
              ↓ 通过
┌─────────────────────────────────────┐
│  Job Execution                      │
│  (实际执行)                          │
└─────────────────────────────────────┘
```

---

## 8. 最佳实践

1. **不要跳过元数据表检查**：这是最基础的安全保障
2. **使用 `scripts/init.sql` 初始化数据库**：确保表结构一致
3. **关注启动日志**：出现 ✗ 标记立即处理
4. **测试失败场景**：确保错误提示清晰友好
5. **CI/CD 集成**：在自动化测试中验证元数据表存在

---

## 9. 故障排查

### 问题 1：应用启动失败，提示元数据表缺失

**原因：** 数据库未初始化或表结构不完整

**解决方案：**
```bash
# 执行初始化脚本
sqlcmd -S yourserver -d yourdb -U youruser -P yourpass -i scripts/init.sql
```

### 问题 2：StartupMetadataListener 没有执行

**原因：** `@Component` 未被扫描到

**解决方案：**
- 确认类在正确的包路径下（`com.example.batch.core.bootstrap`）
- 检查 Spring Boot 主类的 `@ComponentScan` 配置

### 问题 3：检查顺序不正确

**原因：** `@Order` 注解失效或有其他更高优先级的 Runner

**解决方案：**
- 确认 `@Order(Ordered.HIGHEST_PRECEDENCE)` 存在
- 检查是否有其他 `CommandLineRunner` 使用了更高的优先级

---

## 10. 相关文件

| 文件 | 路径 | 作用 |
|------|------|------|
| StartupMetadataListener | `src/main/java/com/example/batch/core/bootstrap/` | 元数据表检查 |
| DynamicJobRunner | `src/main/java/com/example/batch/core/runner/` | Job 执行器 |
| JobParametersFormatValidator | `src/main/java/com/example/batch/core/validation/` | 参数格式校验 |
| ExecutionModeValidator | `src/main/java/com/example/batch/core/execution/` | 执行模式语义校验 |
| init.sql | `scripts/` | 数据库初始化脚本 |
