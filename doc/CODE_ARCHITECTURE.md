# BatchWeaver 代码架构详解

> 从代码角度深入理解 BatchWeaver 的实现原理

## 目录

1. [项目概览](#项目概览)
2. [核心架构](#核心架构)
3. [关键组件详解](#关键组件详解)
4. [执行流程](#执行流程)
5. [数据模型](#数据模型)
6. [配置说明](#配置说明)
7. [扩展指南](#扩展指南)

---

## 项目概览

### 技术栈
- **JDK**: 21
- **Spring Boot**: 3.2.0
- **Spring Batch**: 5.x
- **数据库**: H2 (文件模式)
- **连接池**: Druid 1.2.20
- **XML 解析**: Jackson XML
- **构建工具**: Maven

### 项目结构
```
src/main/java/com/example/batch/
├── BatchApplication.java              # 主入口
├── config/
│   └── DruidConfig.java              # Druid 数据源配置
├── core/
│   ├── XmlJobParser.java             # XML 解析器（核心）
│   ├── DynamicJobRunner.java         # 命令行运行器
│   └── model/
│       ├── JobXml.java               # Job XML 模型
│       ├── StepXml.java              # Step XML 模型
│       └── PropertyXml.java          # Property XML 模型
├── components/
│   ├── ReflectionTasklet.java        # 反射执行器（核心）
│   └── FileValidator.java            # 文件校验器
├── steps/
│   ├── FileValidationTasklet.java    # 文件校验步骤
│   ├── CsvImportTasklet.java         # CSV 导入步骤
│   ├── DemoTasklet.java              # 演示步骤
│   ├── Step1Processor.java           # 多步骤示例 1
│   ├── Step2Processor.java           # 多步骤示例 2
│   └── Step3Processor.java           # 多步骤示例 3
└── verify/
    └── DataVerifier.java             # 数据验证工具

src/main/resources/
├── application.yml                    # 应用配置
├── schema-all.sql                     # 数据库表结构
└── jobs/
    ├── demo-job.xml                   # 演示 Job
    ├── import-job.xml                 # 导入 Job
    └── multi-step-job.xml             # 多步骤 Job
```

---

## 核心架构

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      命令行触发                               │
│              run-job.bat jobName=importJob                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  DynamicJobRunner                            │
│  - 解析命令行参数 (jobName, params)                           │
│  - 构建 JobParameters                                        │
│  - 调用 JobLauncher 启动 Job                                 │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   XmlJobParser                               │
│  - @PostConstruct 时扫描 jobs/*.xml                          │
│  - 使用 Jackson XML 解析为 JobXml 对象                       │
│  - 构建 Spring Batch Job 和 Step                            │
│  - 注册到 jobRegistry (ConcurrentHashMap)                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                 ReflectionTasklet                            │
│  - 接收 className, methodName, properties                   │
│  - 从 Spring 容器获取 Bean 或反射创建实例                     │
│  - 注入属性 (使用 BeanWrapper)                               │
│  - 反射调用指定方法                                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              业务处理类 (Tasklet)                             │
│  - FileValidationTasklet: 文件校验                           │
│  - CsvImportTasklet: CSV 导入                               │
│  - 自定义 Processor: 业务逻辑                                │
└─────────────────────────────────────────────────────────────┘
```

### 设计模式

1. **策略模式**: ReflectionTasklet 作为通用执行器，具体业务逻辑由不同的 Tasklet 实现
2. **工厂模式**: XmlJobParser 根据 XML 配置动态创建 Job 和 Step
3. **注册表模式**: jobRegistry 维护所有已加载的 Job
4. **依赖注入**: 使用 Spring 容器管理所有组件

---

## 关键组件详解

### 1. BatchApplication.java

**职责**: 应用程序入口，处理默认参数

```java
@SpringBootApplication
@EnableScheduling  // 启用定时任务支持
public class BatchApplication {
    
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
            System.out.println("No jobName provided. Using default job: " + DEFAULT_JOB_NAME);
            args = new String[]{"jobName=" + DEFAULT_JOB_NAME};
        } else if (!hasJobName && args.length > 0) {
            // 有其他参数但没有 jobName，添加默认 jobName
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "jobName=" + DEFAULT_JOB_NAME;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
        }

        SpringApplication.run(BatchApplication.class, args);
    }
}
```

**关键点**:
- `@EnableScheduling`: 启用定时任务支持（为未来功能预留）
- **默认参数处理**: 在应用入口统一处理，无参数时自动添加 `jobName=demoJob`
- **灵活配置**: 可通过修改 `DEFAULT_JOB_NAME` 常量更改默认 Job
- **智能合并**: 支持只传递其他参数时自动添加默认 jobName

**使用场景**:
1. **开发调试**: 从 IDE 直接运行，无需配置参数
2. **快速测试**: 命令行运行 `java -jar app.jar` 即可启动默认 Job
3. **灵活切换**: 修改常量即可更改默认 Job，无需修改多处代码

---

### 2. XmlJobParser.java

**职责**: XML 解析和 Job 构建的核心组件

#### 核心流程

```java
@PostConstruct
public void loadJobs() {
    // 1. 扫描 classpath:jobs/*.xml
    Resource[] resources = resolver.getResources("classpath:jobs/*.xml");
    
    // 2. 遍历每个 XML 文件
    for (Resource resource : resources) {
        // 3. 使用 Jackson XML 解析为 JobXml 对象
        JobXml jobXml = xmlMapper.readValue(resource.getInputStream(), JobXml.class);
        
        // 4. 构建 Spring Batch Job
        Job job = buildJob(jobXml);
        
        // 5. 注册到 jobRegistry
        jobRegistry.put(job.getName(), job);
    }
}
```

#### Job 构建逻辑

```java
private Job buildJob(JobXml jobXml) {
    JobBuilder jobBuilder = new JobBuilder(jobXml.getId(), jobRepository);
    
    SimpleJobBuilder simpleJobBuilder = null;
    
    // 按顺序构建 Step 链
    for (StepXml stepXml : jobXml.getSteps()) {
        Step step = buildStep(stepXml);
        if (simpleJobBuilder == null) {
            simpleJobBuilder = jobBuilder.start(step);  // 第一个 Step
        } else {
            simpleJobBuilder.next(step);                // 后续 Step
        }
    }
    
    return simpleJobBuilder.build();
}
```

#### Step 构建逻辑

```java
private Step buildStep(StepXml stepXml) {
    // 1. 提取属性
    Map<String, String> properties = new HashMap<>();
    stepXml.getProperties().forEach(p -> 
        properties.put(p.getName(), p.getValue())
    );
    
    // 2. 创建 ReflectionTasklet
    ReflectionTasklet tasklet = new ReflectionTasklet(
        stepXml.getClassName(), 
        stepXml.getMethodName(), 
        properties
    );
    tasklet.setApplicationContext(applicationContext);
    
    // 3. 构建 Step
    return new StepBuilder(stepXml.getId(), jobRepository)
            .tasklet(tasklet, transactionManager)
            .build();
}
```

**关键点**:
- 使用 `ConcurrentHashMap` 保证线程安全
- `@PostConstruct` 确保在应用启动时加载所有 Job
- 支持顺序执行的 Step 链

---

### 3. ReflectionTasklet.java

**职责**: 通过反射动态执行业务类的方法

#### 执行流程

```java
@Override
public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    // 1. 加载类
    Class<?> clazz = Class.forName(className);
    
    // 2. 获取实例（优先从 Spring 容器）
    Object target;
    try {
        target = applicationContext.getBean(clazz);  // 从容器获取
    } catch (Exception e) {
        target = clazz.getDeclaredConstructor().newInstance();  // 反射创建
    }
    
    // 3. 注入属性
    if (properties != null && !properties.isEmpty()) {
        BeanWrapper wrapper = new BeanWrapperImpl(target);
        properties.forEach(wrapper::setPropertyValue);
    }
    
    // 4. 查找方法
    Method method = ReflectionUtils.findMethod(clazz, methodName);
    
    // 5. 调用方法
    method.invoke(target);
    
    return RepeatStatus.FINISHED;
}
```

**关键点**:
- 优先使用 Spring 容器中的 Bean（支持依赖注入）
- 使用 `BeanWrapper` 动态设置属性（支持类型转换）
- 支持无参方法和带 `StepContribution`, `ChunkContext` 参数的方法
- 异常会导致 Step 失败，触发断点续传机制

---

### 4. DynamicJobRunner.java

**职责**: 命令行参数解析和 Job 启动

#### 参数解析

```java
@Override
public void run(String... args) {
    String jobName = null;
    Properties params = new Properties();
    
    // 解析参数: jobName=xxx key1=value1 key2=value2
    for (String arg : args) {
        if (arg.startsWith("jobName=")) {
            jobName = arg.split("=")[1];
        } else if (arg.contains("=")) {
            String[] parts = arg.split("=");
            params.put(parts[0], parts[1]);
        }
    }
    
    if (jobName == null) {
        log.info("No jobName provided. Skipping.");
        return;
    }
    
    // 获取 Job
    Job job = xmlJobParser.getJob(jobName);
    
    // 构建 JobParameters
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();
    for (String key : params.stringPropertyNames()) {
        paramsBuilder.addString(key, params.getProperty(key));
    }
    
    // 断点续传逻辑
    if (!params.containsKey("resume") || !"true".equals(params.get("resume"))) {
        paramsBuilder.addLong("run.id", System.currentTimeMillis());
    }
    
    // 启动 Job
    jobLauncher.run(job, paramsBuilder.toJobParameters());
}
```

**关键点**:
- 如果没有 `jobName` 参数，跳过执行（避免影响其他 CommandLineRunner）
- `resume=true` 时不添加 `run.id`，实现断点续传
- 所有其他参数都作为 JobParameters 传递

---

### 5. FileValidator.java

**职责**: 文件格式校验

#### 校验规则

```java
public void validate(String filePath) {
    // 1. 检查文件存在性
    File file = new File(filePath);
    if (!file.exists()) {
        throw new IllegalArgumentException("File not found");
    }
    
    // 2. 读取第一行（Header）
    String firstLine = reader.readLine();
    
    // 3. 校验日期格式 (YYYY-MM-DD)
    LocalDate.parse(firstLine.trim(), DateTimeFormatter.ISO_DATE);
    
    // 4. 统计数据行数
    int dataCount = 0;
    String lastLine = firstLine;
    while ((currentLine = reader.readLine()) != null) {
        lastLine = currentLine;
        dataCount++;
    }
    dataCount--;  // 减去 footer 行
    
    // 5. 校验 Footer (Total: N)
    int declaredCount = parseFooterCount(lastLine);
    if (declaredCount != dataCount) {
        throw new IllegalArgumentException("Count mismatch");
    }
}
```

**文件格式示例**:
```
2024-01-01
Alice,alice@example.com
Bob,bob@example.com
Total: 2
```

---

### 6. CsvImportTasklet.java

**职责**: CSV 数据导入

```java
public void execute() {
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
        // 跳过 Header (日期行)
        reader.readLine();
        
        String line;
        while ((line = reader.readLine()) != null) {
            // 遇到 Footer 停止
            if (line.startsWith("Total:")) {
                break;
            }
            
            // 解析 CSV
            String[] parts = line.split(",");
            String name = parts[0].trim();
            String email = parts[1].trim();
            
            // 插入数据库
            jdbcTemplate.update(
                "INSERT INTO USER_DATA (NAME, EMAIL) VALUES (?, ?)", 
                name, email
            );
        }
    }
}
```

**关键点**:
- 使用 `JdbcTemplate` 进行数据库操作
- 自动跳过 Header 和 Footer
- 异常会导致事务回滚

---

## 执行流程

### 完整执行链路

```
1. 用户执行命令
   └─> run-job.bat jobName=importJob date=2024-01-01

2. Spring Boot 启动
   └─> BatchApplication.main()
   
3. 初始化阶段
   └─> XmlJobParser.@PostConstruct
       └─> 扫描 jobs/*.xml
       └─> 解析为 JobXml 对象
       └─> 构建 Spring Batch Job
       └─> 注册到 jobRegistry

4. 命令行执行
   └─> DynamicJobRunner.run(args)
       └─> 解析参数: jobName=importJob, date=2024-01-01
       └─> 从 jobRegistry 获取 Job
       └─> 构建 JobParameters
       └─> jobLauncher.run(job, params)

5. Job 执行
   └─> Spring Batch 按顺序执行 Step
       └─> Step 1: validate
           └─> ReflectionTasklet.execute()
               └─> 加载 FileValidationTasklet
               └─> 注入 filePath="data/input.csv"
               └─> 调用 execute() 方法
                   └─> FileValidator.validate()
       
       └─> Step 2: import
           └─> ReflectionTasklet.execute()
               └─> 加载 CsvImportTasklet
               └─> 注入 filePath="data/input.csv"
               └─> 调用 execute() 方法
                   └─> 读取 CSV
                   └─> 插入数据库

6. 结果记录
   └─> Spring Batch 将执行状态写入元数据表
       └─> BATCH_JOB_INSTANCE
       └─> BATCH_JOB_EXECUTION
       └─> BATCH_STEP_EXECUTION
```

### 断点续传机制

```
场景: multiStepJob 的 Step 3 失败

执行状态:
├─ Step 1: preprocessData  ✅ COMPLETED
├─ Step 2: transformData   ✅ COMPLETED
└─ Step 3: loadData        ❌ FAILED

重新执行:
run-job.bat jobName=multiStepJob resume=true

Spring Batch 行为:
1. 查询 BATCH_JOB_EXECUTION 表
2. 发现上次执行失败
3. 查询 BATCH_STEP_EXECUTION 表
4. 跳过 COMPLETED 的 Step 1 和 Step 2
5. 从 FAILED 的 Step 3 继续执行
```

**关键代码**:
```java
// resume=true 时不添加 run.id，使用相同的 JobParameters
if (!params.containsKey("resume") || !"true".equals(params.get("resume"))) {
    paramsBuilder.addLong("run.id", System.currentTimeMillis());
}
```

---

## 数据模型

### XML 模型类

#### JobXml.java
```java
@JacksonXmlRootElement(localName = "job")
public class JobXml {
    @JacksonXmlProperty(isAttribute = true)
    private String id;  // Job ID
    
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "step")
    private List<StepXml> steps;  // Step 列表
}
```

#### StepXml.java
```java
public class StepXml {
    @JacksonXmlProperty(isAttribute = true)
    private String id;  // Step ID
    
    @JacksonXmlProperty(isAttribute = true)
    private String className;  // 处理类全限定名
    
    @JacksonXmlProperty(isAttribute = true)
    private String methodName;  // 方法名（默认 execute）
    
    @JacksonXmlProperty(localName = "property")
    private List<PropertyXml> properties;  // 属性列表
}
```

#### PropertyXml.java
```java
public class PropertyXml {
    @JacksonXmlProperty(isAttribute = true)
    private String name;  // 属性名
    
    @JacksonXmlProperty(isAttribute = true)
    private String value;  // 属性值
}
```

### 数据库表

#### USER_DATA (业务表)
```sql
CREATE TABLE USER_DATA (
    ID BIGINT IDENTITY NOT NULL PRIMARY KEY,
    NAME VARCHAR(100),
    EMAIL VARCHAR(100)
);
```

#### Spring Batch 元数据表
- `BATCH_JOB_INSTANCE`: Job 实例
- `BATCH_JOB_EXECUTION`: Job 执行记录
- `BATCH_JOB_EXECUTION_PARAMS`: Job 参数
- `BATCH_STEP_EXECUTION`: Step 执行记录
- `BATCH_JOB_EXECUTION_CONTEXT`: Job 执行上下文
- `BATCH_STEP_EXECUTION_CONTEXT`: Step 执行上下文

---

## 配置说明

### application.yml

```yaml
spring:
  datasource:
    # H2 数据源配置
    url: jdbc:h2:file:./data/batchdb
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
    
    # Druid 连接池配置
    druid:
      initial-size: 5           # 初始连接数
      min-idle: 5               # 最小空闲连接
      max-active: 20            # 最大活跃连接
      max-wait: 60000           # 获取连接最大等待时间
      validation-query: "SELECT 1"
      test-while-idle: true
      
  batch:
    jdbc:
      initialize-schema: always  # 自动初始化 Batch 元数据表
    job:
      enabled: false            # 禁止自动执行 Job

logging:
  level:
    com.example.batch: DEBUG    # 业务日志级别
    org.springframework.batch: INFO
```

### DruidConfig.java

```java
@Configuration
public class DruidConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DataSource dataSource() {
        return new DruidDataSource();
    }
}
```

**关键点**:
- `@ConfigurationProperties` 自动绑定配置
- Druid 提供连接池监控和统计功能

---

## 扩展指南

### 1. 添加新的 Job

#### Step 1: 创建业务处理类

```java
package com.example.batch.steps;

import org.springframework.stereotype.Component;

@Component
public class MyCustomTasklet {
    
    private String inputFile;
    private int batchSize;
    
    // Setter 方法（用于属性注入）
    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    // 业务逻辑
    public void execute() {
        // 实现你的业务逻辑
        System.out.println("Processing " + inputFile + " with batch size " + batchSize);
    }
}
```

#### Step 2: 创建 XML 配置

```xml
<!-- src/main/resources/jobs/my-custom-job.xml -->
<job id="myCustomJob">
    <step id="step1" 
          className="com.example.batch.steps.MyCustomTasklet" 
          methodName="execute">
        <property name="inputFile" value="data/input.txt"/>
        <property name="batchSize" value="1000"/>
    </step>
</job>
```

#### Step 3: 运行

```bash
mvn clean package -DskipTests
.\run-job.bat jobName=myCustomJob
```

### 2. 添加条件分支

虽然当前实现只支持顺序执行，但可以通过以下方式扩展：

#### 修改 XmlJobParser.buildJob()

```java
// 支持条件分支的 XML 格式
<job id="conditionalJob">
    <step id="step1" className="..." methodName="..."/>
    <decision id="decision1" decider="com.example.MyDecider"/>
    <step id="step2a" on="SUCCESS" className="..." methodName="..."/>
    <step id="step2b" on="FAILURE" className="..." methodName="..."/>
</job>
```

### 3. 添加并行执行

```java
// 在 XmlJobParser 中添加并行支持
private Job buildParallelJob(JobXml jobXml) {
    Flow flow1 = new FlowBuilder<Flow>("flow1")
        .start(step1)
        .build();
        
    Flow flow2 = new FlowBuilder<Flow>("flow2")
        .start(step2)
        .build();
        
    return new JobBuilder(jobXml.getId(), jobRepository)
        .start(flow1)
        .split(new SimpleAsyncTaskExecutor())
        .add(flow2)
        .end()
        .build();
}
```

### 4. 添加监听器

```java
@Component
public class JobCompletionListener implements JobExecutionListener {
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            // 发送通知、清理资源等
        }
    }
}

// 在 XmlJobParser 中注册
jobBuilder.listener(jobCompletionListener);
```

### 5. 添加 Chunk 处理

当前实现使用 Tasklet 模式，可以扩展支持 Chunk 模式：

```java
// 添加 Reader, Processor, Writer 支持
<step id="chunkStep" type="chunk">
    <reader className="com.example.MyItemReader"/>
    <processor className="com.example.MyItemProcessor"/>
    <writer className="com.example.MyItemWriter"/>
    <chunk-size>100</chunk-size>
</step>
```

---

## 总结

### 核心优势

1. **动态配置**: 通过 XML 配置 Job，无需重新编译
2. **反射机制**: ReflectionTasklet 实现了通用的执行框架
3. **Spring 集成**: 充分利用 Spring 的依赖注入和事务管理
4. **断点续传**: 基于 Spring Batch 的元数据表实现
5. **扩展性强**: 易于添加新的 Step 和 Job

### 技术亮点

1. **Jackson XML**: 优雅的 XML 解析方案
2. **BeanWrapper**: 动态属性注入
3. **ConcurrentHashMap**: 线程安全的 Job 注册表
4. **Druid**: 高性能连接池
5. **H2 文件模式**: 无需外部数据库

### 适用场景

- 数据导入/导出
- 批量数据处理
- 定时任务调度
- ETL 流程
- 文件处理流水线

---

**文档版本**: 1.0  
**最后更新**: 2024-01-01  
**维护者**: BatchWeaver Team
