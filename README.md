# BatchWeaver - Spring Batch 5.0 原生编排示例

<div align="center">

**基于 Spring Batch 5.0 的原生 Java 配置示例项目**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5.x-blue.svg)](https://spring.io/projects/spring-batch)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![SQL Server](https://img.shields.io/badge/SQL%20Server-2022-red.svg)](https://www.microsoft.com/sql-server)

</div>

---

## 📖 项目简介

本项目使用 **原生 Spring Batch 5.0 Java Configuration**，遵循 Spring Batch 官方最佳实践，采用 `@Configuration` 和 `@Bean` 进行 Job 编排。

### 核心特性

- ✅ **原生支持**: 完全兼容 Spring Batch 5.x 生态
- ✅ **类型安全**: 编译时检查，避免配置错误
- ✅ **灵活编排**: 支持复杂的条件流（Conditional Flow）
- ✅ **断点续传**: 原生支持失败重启机制
- ✅ **高级执行控制**: 支持标准、断点续传、跳过失败、独立 Step 四种执行模式
- ✅ **分层架构**: Config 层 + Service 层，职责清晰
- ✅ **多数据源**: 集成 MyBatis-Flex，支持动态路由 4 个数据库
- ✅ **高可观测性**: 启动契约 + 执行摘要，全链路日志追踪

---

## 🏗️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.5.7 | 应用框架 |
| **Spring Batch** | 5.x | 批处理框架 |
| **MyBatis-Flex** | 2.0.9 | ORM 框架 + 多数据源 |
| **Java** | 21 | 开发语言（LTS 版本） |
| **SQL Server** | 2022 | 元数据存储 |
| **HikariCP** | 默认 | 数据库连接池 |

---

## 📁 项目结构

```
src/main/java/com/example/batch/
├── BatchApplication.java          # Spring Boot 入口
├── config/                        # 应用装配层（数据源、Batch 基础设施）
│   ├── BatchConfig.java
│   └── DataSourceConfig.java
├── core/                          # 框架核心（与业务无关）
│   ├── execution/                 # 执行模式 & 动态编排
│   ├── runner/                    # CLI 启动入口
│   ├── logging/                   # 日志 & 监控
│   ├── env/                       # 环境变量解密等横切能力
│   └── bootstrap/                 # 启动期辅助逻辑
└── demo/                          # 示例业务（作业 & 数据访问）
    ├── job/
    │   ├── config/                # 各示例 Job 的编排配置
    │   └── service/               # 示例 Job 的业务逻辑
    └── infrastructure/            # 示例数据访问层
        ├── entity/
        └── mapper/
```

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd BatchWeaver
```

### 2. 初始化数据库

在 SQL Server 中执行初始化脚本（见 `scripts/init.sql`）。

### 3. 配置数据库连接

编辑 `src/main/resources/application.yml` 修改数据库连接信息。

### 4. 编译打包

```bash
mvn clean package -DskipTests
```

### 5. 运行示例任务

#### 运行 Demo Job（数据流转）
```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=demoJob
```

#### 运行 Conditional Job（条件流）
```bash
# 正常运行 (Step 1 -> Step 2)
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=conditionalJob

# 模拟失败 (Step 1 -> Step 3)
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=conditionalJob fail=true
```

#### 运行 Multi-DataSource Job（多数据源演示）

**成功场景**（所有 Step 正常执行）：
```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=multiDataSourceJob simulateFail=false
```

或使用脚本：
```bash
scripts\demo-success.bat
```

**失败恢复场景**（Step3 失败，断点续传）：
```bash
# 第一次运行：Step3 失败
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=multiDataSourceJob simulateFail=true id=9999

# 第二次运行：断点续传（从 Step3 继续）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=multiDataSourceJob simulateFail=false id=9999
```

或使用脚本（自动执行两次）：
```bash
scripts\demo-fail-resume.bat
```

**验证结果**：
- 成功场景：DB1/DB2/DB3/DB4 均有数据
- 失败场景：
  - 第一次运行：DB1/DB2 有数据，DB3/DB4 无数据
  - 元数据表记录 `db3Step=FAILED`
  - 第二次运行：仅执行 Step3/Step4，DB3/DB4 补充数据

#### 运行 Advanced Control Job（高级执行控制）🆕

`advancedControlJob` 支持 4 种执行模式，通过 `_mode` 参数控制：

**1. 标准模式（STANDARD）**
```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob
# 或显式指定
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob _mode=STANDARD
```

**2. 断点续传模式（RESUME）**
```bash
# 第一次执行（模拟 Step3 失败）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob simulateFail=step3

# 第二次执行（从 Step3 继续，id 从元数据表查询）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob _mode=RESUME id=1001
```

**3. 跳过失败模式（SKIP_FAIL）**
```bash
# Step3 失败会被标记为 SKIPPED，继续执行 Step4
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob _mode=SKIP_FAIL simulateFail=step3
```

**4. 独立 Step 模式（ISOLATED）**
```bash
# 仅执行 advStep2
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob _mode=ISOLATED _target_steps=advStep2

# 执行多个 Step
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob _mode=ISOLATED _target_steps=advStep2,advStep3
```

**参数说明**：
- `_mode`: 执行模式（`STANDARD`/`RESUME`/`SKIP_FAIL`/`ISOLATED`）
- `_target_steps`: 独立模式下要执行的 Step（逗号分隔，如 `advStep1,advStep2`）
- `id`: 历史 Execution ID（RESUME 模式必需，从元数据表查询）
- `simulateFail`: 模拟失败的 Step（`step1`/`step2`/`step3`/`step4`）

---

## 🔀 多数据源配置

项目集成了 **MyBatis-Flex 2.0.9**，支持 4 个 SQL Server 数据源的动态路由。

### 数据源说明

| 数据源 | 数据库名 | 用途 |
|--------|---------|------|
| **db1** | BatchWeaverDB | Spring Batch 元数据 + 业务数据 |
| **db2** | DB2_Business | 业务数据库 2 |
| **db3** | DB3_Business | 业务数据库 3 |
| **db4** | DB4_Business | 业务数据库 4 |

### 配置示例

在 `application.yml` 中配置：

```yaml
mybatis-flex:
  datasource:
    db1:
     type: com.zaxxer.hikari.HikariDataSource
      url: jdbc:sqlserver://host:1433;databaseName=BatchWeaverDB;...
      username: sa
      password: ***
    db2:
     type: com.zaxxer.hikari.HikariDataSource
      url: jdbc:sqlserver://host:1433;databaseName=DB2_Business;...
      # ...
```

### 使用方式

在 Service 层使用 `@UseDataSource` 注解切换数据源：

```java
@Service
public class Db2BusinessService {

    @Autowired
    private Db2UserMapper mapper;

    @Transactional
    @UseDataSource("db2")  // 动态切换到 db2 数据源
    public void processDb2Data() {
        // 所有数据库操作在 db2 上执行
        mapper.insert(...);
    }
}
```

### 事务隔离机制

**关键设计**：Spring Batch 元数据事务与业务事务完全隔离

1. **元数据事务**：由 `BatchConfig` 中的 `batchTransactionManager` 管理，始终在 `db1` 上执行
2. **业务事务**：由 `DataSourceConfig` 中的 `businessTransactionManager` 管理，根据 `@UseDataSource` 动态切换

**效果**：
- Step 失败时，业务数据回滚（db2/db3/db4）
- 元数据正常提交（db1 的 `BATCH_STEP_EXECUTION` 记录失败状态）
- 保证断点续传机制正常工作

### 数据库初始化

每个数据库需创建 `DEMO_USER` 表：

```sql
CREATE TABLE DEMO_USER (
    ID BIGINT IDENTITY(1,1) PRIMARY KEY,
    USERNAME NVARCHAR(100),
    EMAIL NVARCHAR(100),
    STATUS NVARCHAR(50),
    UPDATE_TIME DATETIME2
);
```

---

## 🎯 开发指南

### 如何添加新 Job

#### Step 1: 创建业务 Service

在 `com.example.batch.demo.job.service` 下创建业务逻辑：

```java
package com.example.batch.demo.job.service.myfeature;

@Service
public class MyService {
  public void processData() {
    // 业务逻辑
  }
}
```

#### Step 2: 创建 Job 配置

在 `com.example.batch.demo.job.config` 下创建配置类：

```java
@Configuration
public class MyJobConfig {

    @Bean
    public Job myJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("myJob", jobRepository)
                .start(step1)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, 
                      PlatformTransactionManager txManager,
                      MyService myService) {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    myService.processData();
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }
}
```

#### Step 3: 运行

```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=myJob
```

### 条件流编排

Spring Batch 支持灵活的条件跳转：

```java
return new JobBuilder("conditionalJob", jobRepository)
        .start(step1)
            .on("FAILED").to(step3) // 失败跳转
        .from(step1)
            .on("*").to(step2)      // 成功继续
        .end()
        .build();
```

### Step 间参数传递

通过 `ExecutionContext` 在 Step 之间传递数据：

```java
public void step1Produce(StepContribution contribution, ChunkContext chunkContext) {
    var ctx = chunkContext.getStepContext()
                          .getStepExecution()
                          .getJobExecution()
                          .getExecutionContext();
    ctx.put("data", "value");
}

public void step2Consume(StepContribution contribution, ChunkContext chunkContext) {
    var ctx = chunkContext.getStepContext()
                          .getStepExecution()
                          .getJobExecution()
                          .getExecutionContext();
    String data = (String) ctx.get("data");
}
```

---

## 📊 监控与运维

使用标准的 Spring Batch 元数据表进行监控：

```sql
-- 查看最近的 Job 执行记录
SELECT * FROM BATCH_JOB_EXECUTION ORDER BY START_TIME DESC;

-- 查看失败的 Job
SELECT * FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED';
```

更多查询示例请参考 [元数据表说明文档](doc/METADATA_TABLES.md)。

---

## 📚 文档导航

| 文档 | 说明 |
|------|------|
| [执行模式文档](doc/EXECUTION_MODES.md) | 四种执行模式的语义、框架设计、流程图与规则说明 |
| [快速开始指南](doc/QUICK_START.md) | 环境准备、运行示例、开发新 Job |
| [元数据表说明](doc/METADATA_TABLES.md) | Spring Batch 元数据表结构与查询示例 |

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

---

## 📄 许可证

本项目采用 MIT 许可证。
