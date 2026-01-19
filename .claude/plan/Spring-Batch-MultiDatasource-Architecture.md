# Spring Batch 5.x 多数据源批处理系统 - 架构规划文档

**生成时间**：2026-01-19
**方案选择**：方案 A - 单 JobRepository + 多业务事务管理器
**技术评分**：9/10

---

## ⚠️ 核心设计铁律（必读）

### 🔴 事务隔离绝对原则

**db1 双重角色声明**：
- ✅ **元数据存储**：Spring Batch 框架元数据表（BATCH_JOB_EXECUTION、BATCH_STEP_EXECUTION 等）
- ✅ **业务数据存储**：db1 自己的业务表

**事务独立性铁律**：
```
┌─────────────────────────────────────────────────────────────┐
│  元数据事务（tm1）： 绝对不受业务事务影响，必须提交成功！  │
│  - 即使 Step 执行失败，元数据也必须记录 FAILED 状态         │
│  - 确保断点续传、失败重试机制的可靠性                       │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  业务事务（tm2/tm3/tm4）： 失败时可以回滚                  │
│  - Step 执行失败时，业务数据不会被持久化                    │
│  - 保证数据一致性和完整性                                   │
└─────────────────────────────────────────────────────────────┘
```

**失败场景保证机制**：
```
Step 执行流程：
├── 开始执行 Step
│   ├── [tm1 事务] 元数据写入：STEP_EXECUTION 状态 = STARTED
│   └── [tm2 事务] 业务数据写入：INSERT INTO db2.DEMO_USER ...
│
├── Step 执行失败（如：数据库约束冲突）
│   ├── ❌ [tm2 事务] 业务事务回滚 → db2 数据不落库
│   └── ✅ [tm1 事务] 元数据事务提交 → STEP_EXECUTION 状态 = FAILED
│
└── 结果
    ├── ✅ 元数据表记录了失败状态（支持断点续传）
    └── ✅ 业务数据保持一致性（脏数据已回滚）
```

**实现保证**：
1. **JobRepository 独立事务**：JobRepository/JobExplorer 绑定 tm1，与业务事务完全隔离
2. **Step 显式事务绑定**：每个 Step 通过 `.transactionManager(tm2/tm3/tm4)` 显式指定业务事务管理器
3. **禁止混用事务管理器**：严禁在 Service 层使用 tm1 进行业务操作

---

## 1. 方案概述

### 1.1 核心架构
- **db1**：承载 Spring Batch 元数据表 + 业务数据（主数据源）
- **db2/db3/db4**：纯业务数据库
- **JobRepository**：绑定 db1 的事务管理器（tm1），**独立于业务事务**
- **Step 事务**：每个 Step 显式指定业务事务管理器（tm2/tm3/tm4），**与元数据事务隔离**

### 1.2 事务隔离原则
- 🔴 **元数据事务（tm1）**：**绝对不受业务事务影响，必须提交成功！** 管理 Batch 框架元数据（BATCH_JOB_EXECUTION、BATCH_STEP_EXECUTION 等）
- ✅ **业务事务（tm2/tm3/tm4）**：管理各自数据源的业务数据操作，失败时可以回滚
- ✅ **隔离效果**：Step 失败时，业务事务回滚，元数据事务正常提交并记录 FAILED 状态，确保断点续传机制可靠

---

## 2. 完整目录结构

```
batch-weaver/
├── src/main/java/com/batchweaver/
│   ├── BatchWeaverApplication.java                    # Spring Boot 启动类
│   ├── config/
│   │   ├── datasource/
│   │   │   ├── DataSource1Config.java                 # db1 数据源配置（元数据 + 业务）
│   │   │   ├── DataSource2Config.java                 # db2 数据源配置
│   │   │   ├── DataSource3Config.java                 # db3 数据源配置
│   │   │   └── DataSource4Config.java                 # db4 数据源配置
│   │   ├── batch/
│   │   │   ├── BatchInfrastructureConfig.java         # Batch 元数据配置（JobRepository/Launcher）
│   │   │   └── DemoJobConfig.java                     # 示例 Job 配置
│   │   └── flatfile/
│   │       ├── FlatFileReaderFactory.java             # 文件 Reader 工厂
│   │       ├── FlatFileWriterFactory.java             # 文件 Writer 工厂
│   │       └── FileValidatorConfig.java               # 首尾行校验器配置
│   ├── batch/
│   │   ├── reader/
│   │   │   ├── AnnotationDrivenFieldSetMapper.java    # 基于注解的字段映射器
│   │   │   └── HeaderFooterValidatingReader.java      # 带首尾行校验的 Reader
│   │   ├── processor/
│   │   │   ├── DataCleansingProcessor.java            # 数据清洗处理器
│   │   │   └── TypeConversionProcessor.java           # 类型转换处理器
│   │   ├── writer/
│   │   │   └── MultiDataSourceJdbcWriter.java         # 多数据源 JDBC Writer
│   │   └── validator/
│   │       ├── HeaderValidator.java                   # 首行校验器
│   │       ├── FooterValidator.java                   # 尾行校验器
│   │       └── FilePathSecurityValidator.java         # 文件路径安全校验器
│   ├── domain/
│   │   ├── annotation/
│   │   │   ├── FileColumn.java                        # 文件列映射注解
│   │   │   ├── FileHeader.java                        # 文件首行标记注解
│   │   │   └── FileFooter.java                        # 文件尾行标记注解
│   │   ├── entity/
│   │   │   └── DemoUser.java                          # 示例实体类
│   │   └── converter/
│   │       ├── TypeConverter.java                     # 类型转换器接口
│   │       ├── StringToIntegerConverter.java          # String → Integer
│   │       ├── StringToDateConverter.java             # String → Date
│   │       └── StringToBigDecimalConverter.java       # String → BigDecimal
│   ├── service/
│   │   ├── Db1BusinessService.java                    # db1 业务服务
│   │   ├── Db2BusinessService.java                    # db2 业务服务
│   │   ├── Db3BusinessService.java                    # db3 业务服务
│   │   └── Db4BusinessService.java                    # db4 业务服务
│   └── util/
│       ├── CsvInjectionSanitizer.java                 # CSV 注入防护
│       └── FilePathNormalizer.java                    # 路径规范化工具
├── src/main/resources/
│   ├── application.yml                                 # 主配置文件
│   ├── schema-db1.sql                                  # db1 表结构（Batch 元数据 + 业务表）
│   ├── schema-db2.sql                                  # db2 表结构
│   ├── schema-db3.sql                                  # db3 表结构
│   └── schema-db4.sql                                  # db4 表结构
├── src/test/java/com/batchweaver/
│   └── batch/
│       └── DemoJobIntegrationTest.java                # 集成测试
└── pom.xml                                             # Maven 依赖配置
```

---

## 3. 核心类职责清单

| 类名 | 职责 | 关键方法 |
|------|------|---------|
| **DataSource1Config** | 配置 db1（元数据 + 业务）的 DataSource、JdbcTemplate、TransactionManager | `dataSource1()`, `jdbcTemplate1()`, `namedJdbcTemplate1()`, `tm1()` |
| **DataSource2Config** | 配置 db2 的 DataSource、JdbcTemplate、TransactionManager | `dataSource2()`, `jdbcTemplate2()`, `namedJdbcTemplate2()`, `tm2()` |
| **DataSource3Config** | 配置 db3 的 DataSource、JdbcTemplate、TransactionManager | `dataSource3()`, `jdbcTemplate3()`, `namedJdbcTemplate3()`, `tm3()` |
| **DataSource4Config** | 配置 db4 的 DataSource、JdbcTemplate、TransactionManager | `dataSource4()`, `jdbcTemplate4()`, `namedJdbcTemplate4()`, `tm4()` |
| 🔴 **BatchInfrastructureConfig** | **关键配置**：配置 JobRepository、JobLauncher、JobExplorer，**绑定 tm1 确保元数据事务独立于业务事务** | `jobRepository(dataSource1, tm1)`, `jobLauncher()`, `jobExplorer()` |
| **AnnotationDrivenFieldSetMapper** | 解析 @FileColumn 注解，反射映射字段 | `mapFieldSet(FieldSet fs)` |
| **HeaderFooterValidatingReader** | 包装 FlatFileItemReader，校验首尾行 | `read()`, `validateHeader()`, `validateFooter()` |
| **DataCleansingProcessor** | 数据清洗（trim、大小写转换、默认值填充） | `process(T item)` |
| **TypeConversionProcessor** | 类型转换（String → Integer/Date/BigDecimal） | `process(T item)`, `convert(String value, Class<?> targetType)` |
| **Db1BusinessService** | db1 业务逻辑，注入 jdbcTemplate1，使用 @Transactional(tm1) | `processDb1Data(List<T> items)` |
| **Db2BusinessService** | db2 业务逻辑，注入 jdbcTemplate2，使用 @Transactional(tm2) | `processDb2Data(List<T> items)` |
| **Db3BusinessService** | db3 业务逻辑，注入 jdbcTemplate3，使用 @Transactional(tm3) | `processDb3Data(List<T> items)` |
| **Db4BusinessService** | db4 业务逻辑，注入 jdbcTemplate4，使用 @Transactional(tm4) | `processDb4Data(List<T> items)` |
| **CsvInjectionSanitizer** | 检测并转义 `=`、`+`、`-`、`@` 开头的内容 | `sanitize(String value)` |
| **FilePathNormalizer** | 防止路径遍历攻击（禁止 `..` 和绝对路径） | `normalize(String path)`, `validatePath(String path)` |
| **HeaderValidator** | 首行格式校验（日期格式、文件标识） | `validate(String headerLine)` |
| **FooterValidator** | 尾行记录总数校验 | `validate(String footerLine, long actualRecordCount)` |

---

## 4. 配置文件详解

### 4.1 application.yml

```yaml
spring:
  datasource:
    # DB1: Spring Batch 元数据 + 业务数据（主数据源）
    db1:
      jdbc-url: jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB;encrypt=true;trustServerCertificate=true
      username: sa
      password: YourPassword123
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
      hikari:
        maximum-pool-size: 15
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        pool-name: HikariPool-DB1

    # DB2: 业务数据库 2
    db2:
      jdbc-url: jdbc:sqlserver://localhost:1433;databaseName=DB2_Business;encrypt=true;trustServerCertificate=true
      username: sa
      password: YourPassword123
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
      hikari:
        maximum-pool-size: 10
        minimum-idle: 3
        connection-timeout: 30000
        pool-name: HikariPool-DB2

    # DB3: 业务数据库 3
    db3:
      jdbc-url: jdbc:sqlserver://localhost:1433;databaseName=DB3_Business;encrypt=true;trustServerCertificate=true
      username: sa
      password: YourPassword123
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
      hikari:
        maximum-pool-size: 10
        minimum-idle: 3
        connection-timeout: 30000
        pool-name: HikariPool-DB3

    # DB4: 业务数据库 4
    db4:
      jdbc-url: jdbc:sqlserver://localhost:1433;databaseName=DB4_Business;encrypt=true;trustServerCertificate=true
      username: sa
      password: YourPassword123
      driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
      hikari:
        maximum-pool-size: 10
        minimum-idle: 3
        connection-timeout: 30000
        pool-name: HikariPool-DB4

  # Spring Batch 配置
  batch:
    jdbc:
      initialize-schema: always          # 自动初始化 Batch 元数据表
      table-prefix: BATCH_                # 元数据表前缀（默认）
    job:
      enabled: false                      # 禁止启动时自动运行 Job

# 日志配置
logging:
  level:
    root: INFO
    org.springframework.batch: DEBUG
    org.springframework.jdbc: DEBUG
    com.batchweaver: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# 批处理配置
batch:
  chunk-size: 100                         # 默认 chunk 大小
  file:
    base-path: /data/batch/files          # 文件基础路径
    allowed-extensions: txt,csv,dat       # 允许的文件扩展名
```

### 4.2 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.7</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>batch-weaver</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>BatchWeaver</name>
    <description>Spring Batch 5.x Multi-Datasource System</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Spring Boot Batch Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-batch</artifactId>
        </dependency>

        <!-- Spring Boot JDBC Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <!-- SQL Server JDBC Driver -->
        <dependency>
            <groupId>com.microsoft.sqlserver</groupId>
            <artifactId>mssql-jdbc</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- HikariCP (已由 spring-boot-starter-jdbc 包含) -->

        <!-- Lombok (可选，简化代码) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Validation API -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.batch</groupId>
            <artifactId>spring-batch-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 5. 关键实现点

### 5.1 事务隔离实现

#### a) BatchInfrastructureConfig - 元数据事务独立配置

**关键点**：JobRepository 必须绑定 tm1，确保元数据事务不受业务事务影响！

```java
@Configuration
@EnableBatchProcessing
public class BatchInfrastructureConfig {

    /**
     * 🔴 关键配置：JobRepository 绑定 tm1（db1 事务管理器）
     * 确保元数据事务独立于业务事务，失败时元数据也能提交
     */
    @Bean
    public JobRepository jobRepository(@Qualifier("dataSource1") DataSource dataSource1,
                                       @Qualifier("tm1") PlatformTransactionManager tm1) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource1);       // ✅ 使用 db1 数据源
        factory.setTransactionManager(tm1);       // 🔴 绑定 tm1，确保元数据事务独立
        factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factory.setTablePrefix("BATCH_");         // Spring Batch 元数据表前缀
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * JobLauncher 配置（使用上面的 JobRepository）
     */
    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    /**
     * JobExplorer 配置（用于查询批处理执行历史）
     */
    @Bean
    public JobExplorer jobExplorer(@Qualifier("dataSource1") DataSource dataSource1) throws Exception {
        JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
        factory.setDataSource(dataSource1);
        factory.setTablePrefix("BATCH_");
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
```

**配置说明**：
- ✅ `JobRepository` 使用 `dataSource1`（db1）+ `tm1`（db1 事务管理器）
- ✅ 所有元数据操作（BATCH_JOB_EXECUTION、BATCH_STEP_EXECUTION 等）由 tm1 管理
- ✅ 即使 Step 业务逻辑失败（tm2 回滚），元数据也会提交（tm1 独立提交）

---

#### b) StepBuilder 显式绑定业务事务管理器

```java
@Configuration
public class DemoJobConfig {

    @Bean
    public Step importFileStep(JobRepository jobRepository,
                               @Qualifier("tm2") PlatformTransactionManager tm2,
                               ItemReader<DemoUser> reader,
                               ItemProcessor<DemoUser, DemoUser> processor,
                               ItemWriter<DemoUser> writer) {
        return new StepBuilder("importFileStep", jobRepository)
            .transactionManager(tm2)  // ✅ 显式指定业务事务管理器 tm2
            .<DemoUser, DemoUser>chunk(100, tm2)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(10)
            .skip(Exception.class)
            .build();
    }

    @Bean
    public Job demoJob(JobRepository jobRepository, Step importFileStep) {
        return new JobBuilder("demoJob", jobRepository)
            .start(importFileStep)
            .build();
    }
}
```

**Service 层事务注解：**

```java
@Service
public class Db2BusinessService {

    private final NamedParameterJdbcTemplate namedJdbcTemplate2;

    public Db2BusinessService(@Qualifier("namedJdbcTemplate2")
                              NamedParameterJdbcTemplate namedJdbcTemplate2) {
        this.namedJdbcTemplate2 = namedJdbcTemplate2;
    }

    @Transactional(transactionManager = "tm2", propagation = Propagation.REQUIRED)
    public void processDb2Data(List<DemoUser> users) {
        String sql = "INSERT INTO DEMO_USER (id, name, email) VALUES (:id, :name, :email)";
        SqlParameterSource[] batchParams = users.stream()
            .map(user -> new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("name", user.getName())
                .addValue("email", user.getEmail()))
            .toArray(SqlParameterSource[]::new);
        namedJdbcTemplate2.batchUpdate(sql, batchParams);
    }
}
```

### 5.2 注解驱动字段映射

**@FileColumn 注解定义：**

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FileColumn {
    int index();                           // 列索引（从 0 开始）
    String name() default "";              // 列名称（用于日志）
    boolean trim() default true;           // 是否去除前后空格
    boolean toUpperCase() default false;   // 是否转大写
    boolean toLowerCase() default false;   // 是否转小写
    String defaultValue() default "";      // 默认值
    Class<? extends TypeConverter> converter() default TypeConverter.class;  // 自定义转换器
}
```

**实体类示例：**

```java
@Data
public class DemoUser {

    @FileColumn(index = 0, name = "userId")
    private Integer id;

    @FileColumn(index = 1, name = "userName", trim = true, toUpperCase = true)
    private String name;

    @FileColumn(index = 2, name = "email", trim = true, defaultValue = "unknown@example.com")
    private String email;

    @FileColumn(index = 3, name = "birthDate", converter = StringToDateConverter.class)
    private Date birthDate;
}
```

**AnnotationDrivenFieldSetMapper 实现：**

```java
public class AnnotationDrivenFieldSetMapper<T> implements FieldSetMapper<T> {

    private final Class<T> targetType;

    @Override
    public T mapFieldSet(FieldSet fieldSet) throws BindException {
        try {
            T instance = targetType.getDeclaredConstructor().newInstance();

            for (Field field : targetType.getDeclaredFields()) {
                if (field.isAnnotationPresent(FileColumn.class)) {
                    FileColumn annotation = field.getAnnotation(FileColumn.class);
                    String value = fieldSet.readString(annotation.index());

                    // 数据清洗
                    if (annotation.trim()) {
                        value = value != null ? value.trim() : null;
                    }
                    if (annotation.toUpperCase()) {
                        value = value != null ? value.toUpperCase() : null;
                    }
                    if (annotation.toLowerCase()) {
                        value = value != null ? value.toLowerCase() : null;
                    }
                    if ((value == null || value.isEmpty()) && !annotation.defaultValue().isEmpty()) {
                        value = annotation.defaultValue();
                    }

                    // 类型转换
                    Object convertedValue = convert(value, field.getType(), annotation.converter());

                    field.setAccessible(true);
                    field.set(instance, convertedValue);
                }
            }

            return instance;
        } catch (Exception e) {
            throw new BindException("Failed to map FieldSet", e);
        }
    }

    private Object convert(String value, Class<?> targetType,
                          Class<? extends TypeConverter> converterClass) {
        if (converterClass != TypeConverter.class) {
            // 使用自定义转换器
            TypeConverter converter = converterClass.getDeclaredConstructor().newInstance();
            return converter.convert(value);
        }

        // 内置转换器
        if (targetType == Integer.class) {
            return Integer.valueOf(value);
        } else if (targetType == Date.class) {
            return new SimpleDateFormat("yyyyMMdd").parse(value);
        } else if (targetType == BigDecimal.class) {
            return new BigDecimal(value);
        }

        return value;
    }
}
```

### 5.3 首尾行校验逻辑

**HeaderValidator 实现：**

```java
@Component
public class HeaderValidator {

    // 首行格式：H|20261231|DEMO_FILE
    public void validate(String headerLine) throws ValidationException {
        if (headerLine == null || !headerLine.startsWith("H|")) {
            throw new ValidationException("Invalid header format: must start with 'H|'");
        }

        String[] parts = headerLine.split("\\|");
        if (parts.length < 3) {
            throw new ValidationException("Invalid header format: missing fields");
        }

        // 验证日期格式
        String dateStr = parts[1];
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format in header: " + dateStr, e);
        }

        // 验证文件标识
        String fileIdentifier = parts[2];
        if (fileIdentifier == null || fileIdentifier.trim().isEmpty()) {
            throw new ValidationException("Invalid file identifier in header");
        }
    }
}
```

**FooterValidator 实现：**

```java
@Component
public class FooterValidator {

    // 尾行格式：T|1000
    public void validate(String footerLine, long actualRecordCount) throws ValidationException {
        if (footerLine == null || !footerLine.startsWith("T|")) {
            throw new ValidationException("Invalid footer format: must start with 'T|'");
        }

        String[] parts = footerLine.split("\\|");
        if (parts.length < 2) {
            throw new ValidationException("Invalid footer format: missing record count");
        }

        long declaredCount;
        try {
            declaredCount = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid record count in footer: " + parts[1], e);
        }

        if (declaredCount != actualRecordCount) {
            throw new ValidationException(
                String.format("Record count mismatch: declared=%d, actual=%d",
                             declaredCount, actualRecordCount)
            );
        }
    }
}
```

**HeaderFooterValidatingReader 实现：**

```java
public class HeaderFooterValidatingReader<T> implements ItemReader<T> {

    private final FlatFileItemReader<T> delegate;
    private final HeaderValidator headerValidator;
    private final FooterValidator footerValidator;
    private boolean headerValidated = false;
    private long recordCount = 0;
    private String footerLine = null;

    @Override
    public T read() throws Exception {
        T item = delegate.read();

        if (item == null) {
            // 读取结束，校验尾行
            if (footerLine != null) {
                footerValidator.validate(footerLine, recordCount);
            }
            return null;
        }

        if (!headerValidated) {
            // 首次读取时校验首行（假设首行已在 delegate 中跳过）
            headerValidated = true;
        }

        recordCount++;
        return item;
    }
}
```

### 5.4 安全防护实现

**CSV 注入防护：**

```java
@Component
public class CsvInjectionSanitizer {

    private static final Pattern DANGEROUS_PREFIX = Pattern.compile("^[=+\\-@]");

    public String sanitize(String value) {
        if (value == null) {
            return null;
        }

        if (DANGEROUS_PREFIX.matcher(value).find()) {
            return "'" + value;  // 在前面加单引号，转义危险字符
        }

        return value;
    }

    public List<String> sanitizeAll(List<String> values) {
        return values.stream()
            .map(this::sanitize)
            .collect(Collectors.toList());
    }
}
```

**文件路径安全校验：**

```java
@Component
public class FilePathNormalizer {

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\.");

    public String normalize(String filePath) throws SecurityException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        // 防止路径遍历攻击
        if (PATH_TRAVERSAL.matcher(filePath).find()) {
            throw new SecurityException("Path traversal detected: " + filePath);
        }

        // 禁止绝对路径（可选，根据需求调整）
        Path path = Paths.get(filePath);
        if (path.isAbsolute()) {
            throw new SecurityException("Absolute path not allowed: " + filePath);
        }

        // 规范化路径
        try {
            return path.normalize().toString();
        } catch (InvalidPathException e) {
            throw new SecurityException("Invalid file path: " + filePath, e);
        }
    }

    public void validateExtension(String filePath, Set<String> allowedExtensions) {
        String extension = getFileExtension(filePath);
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new SecurityException("File extension not allowed: " + extension);
        }
    }

    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        return lastDotIndex > 0 ? filePath.substring(lastDotIndex + 1) : "";
    }
}
```

---

## 6. 实施步骤清单（优先级排序）

| 优先级 | 步骤 | 输出 | 预计工作量 |
|--------|------|------|-----------|
| **P1** | 创建 Maven 项目结构 + pom.xml | 可编译的空项目 | 0.5h |
| **P2** | 配置 4 个数据源（DataSource1-4Config） | 数据源 Bean 可注入 | 1h |
| **P3** | 配置 Batch 基础设施（JobRepository/Launcher） | Batch 框架可用 | 0.5h |
| **P4** | 实现 FlatFile 注解 + 映射器 | @FileColumn 可解析 | 2h |
| **P5** | 实现首尾行校验器 | 文件导入可校验 | 1.5h |
| **P6** | 实现数据清洗 Processor | 数据自动 trim/转换 | 1h |
| **P7** | 实现 Service 层（多数据源 JdbcTemplate） | 业务数据可写入 | 1.5h |
| **P8** | 实现示例 Job（文件导入 → db2） | 端到端可运行 | 2h |
| **P9** | 实现安全防护（路径校验、CSV 注入） | 安全基线达标 | 1h |
| **P10** | 编写集成测试 | 质量保障 | 2h |

**总预估工作量**：13 小时（不含调试和优化）

---

## 7. 技术风险与缓解策略

| 风险类型 | 描述 | 影响级别 | 缓解策略 |
|---------|------|---------|---------|
| **性能瓶颈** | db1 同时承载元数据与业务可能成为热点 | 中 | 1. 增加 db1 连接池配额<br>2. 优化元数据表索引<br>3. 必要时拆分元数据到独立库 |
| **事务一致性** | 跨库写入存在分布式事务缺口 | 高 | 1. 明确"单 Step 单事务管理器"规则<br>2. 禁止跨库事务操作<br>3. 设计补偿逻辑 |
| **重跑幂等** | 失败重跑可能导致业务重复写入 | 高 | 1. 业务表设计幂等键（唯一索引）<br>2. 实现幂等性检查逻辑<br>3. 使用 MERGE 或 UPSERT 语句 |
| **版本兼容** | Boot 3.5.7 + Batch 5.x 配置差异 | 低 | 1. 严格遵循官方文档配置模式<br>2. 集成测试覆盖核心场景 |
| **大文件内存** | 大文件可能导致 OOM | 中 | 1. 使用流式读取（FlatFileItemReader）<br>2. 调整 chunk 大小<br>3. 增加 JVM 堆内存 |

---

## 8. 质量保障策略

### 8.1 单元测试
- **AnnotationDrivenFieldSetMapper**：测试注解解析、类型转换、数据清洗
- **HeaderValidator/FooterValidator**：测试各种异常输入
- **CsvInjectionSanitizer**：测试危险字符转义

### 8.2 集成测试
- **端到端 Job 测试**：
  - 正常流程：文件读取 → 数据写入 → 元数据状态 COMPLETED
  - 异常流程：文件格式错误 → Step FAILED → 业务回滚 → 元数据记录 FAILED
  - 重跑场景：失败后重启 → 从断点继续执行

### 8.3 🔴 事务隔离验证测试（核心测试）

**测试目标**：验证元数据事务独立性，确保业务失败时元数据仍能提交。

**测试用例**：模拟 Step 执行失败，验证元数据记录 FAILED 状态但业务数据已回滚。

```java
@SpringBootTest
@Transactional(transactionManager = "tm2", propagation = Propagation.NOT_SUPPORTED)  // 禁用测试默认事务
public class TransactionIsolationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job demoJob;

    @Autowired
    @Qualifier("jdbcTemplate1")
    private JdbcTemplate jdbcTemplate1;  // 元数据库

    @Autowired
    @Qualifier("jdbcTemplate2")
    private JdbcTemplate jdbcTemplate2;  // 业务库

    @Test
    public void testMetadataCommitWhenBusinessRollback() throws Exception {
        // 1. 准备：清空业务表和元数据表
        jdbcTemplate2.execute("DELETE FROM DEMO_USER");
        jdbcTemplate1.execute("DELETE FROM BATCH_JOB_EXECUTION");
        jdbcTemplate1.execute("DELETE FROM BATCH_STEP_EXECUTION");

        // 2. 准备：创建故意会失败的测试文件（如包含重复的主键）
        String testFilePath = prepareInvalidFile();  // 包含重复 ID 导致唯一约束冲突

        // 3. 执行：运行 Job（预期失败）
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", testFilePath)
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(demoJob, jobParameters);

        // 4. 验证：Job 执行状态为 FAILED
        assertEquals(BatchStatus.FAILED, jobExecution.getStatus());

        // 5. 验证：元数据表已记录 FAILED 状态（tm1 提交成功）
        Long jobExecutionCount = jdbcTemplate1.queryForObject(
            "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED'",
            Long.class
        );
        assertEquals(1L, jobExecutionCount);

        Long stepExecutionCount = jdbcTemplate1.queryForObject(
            "SELECT COUNT(*) FROM BATCH_STEP_EXECUTION WHERE STATUS = 'FAILED'",
            Long.class
        );
        assertEquals(1L, stepExecutionCount);

        // 6. ✅ 关键验证：业务表数据为空（tm2 回滚成功）
        Long businessDataCount = jdbcTemplate2.queryForObject(
            "SELECT COUNT(*) FROM DEMO_USER",
            Long.class
        );
        assertEquals(0L, businessDataCount, "业务事务应已回滚，业务表应为空！");

        // 7. 验证：重跑机制可用（元数据记录了失败状态，支持断点续传）
        JobExecution retryExecution = jobLauncher.run(demoJob, jobParameters);
        // 验证重跑时可以正确识别之前的失败
        assertNotNull(retryExecution);
    }

    private String prepareInvalidFile() {
        // 创建包含重复主键的测试文件
        String content = """
            H|20261231|TEST_FILE
            1|John Doe|john@example.com
            1|Duplicate ID|duplicate@example.com
            T|2
            """;
        // 写入临时文件并返回路径
        return writeToTempFile(content);
    }
}
```

**验证标准**：
- ✅ `BATCH_JOB_EXECUTION` 表有 FAILED 记录
- ✅ `BATCH_STEP_EXECUTION` 表有 FAILED 记录
- ✅ `DEMO_USER` 表为空（业务数据已回滚）
- ✅ 重跑 Job 时可识别之前的失败状态

**失败场景**：如果业务表有数据残留，说明事务隔离配置错误！

---

### 8.4 性能测试
- **基准测试**：
  - 10MB 文件（约 10 万条记录）：处理时间 < 2 分钟
  - 100MB 文件（约 100 万条记录）：处理时间 < 20 分钟
- **压力测试**：
  - 并发 5 个 Job：系统稳定运行，无 OOM

---

## 9. 后续扩展方向

### 9.1 功能增强
- **多线程处理**：使用 TaskExecutor 实现并行处理
- **分区 Step**：对大文件进行分区处理
- **动态路由**：根据数据内容动态选择目标数据源
- **监控告警**：集成 Micrometer + Prometheus，监控 Job 执行状态

### 9.2 架构优化
- **元数据库独立**：将 Batch 元数据迁移到独立数据库，减轻 db1 压力
- **分布式事务**：引入 Seata/Atomikos 支持跨库事务
- **消息驱动**：使用 Kafka/RabbitMQ 解耦文件上传与批处理

---

## 10. 交付物清单

- ✅ 完整项目目录结构
- ✅ 核心类职责清单
- ✅ 配置文件示例（application.yml、pom.xml）
- ✅ 关键实现点代码示例
- ✅ 实施步骤优先级排序
- ✅ 技术风险与缓解策略
- ✅ 质量保障策略

---

**规划批准状态**：待用户批准
**下一步操作**：用户批准后进入**阶段 4：代码实施**
