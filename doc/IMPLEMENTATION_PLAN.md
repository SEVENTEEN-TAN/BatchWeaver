# 实施计划 - BatchWeaver

## 目标描述
基于 JDK 21 和 Spring Batch 实现一个轻量级的批量任务调度系统。该系统允许通过 XML 定义 Job，执行 Java 类作为步骤，并支持带校验的数据导入/导出。它必须支持命令行触发、定时执行和断点续传。

## 用户审查要求
> [!IMPORTANT]
> XML 配置策略：由于 Spring Batch 5 移除了对传统 XML 配置的支持，我们将实现一个自定义 XML 解析器，用于读取简化的 XML 格式并动态定义 Job 与 Step。这允许在不依赖已弃用框架功能的情况下实现 XML 控制面。

> [!NOTE]
> 数据存储：我们将使用 H2 Database（文件模式）进行持久化，以支持 Job 续传和状态跟踪，无需安装外部 MySQL。这既满足“最小实现”的要求，也满足持久化的需求。

## 拟议变更

### 项目结构
#### [NEW] [pom.xml](../pom.xml)
- Spring Boot Starter Batch
- Spring Boot Starter Data JDBC
- Druid Starter
- H2 Database
- Jackson（用于 JSON）

#### [NEW] [application.properties](../src/main/resources/application.properties)
- Druid 数据源配置
- Spring Batch 初始化设置（始终执行脚本）
- H2 文件路径设置

### 核心逻辑 `com.example.batch`

#### [NEW] [BatchApplication.java](../src/main/java/com/example/batch/BatchApplication.java)
- 主程序入口

#### [NEW] [config/DruidConfig.java](../src/main/java/com/example/batch/config/DruidConfig.java)
- Druid 数据源 Bean 定义

#### [NEW] [core/XmlJobParser.java](../src/main/java/com/example/batch/core/XmlJobParser.java)
- 解析位于 `jobs/` 目录下的用户定义 XML 文件
- 将 XML 标签映射到 Spring Batch 的 `Job` 与 `Step` 构建器

#### [NEW] [core/DynamicJobRunner.java](../src/main/java/com/example/batch/core/DynamicJobRunner.java)
- `CommandLineRunner` 实现，用于处理 CLI 参数（`jobName`、`params`）
- 根据输入触发 Job

#### [NEW] [components/ReflectionTasklet.java](../src/main/java/com/example/batch/components/ReflectionTasklet.java)
- 通用 `Tasklet`，接收类名（来自 XML），实例化并执行指定方法（默认 `execute`）

#### [NEW] [components/FileValidator.java](../src/main/java/com/example/batch/components/FileValidator.java)
- 验证文件头（日期）与文件尾（总数）的逻辑

### 示例资源
#### [NEW] [jobs/demo-job.xml](../src/main/resources/jobs/demo-job.xml)
- 示例 Job 定义

## 验证计划

### 自动化测试
- 运行 `mvn test` 验证上下文加载

### 手动验证
1. CLI 触发：`java -jar app.jar jobName=demoJob date=2024-01-01`
2. 校验：提供一个尾行计数错误的文件，验证 Job 失败
3. 续传：修正文件后使用相同参数重新运行，验证 Job 从失败的 Step 继续执行
4. 数据持久化：查看 H2 控制台或日志，验证 Druid 激活且 Batch 元数据表已填充
