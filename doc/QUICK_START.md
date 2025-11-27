# 快速开始文档

## 环境准备
1. **数据库**: 确保 SQL Server 2022 已启动。
2. **初始化**: 运行 `scripts/init.sql` 创建必要的表结构。
3. **配置**: 检查 `src/main/resources/application.yml` 中的数据库连接信息。

## 运行 Job

本项目支持通过命令行参数指定要运行的 Job。

### 1. 运行 Demo Job (数据流转)
该 Job 演示了数据的导入、更新和导出流程。

```bash
# 运行方式 1: IDE 参数
Program arguments: jobName=demoJob

# 运行方式 2: Java Jar
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=demoJob
```

**预期结果**:
- `DEMO_USER` 表中新增模拟数据。
- 控制台打印导入、更新、导出的日志信息。

### 2. 运行 Breakpoint Job (断点续传)
该 Job 模拟任务执行失败，并演示 Spring Batch 的断点续传机制。

#### 断点续传原理
Spring Batch 通过 `JobParameters` 来识别 Job 实例：
- **相同参数** = 同一个 Job 实例 → 失败后可以续传
- **不同参数** = 新的 Job 实例 → 从头开始执行

#### 步骤 1: 首次运行 (模拟失败)
```bash
# 使用固定参数运行
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=breakpointJob
```
- **现象**: 任务在 Step 2 抛出异常并失败
- **数据库**: `BATCH_JOB_EXECUTION` 表中该任务状态为 `FAILED`
- **文件**: 在项目根目录生成 `breakpoint_marker.tmp` 标记文件

#### 步骤 2: 再次运行 (断点续传)
```bash
# 使用相同的参数再次运行，Spring Batch 会自动识别并重启
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=breakpointJob
```
- **现象**: 
  - Spring Batch 检测到之前失败的实例
  - 自动跳过已成功的 Step 1
  - 从失败的 Step 2 重新开始执行
  - 检测到标记文件存在，不再抛出异常
  - 继续执行 Step 3，任务成功完成
- **数据库**: 
  - 同一个 `JOB_EXECUTION_ID` 的状态更新为 `COMPLETED`
  - `BATCH_STEP_EXECUTION` 表中可以看到 Step 1 只执行了一次，Step 2 执行了两次

#### 步骤 3: 强制创建新实例 (可选)
```bash
# 如果想从头开始执行新的任务，传入 restart=false
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=breakpointJob restart=false
```
- **现象**: 创建新的 Job 实例，从 Step 1 开始执行
- **注意**: 需要先删除 `breakpoint_marker.tmp` 文件，否则不会触发失败

## 常用命令
- **打包**: `mvn clean package -DskipTests`
- **清理**: `mvn clean`
