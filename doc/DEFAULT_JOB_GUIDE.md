# 默认 Job 配置指南

> 如何配置和使用 BatchWeaver 的默认 Job 功能

## 概述

BatchWeaver 支持在无参数启动时自动运行默认 Job，这使得开发调试和快速测试变得更加便捷。

## 默认 Job 的工作原理

### 处理位置

默认 Job 的处理逻辑位于 `BatchApplication.main()` 方法中，在应用启动前就完成参数的处理。

### 处理流程

```
1. 检查命令行参数中是否包含 jobName=xxx
   ├─ 有 jobName → 直接使用用户指定的 Job
   └─ 没有 jobName → 继续下一步

2. 检查是否有其他参数
   ├─ 没有任何参数 → 添加 jobName=demoJob
   └─ 有其他参数 → 在参数列表前添加 jobName=demoJob

3. 启动 Spring Boot 应用
```

### 代码实现

```java
@SpringBootApplication
@EnableScheduling
public class BatchApplication {

    // 默认 Job 名称（可修改）
    private static final String DEFAULT_JOB_NAME = "demoJob";

    public static void main(String[] args) {
        // 检查是否提供了 jobName 参数
        boolean hasJobName = false;
        for (String arg : args) {
            if (arg.startsWith("jobName=")) {
                hasJobName = true;
                break;
            }
        }

        // 如果没有提供 jobName，添加默认的 jobName
        if (!hasJobName && args.length == 0) {
            System.out.println("========================================");
            System.out.println("No jobName provided. Using default job: " + DEFAULT_JOB_NAME);
            System.out.println("To run a specific job, use: java -jar app.jar jobName=<jobName>");
            System.out.println("Or set program arguments in your IDE: jobName=<jobName>");
            System.out.println("========================================");
            args = new String[]{"jobName=" + DEFAULT_JOB_NAME};
        } else if (!hasJobName && args.length > 0) {
            // 有其他参数但没有 jobName，添加默认 jobName
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "jobName=" + DEFAULT_JOB_NAME;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
            System.out.println("No jobName provided. Using default job: " + DEFAULT_JOB_NAME);
        }

        SpringApplication.run(BatchApplication.class, args);
    }
}
```

## 使用方法

### 1. 使用默认 Job

**命令行**:
```bash
# 直接运行，使用默认 Job (demoJob)
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar
```

**IDE**:
- 直接运行 `BatchApplication.main()` 方法
- 无需配置任何参数

**输出**:
```
========================================
No jobName provided. Using default job: demoJob
To run a specific job, use: java -jar app.jar jobName=<jobName>
Or set program arguments in your IDE: jobName=<jobName>
========================================
Starting job: demoJob
...
```

### 2. 指定特定 Job

**命令行**:
```bash
# 运行指定的 Job
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=importJob
```

**IDE**:
- Run → Edit Configurations
- 在 "Program arguments" 中输入: `jobName=importJob`

### 3. 带其他参数但使用默认 Job

**命令行**:
```bash
# 只传递其他参数，自动使用默认 Job
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar date=2024-01-01 batchSize=1000
```

**等价于**:
```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=demoJob date=2024-01-01 batchSize=1000
```

## 修改默认 Job

### 方法 1: 修改源代码（推荐）

编辑 `src/main/java/com/example/batch/BatchApplication.java`：

```java
// 修改这个常量
private static final String DEFAULT_JOB_NAME = "importJob";  // 改为你想要的 Job
```

**优点**:
- ✅ 简单直接
- ✅ 编译时确定
- ✅ 类型安全

**适用场景**:
- 项目主要使用某个特定的 Job
- 开发环境固定使用某个 Job

### 方法 2: 使用环境变量（未来支持）

```bash
# 设置环境变量
export DEFAULT_JOB_NAME=importJob

# 运行应用
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar
```

**优点**:
- ✅ 无需修改代码
- ✅ 灵活切换
- ✅ 适合不同环境

**适用场景**:
- 不同环境使用不同的默认 Job
- 需要动态配置

> 注意: 此功能需要在代码中实现环境变量读取逻辑

### 方法 3: 使用配置文件（未来支持）

在 `application.yml` 中配置：

```yaml
batch:
  default-job-name: importJob
```

**优点**:
- ✅ 配置集中管理
- ✅ 支持不同 profile
- ✅ 易于维护

**适用场景**:
- 需要根据 profile 切换默认 Job
- 配置需要版本控制

> 注意: 此功能需要在代码中实现配置读取逻辑

## 使用场景

### 场景 1: 开发调试

**需求**: 频繁测试某个 Job，不想每次都输入参数

**解决方案**:
1. 将该 Job 设置为默认 Job
2. 从 IDE 直接运行 `BatchApplication.main()`
3. 无需配置任何参数

**示例**:
```java
// 开发期间主要测试 importJob
private static final String DEFAULT_JOB_NAME = "importJob";
```

### 场景 2: 快速演示

**需求**: 向他人演示系统功能，希望一键启动

**解决方案**:
1. 设置 `demoJob` 为默认 Job
2. 运行 `java -jar app.jar`
3. 系统自动运行演示 Job

### 场景 3: 生产环境

**需求**: 生产环境主要运行某个 Job，偶尔运行其他 Job

**解决方案**:
1. 将主要 Job 设置为默认 Job
2. 日常运行无需指定参数
3. 运行其他 Job 时显式指定 `jobName`

**示例**:
```bash
# 日常运行（使用默认 Job）
java -jar app.jar

# 偶尔运行其他 Job
java -jar app.jar jobName=specialJob
```

### 场景 4: 定时任务

**需求**: 使用 cron 定时运行某个 Job

**解决方案**:
1. 将该 Job 设置为默认 Job
2. 在 crontab 中配置简单的启动命令

**示例**:
```bash
# crontab 配置
0 2 * * * cd /app && java -jar batch-weaver.jar
```

## 最佳实践

### 1. 选择合适的默认 Job

**建议**:
- 开发环境: 选择最常测试的 Job
- 生产环境: 选择最常运行的 Job
- 演示环境: 选择 `demoJob`

### 2. 提供清晰的提示

默认实现已经包含了清晰的提示信息：
```
No jobName provided. Using default job: demoJob
To run a specific job, use: java -jar app.jar jobName=<jobName>
```

### 3. 文档说明

在项目 README 中说明：
- 默认 Job 是什么
- 如何修改默认 Job
- 如何运行其他 Job

### 4. 测试验证

修改默认 Job 后，测试以下场景：
- ✅ 无参数启动
- ✅ 只传递其他参数
- ✅ 显式指定 jobName

## 常见问题

### Q1: 为什么在 BatchApplication 中处理而不是在 DynamicJobRunner 中？

**A**: 在 `BatchApplication.main()` 中处理有以下优势：
1. **更早处理**: 在应用启动前就完成参数处理
2. **更直观**: 入口点统一处理所有参数
3. **更灵活**: 易于修改和扩展
4. **更清晰**: 职责分离，`DynamicJobRunner` 只负责执行 Job

### Q2: 如果默认 Job 不存在会怎样？

**A**: 系统会报错并提示 Job 未找到：
```
Job not found: demoJob
Available jobs: [importJob, multiStepJob]
```

**解决方法**:
1. 确保默认 Job 的 XML 文件存在于 `src/main/resources/jobs/` 目录
2. 或修改 `DEFAULT_JOB_NAME` 为存在的 Job

### Q3: 可以禁用默认 Job 功能吗？

**A**: 可以，有两种方法：

**方法 1**: 注释掉默认参数处理代码
```java
public static void main(String[] args) {
    // 注释掉默认参数处理
    // if (!hasJobName && args.length == 0) { ... }
    
    SpringApplication.run(BatchApplication.class, args);
}
```

**方法 2**: 设置默认 Job 为 null（需要修改代码逻辑）
```java
private static final String DEFAULT_JOB_NAME = null;

// 添加 null 检查
if (DEFAULT_JOB_NAME != null && !hasJobName && args.length == 0) {
    args = new String[]{"jobName=" + DEFAULT_JOB_NAME};
}
```

### Q4: 如何查看所有可用的 Job？

**A**: 运行无参数命令，在日志中会显示：
```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar
```

或者查看 `src/main/resources/jobs/` 目录下的 XML 文件。

### Q5: 默认 Job 会影响断点续传吗？

**A**: 不会。断点续传使用 `resume=true` 参数，与 jobName 独立：
```bash
# 使用默认 Job 续传
java -jar app.jar resume=true

# 等价于
java -jar app.jar jobName=demoJob resume=true
```

## 扩展功能

### 未来可能的改进

1. **支持环境变量**
   ```java
   String defaultJobName = System.getenv("DEFAULT_JOB_NAME");
   if (defaultJobName == null) {
       defaultJobName = "demoJob";
   }
   ```

2. **支持配置文件**
   ```yaml
   batch:
     default-job-name: importJob
   ```

3. **支持多个默认 Job（按优先级）**
   ```java
   private static final String[] DEFAULT_JOBS = {"importJob", "demoJob"};
   ```

4. **支持默认参数**
   ```java
   private static final Map<String, String> DEFAULT_PARAMS = Map.of(
       "date", "today",
       "batchSize", "1000"
   );
   ```

## 总结

默认 Job 功能使得 BatchWeaver 更加易用：

✅ **开发友好**: 从 IDE 直接运行，无需配置  
✅ **灵活配置**: 修改一个常量即可更改默认 Job  
✅ **智能处理**: 自动合并参数，支持多种使用场景  
✅ **清晰提示**: 提供明确的使用说明  

通过合理配置默认 Job，可以大大提高开发效率和用户体验！

---

**文档版本**: 1.0  
**最后更新**: 2024-01-01  
**相关文档**: 
- [快速参考](QUICK_REFERENCE.md)
- [代码架构](CODE_ARCHITECTURE.md)
- [启动脚本说明](../scripts/README.md)
