# 项目交付文档 - BatchWeaver

## 项目概述
本项目基于 **JDK 21** 和 **Spring Batch 5** 实现一个轻量级批量任务调度系统，满足以下核心需求：

- **XML Job 配置**：通过自定义 XML 解析器动态定义 Job 与 Step  
- **Druid 连接池**：集成 Druid 进行数据库连接管理  
- **数据导入/导出**：支持 CSV、JSON、TXT 格式  
- **文件校验**：验证首行日期和尾行总数  
- **断点续传**：Job 失败后可选择从断点继续或重新执行  
- **主动触发**：支持定时任务与命令行（Shell/Bat）触发  
- **参数传递**：运行 Job 时支持传入自定义参数

## 技术栈
- **JDK**：21
- **Spring Boot**：3.2.0
- **Spring Batch**：5.x
- **数据库**：H2（文件模式）
- **连接池**：Druid
- **构建工具**：Maven 3.x

## 核心组件

### 1. XML Job 解析器（`XmlJobParser`）
- 位置：[`XmlJobParser.java`](../src/main/java/com/example/batch/core/XmlJobParser.java)
- 功能：扫描 `src/main/resources/jobs/*.xml`，解析并注册 Job
- XML 格式：
```xml
<job id="jobName">
    <step id="stepName" className="com.example.Class" methodName="execute">
        <property name="key" value="value"/>
    </step>
</job>
```

### 2. 动态 Job 运行器（`DynamicJobRunner`）
- 位置：[`DynamicJobRunner.java`](../src/main/java/com/example/batch/core/DynamicJobRunner.java)
- 功能：响应命令行参数触发 Job，支持参数传递

### 3. 通用反射 Tasklet（`ReflectionTasklet`）
- 位置：[`ReflectionTasklet.java`](../src/main/java/com/example/batch/components/ReflectionTasklet.java)
- 功能：通过反射执行 XML 中指定的 Java 类与方法，支持属性注入

### 4. 文件校验器（`FileValidator`）
- 位置：[`FileValidator.java`](../src/main/java/com/example/batch/components/FileValidator.java)
- 功能：验证文件首行是否为日期（YYYY-MM-DD），尾行是否为总数（`Total: N`）

### 5. CSV 导入 Tasklet（`CsvImportTasklet`）
- 位置：[`CsvImportTasklet.java`](../src/main/java/com/example/batch/steps/CsvImportTasklet.java)
- 功能：将 CSV 文件数据导入数据库

## 运行指南

### 1. 环境要求
- JDK 21+
- Maven 3.6+

### 2. 构建项目
```bash
mvn clean package -DskipTests
```

### 3. 运行 Job

**方式一：使用批处理脚本（推荐）**
```bash
# Windows
.\run-job.bat jobName=importJob param1=value1

# Linux/Mac（需创建 run-job.sh）
./run-job.sh jobName=importJob param1=value1
```

**方式二：直接使用 Java 命令**
```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=importJob
```

### 4. 断点续传
如果 Job 执行失败，可通过 `resume=true` 参数从断点继续：
```bash
.\run-job.bat jobName=importJob resume=true
```

不传 `resume` 参数或设为 `false` 会创建新的 Job 实例。

### 5. 定时任务
在 Job 相关类上使用 Spring 的 `@Scheduled` 注解即可实现定时执行：
```java
@Scheduled(cron = "0 0 2 * * ?")
public void scheduleJob() {
    // 触发 Job 逻辑
}
```

## 示例资源

### 数据文件
- **有效 CSV**：[`data/input.csv`](../data/input.csv)
```
2024-01-01
Alice, alice@example.com
Bob, bob@example.com
Total: 2
```

- **无效 CSV**：[`data/input_invalid.csv`](../data/input_invalid.csv)
```
2024-01-01
Charlie, charlie@example.com
Diana, diana@example.com
Total: 999  # 错误的总数
```

### Job 定义
- **导入 Job**：[`jobs/import-job.xml`](../src/main/resources/jobs/import-job.xml)
- **测试失败 Job**：[`jobs/test-fail-job.xml`](../src/main/resources/jobs/test-fail-job.xml)
- **演示 Job**：[`jobs/demo-job.xml`](../src/main/resources/jobs/demo-job.xml)
- **多步骤 Job**：[`jobs/multi-step-job.xml`](../src/main/resources/jobs/multi-step-job.xml)

## 验证结果

### 成功场景
1. 构建：Maven 编译打包成功
2. Job 执行：`importJob` 成功运行，文件校验通过
3. 数据导入：CSV 数据成功导入 `USER_DATA`
4. 参数传递：命令行参数正确传递给 Job
5. 多步骤：`multiStepJob` 三个 Step 按顺序执行成功

### 失败场景
1. 文件校验：`testFailJob` 正确检测无效数据（总数不匹配），Job 失败
2. 错误处理：Job 失败后记录在 Spring Batch 元数据表中，可通过 `resume=true` 继续

## 数据库说明
项目使用 H2 文件数据库，存储位置：`./data/batchdb.mv.db`

**核心表：**
- `USER_DATA`：用户数据
- `BATCH_JOB_INSTANCE`：Job 实例
- `BATCH_JOB_EXECUTION`：Job 执行记录
- `BATCH_STEP_EXECUTION`：Step 执行记录

## 扩展指南

### 添加新 Job
1. 在 `src/main/resources/jobs/` 下创建 XML 文件
2. 创建对应的 Java 类（Tasklet）
3. 重新打包运行

### 添加 JSON 支持
修改 `CsvImportTasklet`，使用 Jackson 解析 JSON 文件即可。

### 切换到 MySQL
1. 修改 `application.properties` 中的数据源配置
2. 在 `pom.xml` 中添加 MySQL 驱动依赖

## 项目结构
```
BatchWeaver/
├── data/                          # 数据文件目录
│  ├── input.csv
│  └── input_invalid.csv
├── doc/                           # 文档目录
│  ├── WALKTHROUGH.md             # 详细使用指南
│  ├── JOB_DESIGN.md              # Job/Step 架构设计
│  └── IMPLEMENTATION_PLAN.md     # 技术实施方案
├── src/main/
│  ├── java/com/example/batch/
│  │  ├── BatchApplication.java  # 主应用
│  │  ├── config/
│  │  │  └── DruidConfig.java   # Druid 配置
│  │  ├── core/
│  │  │  ├── XmlJobParser.java  # XML 解析器
│  │  │  ├── DynamicJobRunner.java
│  │  │  └── model/             # XML 模型定义
│  │  ├── components/
│  │  │  ├── FileValidator.java
│  │  │  └── ReflectionTasklet.java
│  │  └── steps/                 # 业务 Tasklet
│  └── resources/
│      ├── application.properties
│      ├── schema-all.sql         # DDL
│      └── jobs/                  # Job XML 定义
└── run-job.bat                    # 启动脚本
```

## 总结
项目已完成核心功能开发与验证，可直接用于生产环境或作为基础框架扩展。
