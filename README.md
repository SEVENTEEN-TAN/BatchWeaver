# BatchWeaver

> 基于 Spring Batch 的 XML 动态编排引擎

BatchWeaver 是一个轻量级的批量任务调度系统，通过 XML 配置实现 Job 的动态编排和执行。

## 核心特性

✅ **XML 动态编排** - 通过 XML 定义 Job 流程，无需重新编译  
✅ **命令行触发** - 支持 Shell/Bat 脚本主动触发  
✅ **文件校验** - 内置数据文件格式校验（日期+总数）  
✅ **数据导入/导出** - 支持 CSV/JSON/TXT 格式  
✅ **断点续传** - Job 失败后可从断点继续执行  
✅ **Druid 连接池** - 高性能数据库连接管理  
✅ **参数传递** - 运行时动态传入参数

## 快速开始

### 1. 环境要求
- JDK 21+
- Maven 3.6+

### 2. 构建项目
```bash
mvn clean package -DskipTests
```

### 3. 运行 Job

**方式 1: 使用启动脚本（推荐）**

```bash
# Windows
scripts\run-job.bat jobName=importJob

# Linux/Mac (首次需要添加执行权限: chmod +x scripts/run-job.sh)
./scripts/run-job.sh jobName=importJob

# 传递参数
scripts\run-job.bat jobName=importJob date=2024-01-01

# 断点续传
scripts\run-job.bat jobName=importJob resume=true
```

**方式 2: 直接运行 JAR**

```bash
# 运行指定的 Job
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=importJob

# 不指定 jobName 时，默认运行 demoJob
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar
```

**方式 3: 从 IDE 直接启动**

在 IntelliJ IDEA 或 Eclipse 中直接运行 `BatchApplication.main()` 方法：
- 无参数启动：自动运行 `demoJob`（默认 Job）
- 带参数启动：在运行配置中添加程序参数 `jobName=importJob`

详细说明请查看 [scripts/README.md](scripts/README.md)

## Job 定义示例

### XML 配置（`src/main/resources/jobs/my-job.xml`）
```xml
<job id="myJob">
    <step id="validate" className="com.example.batch.steps.FileValidator" methodName="execute">
        <property name="filePath" value="data/input.csv"/>
    </step>
    <step id="import" className="com.example.batch.steps.DataImporter" methodName="execute">
        <property name="filePath" value="data/input.csv"/>
    </step>
</job>
```

### Java 处理类
```java
@Component
public class FileValidator {
    private String filePath;
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public void execute() {
        // 校验逻辑
    }
}
```

## 架构设计

```
1个 XML 文件 = 1个 Job
1个 Job = 多个 Step（按顺序执行）
1个 Step = 1个 Java 类的方法
```

## 📚 文档

### � 档快速入门
| 文档 | 说明 | 推荐度 |
|------|------|--------|
| **[快速参考](doc/QUICK_REFERENCE.md)** | 常用代码片段、配置模板、命令速查 | ⭐⭐⭐⭐⭐ |
| **[启动脚本说明](scripts/README.md)** | 脚本使用方法、IDE 启动配置 | ⭐⭐⭐⭐ |

### 🏗️ 架构与设计
| 文档 | 说明 | 推荐度 |
|------|------|--------|
| **[代码架构](doc/CODE_ARCHITECTURE.md)** | 深入理解项目实现、核心组件详解 | ⭐⭐⭐⭐⭐ |
| **[执行流程](doc/EXECUTION_FLOW.md)** | 可视化流程图、断点续传机制 | ⭐⭐⭐⭐ |
| **[设计文档](doc/JOB_DESIGN.md)** | Job/Step 架构设计和最佳实践 | ⭐⭐⭐⭐ |

### 🔧 配置与扩展
| 文档 | 说明 | 推荐度 |
|------|------|--------|
| **[默认 Job 配置](doc/DEFAULT_JOB_GUIDE.md)** | 如何配置和使用默认 Job | ⭐⭐⭐ |
| **[实施计划](doc/IMPLEMENTATION_PLAN.md)** | 技术方案和架构决策 | ⭐⭐⭐ |

### 📖 其他文档
| 文档 | 说明 |
|------|------|
| **[文档索引](doc/INDEX.md)** | 按场景查找文档、关键概念速查 |
| **[完整指南](doc/WALKTHROUGH.md)** | 项目交付文档、验证结果 |
| **[更新日志](CHANGELOG.md)** | 项目更新记录和新功能说明 |

> 💡 **推荐阅读顺序**: 快速参考 → 代码架构 → 执行流程 → 设计文档

## 数据库

- **引擎**: SQL Server 2022
- **数据库名**: `BatchWeaverDB`
- **用户表**: `USER_DATA`
- **元数据**: `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION`
- **配置指南**: [SQL Server 配置文档](doc/SQLSERVER_SETUP.md)

## 技术栈

- Spring Boot 3.2.0
- Spring Batch 5.x
- Druid 连接池
- SQL Server 2022
- JDK 21

## 示例 Job

项目内置了多个示例 Job：

- `importJob` - CSV 数据导入（带文件校验）
- `multiStepJob` - 多步骤处理演示
- `demoJob` - 简单的单步骤示例

## License

MIT License