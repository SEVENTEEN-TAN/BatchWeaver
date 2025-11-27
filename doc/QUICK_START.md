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
该 Job 模拟任务执行失败，并在修复后支持断点续传。

**步骤 1: 首次运行 (模拟失败)**
```bash
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=breakpointJob
```
- **现象**: 任务在 Step 2 抛出异常并失败。
- **数据库**: `BATCH_JOB_EXECUTION` 表中该任务状态为 `FAILED`。

**步骤 2: 再次运行 (断点续传)**
```bash
# 添加 resume=true 参数 (可选，取决于具体实现，Spring Batch 默认会尝试 restart)
java -jar target/batch-scheduler-0.0.1-SNAPSHOT.jar jobName=breakpointJob
```
- **现象**: 任务从 Step 2 重新开始执行（跳过已成功的 Step 1），并最终成功。
- **数据库**: 任务状态更新为 `COMPLETED`。

## 常用命令
- **打包**: `mvn clean package -DskipTests`
- **清理**: `mvn clean`
