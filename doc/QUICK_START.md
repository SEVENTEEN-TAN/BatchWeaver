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
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=demoJob
```

**功能**: 演示基础的 Import -> Update -> Export 流程

### 3. 运行 Conditional Job（条件流）

```bash
# 成功路径 (Step 1 -> Step 2)
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=conditionalJob

# 失败路径 (Step 1 -> Step 3)
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=conditionalJob fail=true
```

**功能**: 演示基于 Step 执行状态的条件跳转

---

## 高级执行模式

支持 4 种执行模式，通过 `_mode` 参数控制：

| 模式 | 说明 | ID 规则 |
|------|------|---------|
| STANDARD | 标准执行（默认） | 不能带 ID |
| RESUME | 断点续传 | 必须带 ID |
| SKIP_FAIL | 跳过失败继续 | 不能带 ID |
| ISOLATED | 独立执行指定 Step | 带/不带均可 |

### STANDARD 模式（默认）

```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob
```

### RESUME 模式（断点续传）

```bash
# 第一次执行（模拟失败）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob simulateFail=step3

# 查询失败的 Execution ID（从元数据表）
# SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION WHERE STATUS='FAILED'

# 第二次执行（续传）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob _mode=RESUME id=<查询到的ID>
```

### SKIP_FAIL 模式（容错执行）

```bash
# Step 失败会被跳过，继续执行后续 Step
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob _mode=SKIP_FAIL simulateFail=step3
```

### ISOLATED 模式（独立执行）

```bash
# 仅执行指定的 Step
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob _mode=ISOLATED _target_steps=advStep2

# 执行多个 Step
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=advancedControlJob _mode=ISOLATED _target_steps=advStep2,advStep3
```

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

import com.example.batch.core.execution.BatchJob;
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
@BatchJob(name = "myJob", steps = {"myStep"})  // 启用高级执行模式
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

### Q: 如何查看失败的 Step？

```sql
SELECT * FROM BATCH_STEP_EXECUTION WHERE STATUS = 'FAILED';
```

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
