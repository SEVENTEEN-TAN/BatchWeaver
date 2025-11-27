# 框架设计文档

## 核心设计理念
本项目采用**动态配置驱动**的设计模式，旨在降低 Spring Batch Job 的开发和维护成本。通过 XML 定义 Job 流程，结合反射机制自动执行业务逻辑，实现了 Job 定义与代码实现的解耦。

## 核心组件

### 1. XmlJobParser
- **功能**: 负责启动时扫描 `classpath:jobs/*.xml` 文件。
- **原理**: 使用 Jackson XML 解析器将 XML 转换为 Java 对象 (`JobXml`, `StepXml`)，然后通过 Spring Batch 的 `JobBuilder` 和 `StepBuilder` 动态构建 Job 实例并注册到 Spring 容器中。
- **优势**: 新增 Job 无需修改 Java 代码，只需添加 XML 配置文件。

### 2. ReflectionTasklet
- **功能**: 通用的 Tasklet 实现，用于执行具体的业务逻辑。
- **原理**: 
    - 接收 XML 中配置的 `className` 和 `methodName`。
    - 在运行时通过反射（Reflection）实例化目标类（或从 Spring 容器获取 Bean）并调用指定方法。
    - 支持将 XML 中的 `properties` 注入到方法参数或 Bean 属性中。
- **优势**: 开发者只需编写普通的 Java Service 方法，无需实现复杂的 Spring Batch 接口。

### 3. DynamicJobRunner
- **功能**: 统一的 Job 启动入口，封装了参数处理和异常捕获逻辑。
- **ID 策略**:
    - **自动生成**: 如果启动参数中未包含 `id`，Runner 会自动使用当前时间戳生成 `id`，从而确保创建新的 Job Instance。
    - **指定 ID**: 如果参数中包含 `id`，Runner 会使用该 ID 尝试启动 Job。如果该 ID 对应的 Job Instance 已存在且已完成，则跳过执行；如果已失败，则尝试断点续传。
- **优势**: 简化了 Job 的重试和新实例创建逻辑，对用户透明。

### 4. BatchApplication (启动器)
- **功能**: 程序的 main 入口，负责环境检测和参数预处理。
- **环境适配**:
    - **IDE 模式**: 检测到 IDE 环境（如 IntelliJ IDEA）时，如果未提供参数，会自动注入默认的 `jobName` 和 `id`，方便开发调试。
    - **CLI 模式**: 生产环境下严格校验必要参数 (`jobName`)，并提供友好的使用提示。

## 目录结构
- `src/main/resources/jobs/`: 存放 Job 的 XML 配置文件。
- `src/main/java/com/example/batch/service/`: 存放具体的业务逻辑 Service。
- `src/main/java/com/example/batch/core/`: 核心框架代码 (`XmlJobParser`, `ReflectionTasklet`, `DynamicJobRunner`)。

## 扩展性
- **新增业务**: 编写新的 Service 类 -> 在 XML 中配置新的 Step。
- **新增流程**: 新建 XML 文件 -> 定义 Steps 顺序。

## 参数传递机制
- 运行时数据在 Step 间通过 `ExecutionContext` 共享：
  - Job 级上下文：`jobExecution.getExecutionContext()`，所有 Step 可访问。
  - Step 级上下文：`stepExecution.getExecutionContext()`，当前 Step 专用。
- `JobParameters` 用于只读的实例标识与配置（例如 `id`），在运行期间不可修改。
- 方法签名支持：
  - 无参方法：`public void method()`（简单逻辑，不访问上下文）
  - 带上下文方法：`public void method(StepContribution, ChunkContext)`（推荐，用于读写 ExecutionContext 与 JobParameters）
- 典型用法：
```
public void step(StepContribution c, ChunkContext x) {
  var job = x.getStepContext().getStepExecution().getJobExecution();
  var ctx = job.getExecutionContext();
  var id = job.getJobParameters().getLong("id");
  ctx.put("batchId", id);
}
```
