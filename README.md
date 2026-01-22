# BatchWeaver - Spring Batch 5.x 多数据源批处理系统

基于 Spring Batch 5.x 的企业级批处理系统，支持多数据源、事务隔离、**单次扫描大文件处理**、基于注解的文件处理框架。

## 核心特性

### 1. 单次扫描 + 延迟决策架构

**设计创新**：
- **零启动延迟** - 不需要在 `open()` 时预先扫描整个文件
- **O(1) 内存占用** - 只缓存两行数据（`prevLine` + `currentLine`）
- **单次顺序扫描** - 文件只被读取一次，完全遵循操作系统预读机制
- **Reader 自包含校验** - 头尾校验逻辑内置在 Reader 中，无需额外 Listener

**适用场景**：
- GB 级大文件处理
- 流式数据导入
- 金融对账、日志导入等批处理场景

**技术原理**：参见 [docs/文件读写.md](docs/文件读写.md)

### 2. 元数据与业务事务隔离

**设计原则**：
- **元数据事务（tm1Meta）**：绝对不受业务事务影响，必须提交成功
- **业务事务（tm1/tm2/tm3/tm4）**：失败时可以回滚
- **隔离保证**：Step 失败时，业务数据回滚，元数据记录 FAILED 状态

**失败场景流程**：
```
Step 执行失败时：
├── [回滚] 业务事务（tm2） → 业务数据不落库
└── [提交] 元数据事务（tm1Meta） → 记录 FAILED 状态，支持断点续传
```

**配置详情**：参见 [docs/多数据源.md](docs/多数据源.md)

### 3. 事务日志观测

**TransactionLogger** - 事务生命周期可视化工具：
```
================================================================================
[事务开始] ID: 1, 名称: step2SyncToDb3, 线程: [1-main]
   传播行为: REQUIRED, 隔离级别: DEFAULT, 只读: false
   活跃事务数: true
================================================================================
   [事务即将提交] ID: 1, 只读: false
   [事务已提交] ID: 1
================================================================================
[事务提交] ID: 1, 名称: step2SyncToDb3, 耗时: 45ms
================================================================================
```

**日志记录**：
- 事务开始/提交/回滚
- 传播行为、隔离级别
- SQL 执行跟踪
- 连接获取/释放

### 4. 多数据源配置

| 数据源 | 用途 | 事务管理器 |
|--------|------|-----------|
| db1 | Spring Batch 元数据 + 业务数据 | tm1Meta（元数据）/ tm1（业务） |
| db2 | 业务数据库 2 | tm2 |
| db3 | 业务数据库 3 | tm3 |
| db4 | 业务数据库 4 | tm4 |

### 5. Decider 模式

**条件分支设计**：
```
chunkProcessingStep → recordCountDecider
├─ VALID → Job COMPLETED
└─ INVALID → cleanupStep → Job FAILED (by Listener)
```

**设计优势**：
- 职责分离：Decider 只负责决策，Step 只负责执行
- 事务简化：Cleanup 不抛异常，由 Listener 设置状态
- 易于测试：每个组件可独立测试

### 6. 基于注解的文件处理框架

```java
@FileColumn(index = 0, name = "userId")
private Integer id;

@FileColumn(index = 1, trim = true, toUpperCase = true)
private String name;

@FileColumn(index = 2, defaultValue = "unknown@example.com")
private String email;
```

**支持**：
- 自动字段映射
- 数据清洗（trim、大小写转换、默认值）
- 首尾行校验
- 类型转换（String → Integer/Date/BigDecimal）
- CSV 注入防护
- 路径安全校验

**框架详情**：参见 [docs/技术框架.md](docs/技术框架.md)

---

## 项目结构

```
batch-weaver/
├── data/
│   ├── input/                   # 示例输入文件
│   └── output/                  # 导出文件输出目录
├── docs/                        # 文档目录
│   ├── 快速开始.md
│   ├── 技术框架.md
│   ├── 多数据源.md
│   ├── 文件读写.md
│   ├── 元数据表.md
│   └── 测试文档.md
├── script/
│   ├── run-job.sh                   # Linux/Mac: 运行单个 Job
│   ├── run-all-jobs.sh              # Linux/Mac: 运行所有 Job
│   ├── run-all-jobs-maven.ps1        # Windows: PowerShell 脚本
│   └── init.sql                 # Spring Batch 元数据表初始化脚本
├── src/main/java/com/batchweaver/
│   ├── BatchWeaverApplication.java
│   ├── core/                    # 框架核心
│   │   ├── annotation/          # @FileColumn 注解
│   │   ├── config/              # Batch 基础设施 + 数据源配置
│   │   │   └── datasource/      # dataSource1~4 + tm1~4
│   │   ├── converter/           # 类型转换器
│   │   ├── factory/             # 工厂模式（Reader/Writer 创建）
│   │   ├── fileprocess/         # 单次扫描：template/reader/writer/listener
│   │   ├── processor/           # 数据清洗处理器
│   │   ├── reader/              # 注解驱动字段映射
│   │   ├── scheduler/           # Job 启动/调度入口
│   │   ├── transaction/         # TransactionLogger 事务日志
│   │   ├── util/                # CSV 注入防护、路径校验
│   │   └── validator/           # 首尾行校验
│   └── demo/                    # 示例作业
│       ├── components/          # 共享组件
│       │   ├── SharedListenersConfig.java
│       │   ├── SharedProcessorsConfig.java
│       │   └── SharedWritersConfig.java
│       ├── config/jobs/         # 作业配置
│       │   ├── ChunkProcessingConfig.java    # Decider 模式示例
│       │   ├── ComplexWorkflowConfig.java    # 复杂工作流
│       │   ├── ConditionalFlowConfig.java    # 条件分支
│       │   ├── DemoJobConfig.java            # 基础 Chunk 模式
│       │   ├── FileExportConfig.java         # 文件导出
│       │   └── FileImportConfig.java         # 文件导入
│       ├── entity/              # 实体类
│       └── service/             # 业务服务
│           └── impl/            # 服务实现
├── src/main/resources/
│   ├── application.yml
│   ├── log4j2-spring.xml
│   ├── schema-db1.sql
│   └── schema-db2.sql
├── pom.xml
└── README.md
```

---

## 文档导航

| 文档 | 说明 |
|------|------|
| [快速开始.md](docs/快速开始.md) | 5分钟快速体验，环境配置与基础用法 |
| [技术框架.md](docs/技术框架.md) | 框架架构、设计模式、核心组件 |
| [多数据源.md](docs/多数据源.md) | 多数据源配置、事务隔离机制、TransactionLogger |
| [文件读写.md](docs/文件读写.md) | 单次扫描架构、HeaderFooterAwareReader |
| [元数据表.md](docs/元数据表.md) | Spring Batch 元数据表结构与断点续传 |
| [测试文档.md](docs/测试文档.md) | 测试用例、验证方法、性能基准 |
---

## 快速开始

### 1. 环境要求

- Java 21
- Maven 3.8+
- SQL Server 2022
- Spring Boot 3.5.7

### 2. 数据库配置

修改 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    db1:
      jdbc-url: jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB
      username: sa
      password: YourPassword123
    db2:
      jdbc-url: jdbc:sqlserver://localhost:1433;databaseName=DB2_Business
      username: sa
      password: YourPassword123
```

### 3. 初始化数据库

```sql
CREATE DATABASE BatchWeaverDB;
CREATE DATABASE DB2_Business;
CREATE DATABASE DB3_Business;
CREATE DATABASE DB4_Business;
```

### 4. 运行项目

```bash
mvn clean install
mvn spring-boot:run
```

### 5. 运行所有 Job 测试

**Windows (PowerShell)**:
```powershell
.\script\run-all-jobs-maven.ps1
```

**Linux/Mac (Bash)**:
```bash
./script/run-all-jobs.sh
```

**运行单个 Job**:
```bash
./script/run-job.sh demoJob
./script/run-job.sh chunkProcessingJob -l my.log
```

**详细步骤**：参见 [docs/快速开始.md](docs/快速开始.md)

---

## 技术栈

| 组件 | 版本/技术 |
|------|-----------|
| 框架 | Spring Boot 3.5.7 + Spring Batch 5.x |
| 数据库 | SQL Server 2022 + HikariCP |
| 语言 | Java 21 |
| 构建工具 | Maven 3.8+ |

---

## 核心类说明

### 工厂模式组件

| 类名 | 职责 |
|------|------|
| **BatchReaderFactory** | 统一创建和初始化 JdbcPagingItemReader |
| **BatchWriterFactory** | 创建 FlatFileItemWriter 并强制 stream 注册 |

### 单次扫描架构组件

| 类名 | 职责 |
|------|------|
| **HeaderFooterAwareReader** | 延迟决策 Reader - 单次扫描 + O(1) 内存 |
| **FooterLineDetector** | Footer 行检测函数式接口 |
| **FileImportJobTemplate** | 文件导入 Job 构建模板 |

### 事务管理组件

| 类名 | 职责 |
|------|------|
| **TransactionLogger** | 事务生命周期日志记录 |
| **BatchInfrastructureConfig** | JobRepository 绑定 tm1Meta |
| **DataSource1-4Config** | 4 个数据源配置 |

### 文件处理组件

| 类名 | 职责 |
|------|------|
| **AnnotationDrivenFieldSetMapper** | @FileColumn 注解解析 |
| **HeaderParser/FooterParser** | 头尾行解析 |
| **HeaderValidator/FooterValidator** | 头尾行校验 |

### 配置分层组件

| 类名 | 职责 |
|------|------|
| **SharedWritersConfig** | 共享 Writer Bean（db2/db3/db4） |
| **SharedListenersConfig** | 共享 Listener Bean（chunk/step/footer） |
| **SharedProcessorsConfig** | 共享 Processor Bean（数据转换） |

---

## Job 示例

| Job 名称 | 配置类 | 模式 | 说明 |
|---------|--------|------|------|
| **demoJob** | DemoJobConfig | Chunk | 基础文件导入 |
| **conditionalFlowJob** | ConditionalFlowConfig | Decider | 条件分支（根据 skip 数量） |
| **chunkProcessingJob** | ChunkProcessingConfig | Decider | 数据校验 + 清理流程 |
| **complexWorkflowJob** | ComplexWorkflowConfig | Decider | 多步骤 + 邮件通知 |
| **masterImportJob** | FileImportConfig | Chunk | 串行执行多个格式导入 |
| **format1/2/3ImportJob** | FileImportConfig | Chunk | 不同格式文件导入 |
| **format1/2ExportJob** | FileExportConfig | Chunk | 数据导出到文件 |

---

## 事务管理最佳实践

### Tasklet vs Chunk

| 模式 | 适用场景 | 事务特点 |
|------|---------|---------|
| **Tasklet** | 清理、同步、通知 | 整个 Step 1 个事务 |
| **Chunk** | 批量读写、ETL | 每个 Chunk 1 个事务 |

### 事务隔离铁律

```
[错误做法]
Step 使用默认事务管理器 → 可能误用 tm1Meta

[正确做法]
Step 显式指定 tm2/tm3/tm4 → 业务事务独立
JobRepository 使用 tm1Meta → 元数据事务独立
```

### 事务日志解读

```
[事务开始] → 事务开启
[SQL执行] → 数据库操作
[事务即将提交] → beforeCommit 回调
[事务已提交] → afterCommit 回调
[事务提交] → 事务完成
```

---

## License

MIT License

---

**重要提示**：
1. 元数据事务（tm1Meta）与业务事务（tm1/tm2/tm3/tm4）必须严格隔离
2. JobRepository 必须绑定 tm1Meta
3. Step 必须显式指定业务事务管理器
4. Service 层 @Transactional 注解必须指定 transactionManager

违反以上原则将导致事务隔离失效！
