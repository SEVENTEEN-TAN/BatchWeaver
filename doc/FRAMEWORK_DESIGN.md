# 框架设计文档

## 核心设计理念

本项目采用 **原生 Spring Batch 5.0 Java Configuration** 设计模式，遵循 Spring Boot 最佳实践，通过 `@Configuration` 和 `@Bean` 进行 Job 编排，实现类型安全和编译时检查。

## 架构分层

### 1. Config 层 (`job/config`)
- **职责**: Job 和 Step 的配置与编排
- **特点**: 纯配置代码，无业务逻辑
- **示例**: `DemoJobConfig`, `ChunkJobConfig`

### 2. Service 层 (`job/service`)
- **职责**: 业务逻辑实现
- **特点**: 标准 Spring `@Service`，可独立测试
- **示例**: `DemoService`, `TransferService`

### 3. Core 层 (`core`)
- **职责**: 框架核心功能
- **组件**: `DynamicJobRunner` (统一启动入口)

## 核心组件

### DynamicJobRunner
- **功能**: 统一的 Job 启动入口，封装参数处理和异常捕获
- **ID 策略**:
    - **自动生成**: 未传 `id` 时使用时间戳创建新实例
    - **指定 ID**: 传入 `id` 时尝试重启或续传
- **优势**: 简化重试和新实例创建逻辑

### BatchApplication (启动器)
- **功能**: 程序入口，环境检测和参数预处理
- **环境适配**:
    - **IDE 模式**: 自动注入默认参数，方便调试
    - **CLI 模式**: 严格校验参数，生产环境友好

## 项目结构

```
src/main/java/com/example/batch/
├── job/
│   ├── config/              # Job 配置层
│   │   ├── DemoJobConfig.java
│   │   ├── BreakpointJobConfig.java
│   │   ├── TransferJobConfig.java
│   │   ├── ChunkJobConfig.java
│   │   └── ConditionalJobConfig.java
│   └── service/             # 业务服务层
│       ├── demo/
│       ├── breakpoint/
│       ├── transfer/
│       └── chunk/
└── core/                    # 框架核心
    └── DynamicJobRunner.java
```

## 参数传递机制

### ExecutionContext
- **Job 级上下文**: `jobExecution.getExecutionContext()` - 所有 Step 可访问
- **Step 级上下文**: `stepExecution.getExecutionContext()` - 当前 Step 专用

### JobParameters
- **用途**: 只读的实例标识与配置（如 `id`）
- **特点**: 运行期间不可修改

### 方法签名
```java
// 推荐：带上下文方法
public void method(StepContribution contribution, ChunkContext chunkContext) {
    var jobExec = chunkContext.getStepContext()
                              .getStepExecution()
                              .getJobExecution();
    var ctx = jobExec.getExecutionContext();
    var id = jobExec.getJobParameters().getLong("id");
    ctx.put("key", "value");
}
```

## Chunk 步骤支持

### 配置示例
```java
@Bean
public Step chunkStep(JobRepository jobRepository,
                      PlatformTransactionManager txManager,
                      ItemReader reader,
                      ItemProcessor processor,
                      ItemWriter writer) {
    return new StepBuilder("chunkStep", jobRepository)
            .<Input, Output>chunk(500, txManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
}
```

### 设计优势
- 将大任务拆分为短事务，缩短锁持有时间
- 每次提交形成检查点，失败仅影响当前批次
- 重跑可从最近提交处继续

## 条件流支持

### 配置示例
```java
@Bean
public Job conditionalJob(JobRepository jobRepository,
                          Step step1, Step step2, Step step3) {
    return new JobBuilder("conditionalJob", jobRepository)
            .start(step1)
                .on("FAILED").to(step3)  // 失败跳转
            .from(step1)
                .on("*").to(step2)       // 成功继续
            .end()
            .build();
}
```

## 扩展性

### 新增业务
1. 在 `service/` 下创建业务 Service
2. 在 `config/` 下创建 Job 配置类
3. 运行 `java -jar app.jar jobName=newJob`

### 优势
- **类型安全**: 编译时检查，避免运行时错误
- **易于测试**: Service 层可独立单元测试
- **IDE 友好**: 完整的代码提示和重构支持
