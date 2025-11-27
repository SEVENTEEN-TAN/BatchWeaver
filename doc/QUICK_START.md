# 快速开始文档

## 环境准备
1. **数据库**: 确保 SQL Server 2022 已启动。
2. **初始化**: 运行 `scripts/init.sql` 创建必要的表结构。
3. **配置**: 检查 `src/main/resources/application.yml` 中的数据库连接信息。

## 运行 Job

本项目支持通过命令行参数指定要运行的 Job。启动程序会自动根据环境（IDE 或 CLI）进行适配。

### 1. 运行 Demo Job (数据流转)
该 Job 演示了数据的导入、更新和导出流程。

```bash
# 运行方式 1: IDE 参数
# IDE 模式下，如果不提供 id，会自动注入默认 id 以便调试
Program arguments: jobName=demoJob

# 运行方式 2: 命令行 (CLI)
# 不提供 id 参数时，系统会自动使用当前时间戳作为 id，创建一个新的 Job 实例
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=demoJob
```

**预期结果**:
- `DEMO_USER` 表中新增模拟数据。
- 控制台打印导入、更新、导出的日志信息。

### 2. 运行 Breakpoint Job (断点续传)
该 Job 模拟任务执行失败，并演示 Spring Batch 的断点续传机制。

#### 断点续传原理
Spring Batch 通过 `JobParameters` 来识别 Job 实例。在本项目中，我们统一使用 `id` 参数作为唯一标识：
- **指定相同的 id** = 同一个 Job 实例 → 失败后可以续传 (Resume)
- **指定新的 id (或不传)** = 新的 Job 实例 → 从头开始执行 (New Instance)

#### 步骤 1: 首次运行 (模拟失败)
为了演示断点续传，我们需要显式指定一个 ID，以便后续引用。

```bash
# 使用固定 ID 运行
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=breakpointJob id=10001
```
- **现象**: 任务在 Step 2 抛出异常并失败
- **数据库**: `BATCH_JOB_EXECUTION` 表中该任务状态为 `FAILED`
- **文件**: 在项目根目录生成 `breakpoint_marker.tmp` 标记文件

#### 步骤 2: 再次运行 (断点续传)
```bash
# 使用相同的 ID 再次运行，Spring Batch 会自动识别并重启
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=breakpointJob id=10001
```
- **现象**: 
  - Spring Batch 检测到 ID 为 10001 的失败实例
  - 自动跳过已成功的 Step 1
  - 从失败的 Step 2 重新开始执行
  - 检测到标记文件存在，不再抛出异常
  - 继续执行 Step 3，任务成功完成
- **数据库**: 
  - 同一个 `JOB_EXECUTION_ID` 的状态更新为 `COMPLETED`
  - `BATCH_STEP_EXECUTION` 表中可以看到 Step 1 只执行了一次，Step 2 执行了两次

#### 步骤 3: 强制创建新实例
```bash
# 如果想从头开始执行新的任务，只需不传 id (自动生成) 或传入新 id
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=breakpointJob
# 或者
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=breakpointJob id=20002
```
- **现象**: 创建新的 Job 实例，从 Step 1 开始执行
- **注意**: 需要先删除 `breakpoint_marker.tmp` 文件，否则不会触发失败

### 3. Step 参数/对象传递示例（transferJob）
使用 `ExecutionContext` 在 Step 间传递数据：

```xml
<job id="transferJob">
  <step id="produce">
    <className>com.example.batch.service.TransferService</className>
    <methodName>step1Produce</methodName>
  </step>
  <step id="consume">
    <className>com.example.batch.service.TransferService</className>
    <methodName>step2Consume</methodName>
  </step>
</job>
```

业务方法签名：
```java
public void step1Produce(StepContribution c, ChunkContext x) {
  var job = x.getStepContext().getStepExecution().getJobExecution();
  var ctx = job.getExecutionContext();
  ctx.put("note", "from-step1");
}
public void step2Consume(StepContribution c, ChunkContext x) {
  var ctx = x.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
  var note = (String) ctx.get("note");
}
```

运行示例：
```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=transferJob id=10001
```

## 常用命令
- **打包**: `mvn clean package -DskipTests`
- **清理**: `mvn clean`
