# 快速开始指南

## 环境准备

### 系统要求
- **JDK**: 21+
- **Maven**: 3.6+
- **SQL Server**: 2022 或兼容版本
- **IDE**: IntelliJ IDEA / Eclipse（推荐 IDEA）

### 数据库初始化

1. 连接到 SQL Server:
```bash
sqlcmd -S localhost -U sa -P YourPassword
```

2. 创建数据库:
```sql
CREATE DATABASE BatchWeaverDB;
GO
```

3. 执行初始化脚本:
```bash
USE BatchWeaverDB;
GO
:r scripts/init.sql
GO
```

### 配置数据库连接

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB;encrypt=true;trustServerCertificate=true
    username: sa
    password: YourStrong!Passw0rd
```

---

## 运行示例

### 1. 编译打包

```bash
mvn clean package -DskipTests
```

### 2. 运行 Demo Job（数据流转）

```bash
# 自动生成 ID (新实例)
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=demoJob

# 指定 ID
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=demoJob id=10001
```

**功能**: 演示基础的 Import -> Update -> Export 流程

### 3. 运行 Breakpoint Job（断点续传）

```bash
# 首次运行（预期失败）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=breakpointJob id=20001

# 再次运行（自动续传）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=breakpointJob id=20001
```

**功能**: 演示失败重启机制

### 4. 运行 Transfer Job（参数传递）

```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=transferJob id=30001
```

**功能**: 演示 Step 间通过 `ExecutionContext` 传递数据

### 5. 运行 Chunk Job（分批处理）

```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=chunkJob
```

**功能**: 演示 Chunk 模式的批量读取、处理、写入

### 6. 运行 Conditional Job（条件流）

```bash
# 成功路径 (Step 1 -> Step 2)
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=conditionalJob

# 失败路径 (Step 1 -> Step 3)
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=conditionalJob fail=true
```

**功能**: 演示基于 Step 执行状态的条件跳转

---

## IDE 运行（IntelliJ IDEA）

### 方式一: 直接运行

1. 打开 `BatchApplication.java`
2. 点击 `main` 方法旁的运行按钮
3. IDE 会自动注入默认参数 (`jobName=demoJob`)

### 方式二: 配置参数

1. 打开 Run/Debug Configurations
2. 在 Program Arguments 中添加:
   ```
   jobName=demoJob id=10001
   ```
3. 运行

---

## 开发新 Job

### Step 1: 创建业务 Service

在 `src/main/java/com/example/batch/job/service/myfeature/` 下创建:

```java
package com.example.batch.job.service.myfeature;

import org.springframework.stereotype.Service;

@Service
public class MyService {
    public void processData() {
        System.out.println("Processing data...");
    }
}
```

### Step 2: 创建 Job 配置

在 `src/main/java/com/example/batch/job/config/` 下创建:

```java
package com.example.batch.job.config;

import com.example.batch.job.service.myfeature.MyService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class MyJobConfig {

    @Bean
    public Job myJob(JobRepository jobRepository, Step myStep) {
        return new JobBuilder("myJob", jobRepository)
                .start(myStep)
                .build();
    }

    @Bean
    public Step myStep(JobRepository jobRepository,
                       PlatformTransactionManager txManager,
                       MyService myService) {
        return new StepBuilder("myStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    myService.processData();
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }
}
```

### Step 3: 运行

```bash
mvn clean package -DskipTests
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=myJob
```

---

## 常见问题

### Q: 如何查看 Job 执行历史？

```sql
SELECT * FROM BATCH_JOB_EXECUTION ORDER BY START_TIME DESC;
```

### Q: 如何重跑失败的 Job？

使用相同的 `id` 参数重新运行即可自动续传。

### Q: 如何传递自定义参数？

```bash
java -jar app.jar jobName=myJob param1=value1 param2=value2
```

在 Service 中通过 `JobParameters` 获取:
```java
var params = chunkContext.getStepContext()
                         .getStepExecution()
                         .getJobExecution()
                         .getJobParameters();
String param1 = params.getString("param1");
```
