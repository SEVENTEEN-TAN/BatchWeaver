# BatchWeaver - Spring Batch 5.x 多数据源批处理系统

基于 Spring Batch 5.x 的企业级批处理系统，支持多数据源、事务隔离、基于注解的文件处理框架。

## 核心特性

### 1. 元数据与业务事务隔离

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

### 2. 多数据源配置

| 数据源 | 用途 | 事务管理器 |
|--------|------|-----------|
| db1 | Spring Batch 元数据 + 业务数据 | tm1 |
| db2 | 业务数据库 2 | tm2 |
| db3 | 业务数据库 3 | tm3 |
| db4 | 业务数据库 4 | tm4 |

### 3. 基于注解的文件处理框架

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

## 📂 项目结构

```
batch-weaver/
├── src/main/java/com/batchweaver/
│   ├── config/
│   │   ├── datasource/          # 数据源配置（4 个数据源）
│   │   ├── batch/               # Batch 基础设施 + Job 配置
│   │   └── flatfile/            # FlatFile 框架配置
│   ├── batch/
│   │   ├── reader/              # 注解驱动的字段映射器
│   │   ├── processor/           # 数据清洗处理器
│   │   ├── writer/              # 数据写入器
│   │   └── validator/           # 首尾行校验器
│   ├── domain/
│   │   ├── annotation/          # @FileColumn 注解
│   │   ├── entity/              # 实体类
│   │   └── converter/           # 类型转换器
│   ├── service/                 # 业务服务层（多数据源 JdbcTemplate）
│   └── util/                    # 工具类（CSV 注入防护、路径校验）
├── src/main/resources/
│   ├── application.yml          # 配置文件
│   ├── schema-db1.sql           # db1 表结构
│   └── schema-db2.sql           # db2 表结构
└── src/test/java/               # 集成测试（事务隔离验证）
```

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

### 3. 初始化数据库表

```sql
-- 在 SQL Server 中创建数据库
CREATE DATABASE BatchWeaverDB;
CREATE DATABASE DB2_Business;

-- 执行表结构脚本
USE BatchWeaverDB;
-- Spring Batch 元数据表由框架自动创建

USE DB2_Business;
-- 执行 src/main/resources/schema-db2.sql
```

### 4. 运行项目

```bash
mvn clean install
mvn spring-boot:run
```

### 5. 运行测试

```bash
mvn test
```

## 事务隔离关键配置

### BatchInfrastructureConfig.java

```java
@Bean
public JobRepository jobRepository(
    @Qualifier("dataSource1") DataSource dataSource1,
    @Qualifier("tm1") PlatformTransactionManager tm1) {

    factory.setDataSource(dataSource1);       // ✅ db1
    factory.setTransactionManager(tm1);       // 绑定 tm1（元数据事务）
    ...
}
```

### DemoJobConfig.java

```java
@Bean
public Step importFileStep(
    JobRepository jobRepository,
    @Qualifier("tm2") PlatformTransactionManager tm2,
    ...) {

    return new StepBuilder("importFileStep", jobRepository)
        .transactionManager(tm2)  // 显式指定业务事务管理器 tm2
        .<DemoUser, DemoUser>chunk(100, tm2)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build();
}
```

### Db2BusinessService.java

```java
@Transactional(transactionManager = "tm2", propagation = Propagation.REQUIRED)
public void batchInsertUsers(List<DemoUser> users) {
    // 业务数据操作（使用 tm2）
}
```

## 📊 测试验证

### 事务隔离验证测试

运行 `TransactionIsolationTest.testMetadataCommitWhenBusinessRollback()`：

**验证标准**：
- ✅ BATCH_JOB_EXECUTION 表有 FAILED 记录（元数据提交）
- ✅ BATCH_STEP_EXECUTION 表有 FAILED 记录（元数据提交）
- ✅ DEMO_USER 表为空（业务数据回滚）

**失败场景**：如果业务表有数据残留，说明事务隔离配置错误！

## 📖 使用示例

### 文件格式

```
H|20261231|DEMO_FILE
1|John Doe|john@example.com|19900115
2|Jane Smith|jane@example.com|19850622
3|Bob Johnson|bob@example.com|19781203
T|3
```

### 实体类定义

```java
@Data
public class DemoUser {
    @FileColumn(index = 0, name = "userId")
    private Integer id;

    @FileColumn(index = 1, trim = true, toUpperCase = true)
    private String name;

    @FileColumn(index = 2, trim = true, defaultValue = "unknown@example.com")
    private String email;

    @FileColumn(index = 3, converter = StringToDateConverter.class)
    private Date birthDate;
}
```

## 🛠 技术栈

- **框架**：Spring Boot 3.5.7 + Spring Batch 5.x
- **数据库**：SQL Server 2022 + HikariCP
- **开发语言**：Java 21
- **构建工具**：Maven 3.8+

## 📋 核心类说明

| 类名 | 职责 |
|------|------|
| **BatchInfrastructureConfig** | 核心配置：JobRepository 绑定 tm1，确保元数据事务独立 |
| **DataSource1-4Config** | 4 个数据源配置，每个数据源独立的连接池和事务管理器 |
| **AnnotationDrivenFieldSetMapper** | 解析 @FileColumn 注解，自动完成字段映射和数据清洗 |
| **HeaderValidator/FooterValidator** | 首尾行格式校验和记录总数验证 |
| **CsvInjectionSanitizer** | CSV 注入防护（转义危险字符） |
| **FilePathNormalizer** | 路径安全校验（防止路径遍历攻击） |

## 📄 License

MIT License

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

---

**⚠️ 重要提示**：
1. 元数据事务（tm1）与业务事务（tm2/tm3/tm4）必须严格隔离
2. JobRepository 必须绑定 tm1
3. Step 必须显式指定业务事务管理器
4. Service 层 @Transactional 注解必须指定 transactionManager

违反以上原则将导致事务隔离失效！
