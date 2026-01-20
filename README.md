# BatchWeaver - Spring Batch 5.x 多数据源批处理系统

基于 Spring Batch 5.x 的企业级批处理系统，支持多数据源、事务隔离、**单次扫描大文件处理**、基于注解的文件处理框架。

## 核心特性

### 1. 单次扫描 + 延迟决策架构 🚀

**设计创新**：
- **零启动延迟** - 不需要在 `open()` 时预先扫描整个文件
- **O(1) 内存占用** - 只缓存两行数据（`prevLine` + `currentLine`）
- **单次顺序扫描** - 文件只被读取一次，完全遵循操作系统预读机制
- **Reader 自包含校验** - 头尾校验逻辑内置在 Reader 中，无需额外 Listener

**适用场景**：
- GB 级大文件处理
- 流式数据导入
- 金融对账、日志导入等批处理场景

📖 **技术原理**：参见 [docs/文件读写.md](docs/文件读写.md)

### 2. 元数据与业务事务隔离

**设计原则**：
- **元数据事务（tm1）**：绝对不受业务事务影响，必须提交成功
- **业务事务（tm2/tm3/tm4）**：失败时可以回滚
- **隔离保证**：Step 失败时，业务数据回滚，元数据记录 FAILED 状态

**失败场景流程**：
```
Step 执行失败时：
├── ❌ 业务事务（tm2）回滚 → 业务数据不落库
└── ✅ 元数据事务（tm1）提交 → 记录 FAILED 状态，支持断点续传
```

📖 **配置详情**：参见 [docs/多数据源.md](docs/多数据源.md)

### 3. 多数据源配置

| 数据源 | 用途 | 事务管理器 |
|--------|------|-----------|
| db1 | Spring Batch 元数据 + 业务数据 | tm1 |
| db2 | 业务数据库 2 | tm2 |
| db3 | 业务数据库 3 | tm3 |
| db4 | 业务数据库 4 | tm4 |

### 4. 基于注解的文件处理框架

```java
@FileColumn(index = 0, name = "userId")
private Integer id;

@FileColumn(index = 1, trim = true, toUpperCase = true)
private String name;

@FileColumn(index = 2, defaultValue = "unknown@example.com")
private String email;
```

支持：
- 自动字段映射
- 数据清洗（trim、大小写转换、默认值）
- 首尾行校验
- 类型转换（String → Integer/Date/BigDecimal）
- CSV 注入防护
- 路径安全校验

📖 **框架详情**：参见 [docs/技术框架.md](docs/技术框架.md)

---

## 📂 项目结构

```
batch-weaver/
├── src/main/java/com/batchweaver/
│   ├── config/
│   │   ├── datasource/          # 数据源配置（4 个数据源）
│   │   ├── batch/               # Batch 基础设施 + Job 配置
│   │   └── flatfile/            # FlatFile 框架配置
│   ├── core/
│   │   └── fileprocess/
│   │       ├── template/        # Job 构建模板
│   │       ├── reader/          # HeaderFooterAwareReader
│   │       ├── function/        # 函数式接口
│   │       └── model/           # 数据模型
│   ├── batch/
│   │   ├── reader/              # 注解驱动的字段映射器
│   │   ├── processor/           # 数据清洗处理器
│   │   └── writer/              # 数据写入器
│   ├── domain/
│   │   ├── annotation/          # @FileColumn 注解
│   │   ├── entity/              # 实体类
│   │   └── converter/           # 类型转换器
│   ├── service/                 # 业务服务层
│   └── util/                    # 工具类（CSV 注入防护、路径校验）
├── src/main/resources/
│   └── application.yml          # 配置文件
├── docs/                        # 文档目录
│   ├── 快速开始.md              # 5分钟快速体验指南
│   ├── 技术框架.md              # 框架架构与设计模式
│   ├── 多数据源.md              # 多数据源配置与事务隔离
│   ├── 文件读写.md              # 单次扫描架构设计
│   └── 测试文档.md              # 测试用例与验证方法
└── src/test/java/               # 集成测试
```

---

## 📖 文档导航

| 文档 | 说明 |
|------|------|
| [快速开始.md](docs/快速开始.md) | 5分钟快速体验，环境配置与基础用法 |
| [技术框架.md](docs/技术框架.md) | 框架架构、设计模式、核心组件 |
| [多数据源.md](docs/多数据源.md) | 多数据源配置、事务隔离机制 |
| [文件读写.md](docs/文件读写.md) | 单次扫描架构、HeaderFooterAwareReader |
| [测试文档.md](docs/测试文档.md) | 测试用例、验证方法、性能基准 |

---

## 🚀 快速开始

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
```

### 3. 初始化数据库

```sql
CREATE DATABASE BatchWeaverDB;
CREATE DATABASE DB2_Business;
```

### 4. 运行项目

```bash
mvn clean install
mvn spring-boot:run
```

📖 **详细步骤**：参见 [docs/快速开始.md](docs/快速开始.md)

---

## 🛠 技术栈

| 组件 | 版本/技术 |
|------|-----------|
| 框架 | Spring Boot 3.5.7 + Spring Batch 5.x |
| 数据库 | SQL Server 2022 + HikariCP |
| 语言 | Java 21 |
| 构建工具 | Maven 3.8+ |

---

## 📋 核心类说明

### 单次扫描架构组件

| 类名 | 职责 |
|------|------|
| **HeaderFooterAwareReader** | 延迟决策 Reader - 单次扫描 + O(1) 内存 |
| **FooterLineDetector** | Footer 行检测函数式接口 |
| **FileImportJobTemplate** | 文件导入 Job 构建模板 |

### 基础设施组件

| 类名 | 职责 |
|------|------|
| **BatchInfrastructureConfig** | JobRepository 绑定 tm1 |
| **DataSource1-4Config** | 4 个数据源配置 |

### 文件处理组件

| 类名 | 职责 |
|------|------|
| **AnnotationDrivenFieldSetMapper** | @FileColumn 注解解析 |
| **HeaderParser/FooterParser** | 头尾行解析 |
| **HeaderValidator/FooterValidator** | 头尾行校验 |

---

## 📄 License

MIT License

---

**⚠️ 重要提示**：
1. 元数据事务（tm1）与业务事务（tm2/tm3/tm4）必须严格隔离
2. JobRepository 必须绑定 tm1
3. Step 必须显式指定业务事务管理器
4. Service 层 @Transactional 注解必须指定 transactionManager

违反以上原则将导致事务隔离失效！
