# BatchWeaver - Spring Batch 动态编排引擎

<div align="center">

**基于 Spring Batch 的 XML 动态编排引擎，支持零代码配置批处理任务**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5.x-blue.svg)](https://spring.io/projects/spring-batch)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![SQL Server](https://img.shields.io/badge/SQL%20Server-2022-red.svg)](https://www.microsoft.com/sql-server)

</div>

---

## 📖 项目简介

BatchWeaver 是一个轻量级的 Spring Batch 动态编排框架，通过 **XML 配置** + **反射机制** 实现批处理任务的零代码编排。开发者只需编写业务逻辑 Service，然后在 XML 中配置调用流程，即可快速构建复杂的批处理任务。

### 核心特性

- ✅ **动态配置**: 通过 XML 定义 Job 流程，无需修改 Java 代码
- ✅ **反射调用**: 自动解析 XML 并通过反射调用 Spring Bean 方法
- ✅ **断点续传**: 原生支持 Spring Batch 的失败重启机制
- ✅ **元数据管理**: 完整的执行历史、状态追踪和统计信息
- ✅ **轻量高效**: 使用 HikariCP 连接池，启动快速，资源占用低

---

## 🏗️ 技术栈

| 技术 | 版本 | 用途 | 评分 |
|------|------|------|------|
| **Spring Boot** | 3.2.0 | 应用框架 | ⭐⭐⭐⭐⭐ |
| **Spring Batch** | 5.x | 批处理框架 | ⭐⭐⭐⭐⭐ |
| **Java** | 21 | 开发语言（LTS 版本） | ⭐⭐⭐⭐⭐ |
| **SQL Server** | 2022 | 元数据存储 | ⭐⭐⭐⭐ |
| **HikariCP** | 默认 | 数据库连接池（轻量高效） | ⭐⭐⭐⭐⭐ |
| **Jackson XML** | - | XML 解析 | ⭐⭐⭐⭐ |
| **Lombok** | - | 代码简化 | ⭐⭐⭐ |

### 技术选型评估

#### ✅ 优势
- **Spring Batch 5.x**: 企业级批处理标准，成熟稳定，社区活跃
- **Java 21**: 最新 LTS 版本，性能优异，支持虚拟线程等新特性
- **XML 动态编排**: 降低开发成本，提高配置灵活性
- **HikariCP**: Spring Boot 默认连接池，零配置，性能卓越，适合批处理场景

#### ⚠️ 注意事项
- **SQL Server**: 企业版需要授权费用，可考虑替换为 PostgreSQL/MySQL
- **反射机制**: 性能略低于直接调用，但在批处理场景下影响可忽略
- **XML 配置**: 缺乏编译时检查，需要加强运行时验证

#### 📊 综合评分: **4.7/5.0**

---

## 🚀 快速开始

### 环境要求

- **JDK**: 21+
- **Maven**: 3.6+
- **SQL Server**: 2022 或兼容版本
- **IDE**: IntelliJ IDEA / Eclipse（推荐 IDEA）

### 1. 克隆项目

```bash
git clone <repository-url>
cd SpringBatch
```

### 2. 初始化数据库

在 SQL Server 中执行初始化脚本：

```bash
# 连接到 SQL Server
sqlcmd -S localhost -U sa -P YourPassword

# 创建数据库
CREATE DATABASE BatchWeaverDB;
GO

# 执行初始化脚本
USE BatchWeaverDB;
GO
:r scripts/init.sql
GO
```

### 3. 配置数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB;encrypt=true;trustServerCertificate=true
    username: sa
    password: YourStrong!Passw0rd
```

### 4. 编译打包

```bash
mvn clean package -DskipTests
```

### 5. 运行示例任务

#### 运行 Demo Job（数据流转）
```bash
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=demoJob
```

#### 运行 Breakpoint Job（断点续传）
```bash
# 首次运行（会失败）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=breakpointJob

# 再次运行（自动续传）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=breakpointJob
```

---

## 📚 文档导航

| 文档 | 说明 |
|------|------|
| [框架设计文档](doc/FRAMEWORK_DESIGN.md) | 核心设计原理、组件说明、扩展方式 |
| [快速开始指南](doc/QUICK_START.md) | 环境准备、运行示例、断点续传详解 |
| [元数据表说明](doc/METADATA_TABLES.md) | Spring Batch 元数据表结构、查询示例、最佳实践 |

---

## 🎯 使用方法

### 方式一: 命令行运行

```bash
# 基本用法
java -jar app.jar jobName=<jobName>

# 传递参数
java -jar app.jar jobName=demoJob param1=value1 param2=value2

# 强制创建新实例
java -jar app.jar jobName=demoJob restart=false
```

### 方式二: IDE 运行

在 IntelliJ IDEA 中配置 Program Arguments：

```
jobName=demoJob
```

### 方式三: 编写自定义 Job

#### Step 1: 创建业务 Service

```java
@Service
public class MyService {
    public void processData() {
        // 业务逻辑
    }
}
```

#### Step 2: 配置 XML

在 `src/main/resources/jobs/` 下创建 `my-job.xml`：

```xml
<job id="myJob">
    <step id="step1">
        <className>com.example.batch.service.MyService</className>
        <methodName>processData</methodName>
    </step>
</job>
```

#### Step 3: 运行

```bash
java -jar app.jar jobName=myJob
```

---

## 📊 项目结构

```
SpringBatch/
├── src/main/java/com/example/batch/
│   ├── BatchApplication.java          # 启动类
│   ├── core/                           # 核心框架
│   │   ├── XmlJobParser.java          # XML 解析器
│   │   ├── DynamicJobRunner.java      # 动态 Job 运行器
│   │   └── model/                      # XML 映射模型
│   ├── components/                     # 组件
│   │   └── ReflectionTasklet.java     # 反射 Tasklet
│   └── service/                        # 业务服务
│       ├── DemoService.java           # 示例服务
│       └── BreakpointService.java     # 断点续传示例
├── src/main/resources/
│   ├── application.yml                 # 应用配置
│   └── jobs/                           # Job XML 配置
│       ├── demo-job.xml               # 数据流转示例
│       └── breakpoint-job.xml         # 断点续传示例
├── scripts/
│   ├── init.sql                        # 数据库初始化脚本
│   ├── run-job.bat                     # Windows 运行脚本
│   └── run-job.sh                      # Linux 运行脚本
└── doc/                                # 文档目录
    ├── FRAMEWORK_DESIGN.md            # 框架设计
    ├── QUICK_START.md                 # 快速开始
    └── METADATA_TABLES.md             # 元数据表说明
```

---

## 🔧 配置说明

### HikariCP 连接池配置（推荐）

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 3           # 最大连接数（批处理场景无需太多）
      minimum-idle: 1                # 最小空闲连接
      connection-timeout: 30000      # 连接超时时间（毫秒）
```

### Batch 配置

```yaml
spring:
  batch:
    jdbc:
      initialize-schema: always    # 开发环境: always，生产环境: never
    job:
      enabled: false               # 禁用自动执行，改为命令行触发
```

---

## 📈 监控与运维

### 查看执行历史

```sql
-- 查看最近的 Job 执行记录
SELECT 
    ji.JOB_NAME,
    je.STATUS,
    je.START_TIME,
    je.END_TIME,
    DATEDIFF(SECOND, je.START_TIME, je.END_TIME) AS DURATION_SECONDS
FROM BATCH_JOB_EXECUTION je
JOIN BATCH_JOB_INSTANCE ji ON je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
ORDER BY je.START_TIME DESC;
```

### 查看失败任务

```sql
-- 查看所有失败的 Job
SELECT 
    ji.JOB_NAME,
    je.JOB_EXECUTION_ID,
    je.EXIT_MESSAGE
FROM BATCH_JOB_EXECUTION je
JOIN BATCH_JOB_INSTANCE ji ON je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
WHERE je.STATUS = 'FAILED';
```

更多查询示例请参考 [元数据表说明文档](doc/METADATA_TABLES.md)。

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

---

## 📄 许可证

本项目采用 MIT 许可证。

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给个 Star！⭐**

</div>