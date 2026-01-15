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
- ✅ **分层架构**: Config 层 + Service 层，职责清晰

---

## 🏗️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.5.7 | 应用框架 |
| **Spring Batch** | 5.x | 批处理框架 |
| **Java** | 21 | 开发语言（LTS 版本） |
| **SQL Server** | 2022 | 元数据存储 |
| **HikariCP** | 默认 | 数据库连接池 |

---

## 📁 项目结构

```
src/main/java/com/example/batch/
├── job/
│   ├── config/              # Job 配置层（纯配置）
│   │   ├── DemoJobConfig.java
│   │   ├── BreakpointJobConfig.java
│   │   ├── TransferJobConfig.java
│   │   ├── ChunkJobConfig.java
│   │   └── ConditionalJobConfig.java
│   └── service/             # 业务服务层
│       ├── demo/            # Demo Job 业务逻辑
│       ├── breakpoint/      # 断点续传业务逻辑
│       ├── transfer/        # 参数传递业务逻辑
│       └── chunk/           # Chunk 处理业务逻辑
└── core/                    # 框架核心
    └── DynamicJobRunner.java
```

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd SpringBatch
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

---

## 🎯 开发指南

### 如何添加新 Job

#### Step 1: 创建业务 Service

在 `com.example.batch.job.service` 下创建业务逻辑：

```java
package com.example.batch.job.service.myfeature;

@Service
public class MyService {
    public void processData() {
        // 业务逻辑
    }
}
```

#### Step 2: 创建 Job 配置

在 `com.example.batch.job.config` 下创建配置类：

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
| [框架设计文档](doc/FRAMEWORK_DESIGN.md) | 核心设计原理、组件说明 |
| [快速开始指南](doc/QUICK_START.md) | 环境准备、运行示例 |
| [元数据表说明](doc/METADATA_TABLES.md) | Spring Batch 元数据表结构 |

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

---

## 📄 许可证

本项目采用 MIT 许可证。
