# BatchWeaver 执行流程详解

> 可视化展示 BatchWeaver 的完整执行链路

## 目录

1. [启动流程](#启动流程)
2. [Job 加载流程](#job-加载流程)
3. [Job 执行流程](#job-执行流程)
4. [断点续传流程](#断点续传流程)
5. [异常处理流程](#异常处理流程)

---

## 启动流程

### 时序图

```
用户                run-job.bat         Spring Boot        XmlJobParser       DynamicJobRunner
 |                      |                    |                   |                    |
 |--[执行命令]--------->|                    |                   |                    |
 |  jobName=importJob   |                    |                   |                    |
 |                      |                    |                   |                    |
 |                      |--[启动应用]------->|                   |                    |
 |                      |                    |                   |                    |
 |                      |                    |--[@PostConstruct]>|                    |
 |                      |                    |                   |                    |
 |                      |                    |                   |--[扫描 XML]        |
 |                      |                    |                   |--[解析 XML]        |
 |                      |                    |                   |--[构建 Job]        |
 |                      |                    |                   |--[注册 Job]        |
 |                      |                    |                   |                    |
 |                      |                    |<--[加载完成]------|                    |
 |                      |                    |                   |                    |
 |                      |                    |--[CommandLineRunner]----------------->|
 |                      |                    |                   |                    |
 |                      |                    |                   |<--[获取 Job]-------|
 |                      |                    |                   |                    |
 |                      |                    |<--[启动 Job]-------------------------|
 |                      |                    |                   |                    |
 |<--[执行结果]---------|                    |                   |                    |
```

### 代码调用链

```
1. run-job.bat
   └─> java -jar batch-weaver.jar jobName=importJob

2. BatchApplication.main()
   └─> SpringApplication.run()
       ├─> 初始化 Spring 容器
       ├─> 扫描 @Component, @Service
       └─> 执行 @PostConstruct 方法

3. XmlJobParser.@PostConstruct.loadJobs()
   └─> PathMatchingResourcePatternResolver.getResources("classpath:jobs/*.xml")
   └─> for each XML file:
       ├─> xmlMapper.readValue() → JobXml
       ├─> buildJob(jobXml) → Job
       └─> jobRegistry.put(jobName, job)

4. DynamicJobRunner.run(args)
   └─> 解析命令行参数
   └─> xmlJobParser.getJob(jobName)
   └─> jobLauncher.run(job, params)
```

---

## Job 加载流程

### 流程图

```
┌─────────────────────────────────────────────────────────────┐
│                    应用启动                                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              XmlJobParser.@PostConstruct                     │
│                   loadJobs()                                 │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         扫描 classpath:jobs/*.xml                            │
│    PathMatchingResourcePatternResolver                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌──────────────────┐          ┌──────────────────┐
│  demo-job.xml    │          │ import-job.xml   │
└────────┬─────────┘          └────────┬─────────┘
         │                               │
         ▼                               ▼
┌─────────────────────────────────────────────────────────────┐
│              Jackson XML 解析                                │
│         xmlMapper.readValue() → JobXml                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  buildJob(jobXml)                            │
│                                                              │
│  1. 创建 JobBuilder                                          │
│  2. for each StepXml:                                       │
│     ├─> buildStep(stepXml)                                  │
│     │   ├─> 提取 properties                                 │
│     │   ├─> new ReflectionTasklet()                         │
│     │   └─> new StepBuilder().tasklet().build()            │
│     └─> jobBuilder.start(step).next(step)...               │
│  3. return job                                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              注册到 jobRegistry                              │
│      ConcurrentHashMap<String, Job>                         │
│                                                              │
│  ├─ "demoJob" → Job                                         │
│  ├─ "importJob" → Job                                       │
│  └─ "multiStepJob" → Job                                    │
└─────────────────────────────────────────────────────────────┘
```

### 关键代码

```java
// XmlJobParser.java
@PostConstruct
public void loadJobs() {
    Resource[] resources = resolver.getResources("classpath:jobs/*.xml");
    
    for (Resource resource : resources) {
        // 解析 XML
        JobXml jobXml = xmlMapper.readValue(resource.getInputStream(), JobXml.class);
        
        // 构建 Job
        Job job = buildJob(jobXml);
        
        // 注册
        jobRegistry.put(job.getName(), job);
        log.info("Registered job: {}", job.getName());
    }
}
```

---

## Job 执行流程

### 详细流程图

```
┌─────────────────────────────────────────────────────────────┐
│           DynamicJobRunner.run(args)                         │
│                                                              │
│  1. 解析参数: jobName=importJob date=2024-01-01             │
│  2. 获取 Job: xmlJobParser.getJob("importJob")              │
│  3. 构建 JobParameters                                       │
│  4. jobLauncher.run(job, params)                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                Spring Batch JobLauncher                      │
│                                                              │
│  1. 创建 JobExecution                                        │
│  2. 保存到 BATCH_JOB_EXECUTION 表                           │
│  3. 按顺序执行 Step                                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌──────────────────┐          ┌──────────────────┐
│   Step 1:        │          │   Step 2:        │
│   validate       │          │   import         │
└────────┬─────────┘          └────────┬─────────┘
         │                               │
         ▼                               ▼
┌─────────────────────────────────────────────────────────────┐
│            ReflectionTasklet.execute()                       │
│                                                              │
│  1. 加载类: Class.forName(className)                         │
│  2. 获取实例:                                                │
│     ├─> applicationContext.getBean(clazz)  [优先]           │
│     └─> clazz.newInstance()                [备用]           │
│  3. 注入属性: BeanWrapper.setPropertyValue()                │
│  4. 查找方法: ReflectionUtils.findMethod()                  │
│  5. 调用方法: method.invoke(target)                         │
│  6. return RepeatStatus.FINISHED                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              业务处理类执行                                   │
│                                                              │
│  Step 1: FileValidationTasklet.execute()                    │
│    └─> FileValidator.validate(filePath)                     │
│        ├─> 检查文件存在                                      │
│        ├─> 校验 Header (日期格式)                            │
│        ├─> 统计数据行数                                      │
│        └─> 校验 Footer (总数匹配)                            │
│                                                              │
│  Step 2: CsvImportTasklet.execute()                         │
│    └─> 读取 CSV 文件                                         │
│    └─> 解析每一行                                            │
│    └─> jdbcTemplate.update(INSERT INTO USER_DATA...)        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              更新执行状态                                     │
│                                                              │
│  BATCH_STEP_EXECUTION:                                      │
│    ├─ step_name: "validate"                                 │
│    ├─ status: "COMPLETED"                                   │
│    ├─ start_time: 2024-01-01 10:00:00                      │
│    └─ end_time: 2024-01-01 10:00:01                        │
│                                                              │
│  BATCH_JOB_EXECUTION:                                       │
│    ├─ job_name: "importJob"                                 │
│    ├─ status: "COMPLETED"                                   │
│    └─ exit_code: "COMPLETED"                                │
└─────────────────────────────────────────────────────────────┘
```

### 示例: importJob 执行

```
命令: run-job.bat jobName=importJob

执行过程:
┌─────────────────────────────────────────────────────────────┐
│ Step 1: validate                                            │
│ ├─ ReflectionTasklet                                        │
│ │  ├─ className: FileValidationTasklet                     │
│ │  ├─ methodName: execute                                  │
│ │  └─ properties: {filePath: "data/input.csv"}            │
│ │                                                           │
│ └─ FileValidationTasklet.execute()                         │
│    └─ FileValidator.validate("data/input.csv")             │
│       ├─ ✅ 文件存在                                        │
│       ├─ ✅ Header: 2024-01-01 (格式正确)                   │
│       ├─ ✅ 数据行数: 2                                     │
│       └─ ✅ Footer: Total: 2 (匹配)                         │
│                                                             │
│ 结果: COMPLETED                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ Step 2: import                                              │
│ ├─ ReflectionTasklet                                        │
│ │  ├─ className: CsvImportTasklet                          │
│ │  ├─ methodName: execute                                  │
│ │  └─ properties: {filePath: "data/input.csv"}            │
│ │                                                           │
│ └─ CsvImportTasklet.execute()                              │
│    ├─ 读取: data/input.csv                                 │
│    ├─ 跳过 Header: 2024-01-01                              │
│    ├─ 解析行 1: Alice,alice@example.com                    │
│    │  └─ INSERT INTO USER_DATA VALUES ('Alice', 'alice@...')│
│    ├─ 解析行 2: Bob,bob@example.com                        │
│    │  └─ INSERT INTO USER_DATA VALUES ('Bob', 'bob@...')  │
│    └─ 遇到 Footer: Total: 2 (停止)                         │
│                                                             │
│ 结果: COMPLETED                                             │
└─────────────────────────────────────────────────────────────┘

最终状态: Job COMPLETED ✅
```

---

## 断点续传流程

### 场景: Step 失败后续传

```
初次执行: run-job.bat jobName=multiStepJob

执行结果:
┌─────────────────────────────────────────────────────────────┐
│ Job: multiStepJob                                           │
│                                                             │
│ ├─ Step 1: preprocessData    ✅ COMPLETED                  │
│ ├─ Step 2: transformData     ✅ COMPLETED                  │
│ └─ Step 3: loadData          ❌ FAILED                     │
│                                                             │
│ 失败原因: 数据库连接超时                                     │
└─────────────────────────────────────────────────────────────┘

数据库状态:
BATCH_JOB_EXECUTION:
  job_instance_id: 1
  status: FAILED
  exit_code: FAILED

BATCH_STEP_EXECUTION:
  ├─ step_name: "preprocessData", status: COMPLETED
  ├─ step_name: "transformData",  status: COMPLETED
  └─ step_name: "loadData",       status: FAILED
```

### 续传执行

```
续传命令: run-job.bat jobName=multiStepJob resume=true

关键代码:
// DynamicJobRunner.java
if (!params.containsKey("resume") || !"true".equals(params.get("resume"))) {
    paramsBuilder.addLong("run.id", System.currentTimeMillis());
}
// resume=true 时不添加 run.id，JobParameters 保持一致

Spring Batch 行为:
┌─────────────────────────────────────────────────────────────┐
│ 1. 根据 JobParameters 查询 BATCH_JOB_EXECUTION              │
│    └─> 发现 job_instance_id=1 的执行记录 (FAILED)          │
│                                                             │
│ 2. 查询 BATCH_STEP_EXECUTION                                │
│    ├─ preprocessData: COMPLETED (跳过)                     │
│    ├─ transformData:  COMPLETED (跳过)                     │
│    └─ loadData:       FAILED    (重新执行)                 │
│                                                             │
│ 3. 从 loadData 开始执行                                     │
│    └─> Step3Processor.execute()                            │
│        └─> ✅ COMPLETED                                     │
│                                                             │
│ 4. 更新 Job 状态                                            │
│    └─> status: COMPLETED                                   │
└─────────────────────────────────────────────────────────────┘

最终结果: Job COMPLETED ✅
```

### 流程图

```
┌─────────────────────────────────────────────────────────────┐
│              用户执行续传命令                                 │
│        run-job.bat jobName=xxx resume=true                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         DynamicJobRunner 不添加 run.id                       │
│      JobParameters 与上次执行保持一致                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│            Spring Batch JobLauncher                          │
│                                                              │
│  1. 根据 JobParameters 查询 JobInstance                      │
│  2. 发现已存在的 JobExecution (FAILED)                       │
│  3. 创建新的 JobExecution (restart)                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│           查询 BATCH_STEP_EXECUTION                          │
│                                                              │
│  ├─ Step 1: COMPLETED → 跳过                                │
│  ├─ Step 2: COMPLETED → 跳过                                │
│  └─ Step 3: FAILED    → 重新执行                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              只执行失败的 Step                                │
│                                                              │
│  Step 3: loadData                                           │
│    └─> ReflectionTasklet.execute()                          │
│        └─> Step3Processor.execute()                         │
│            └─> ✅ COMPLETED                                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              更新 Job 状态为 COMPLETED                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 异常处理流程

### 异常传播链

```
业务代码异常
    │
    ▼
ReflectionTasklet.execute()
    │ method.invoke() 抛出异常
    ▼
Spring Batch Step
    │ 捕获异常，标记 Step 为 FAILED
    ▼
Spring Batch Job
    │ 停止后续 Step 执行
    │ 标记 Job 为 FAILED
    ▼
DynamicJobRunner
    │ 捕获异常，记录日志
    ▼
应用退出
```

### 示例: 文件校验失败

```
场景: input.csv 的 Footer 计数错误

文件内容:
2024-01-01
Alice,alice@example.com
Bob,bob@example.com
Total: 5  ← 错误！实际只有 2 行

执行流程:
┌─────────────────────────────────────────────────────────────┐
│ Step 1: validate                                            │
│                                                             │
│ FileValidator.validate("data/input.csv")                    │
│   ├─ ✅ 文件存在                                            │
│   ├─ ✅ Header: 2024-01-01                                  │
│   ├─ 📊 实际数据行数: 2                                     │
│   ├─ 📊 声明的行数: 5                                       │
│   └─ ❌ 抛出异常:                                           │
│       IllegalArgumentException:                             │
│       "Data count mismatch. Declared: 5, Actual: 2"        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ ReflectionTasklet 捕获异常                                   │
│   └─> 异常向上传播                                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ Spring Batch Step 处理                                       │
│   ├─ 标记 Step 状态: FAILED                                 │
│   ├─ 记录异常信息到 BATCH_STEP_EXECUTION                    │
│   └─ 停止 Job 执行                                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ Job 执行结果                                                 │
│                                                             │
│ ├─ Step 1: validate  ❌ FAILED                             │
│ └─ Step 2: import    ⏸️  未执行                            │
│                                                             │
│ Job 状态: FAILED                                            │
└─────────────────────────────────────────────────────────────┘

数据库记录:
BATCH_STEP_EXECUTION:
  step_name: "validate"
  status: FAILED
  exit_code: FAILED
  exit_message: "Data count mismatch. Declared: 5, Actual: 2"

BATCH_JOB_EXECUTION:
  status: FAILED
  exit_code: FAILED
```

### 修复后续传

```
1. 修复文件:
   Total: 5  →  Total: 2

2. 执行续传:
   run-job.bat jobName=importJob resume=true

3. 执行结果:
   ├─ Step 1: validate  ✅ COMPLETED (重新执行)
   └─ Step 2: import    ✅ COMPLETED (继续执行)
   
   Job 状态: COMPLETED ✅
```

---

## 总结

### 关键流程节点

1. **启动阶段**: `@PostConstruct` 加载所有 XML Job
2. **解析阶段**: Jackson XML → JobXml → Spring Batch Job
3. **执行阶段**: ReflectionTasklet 反射调用业务方法
4. **状态管理**: Spring Batch 元数据表记录执行状态
5. **断点续传**: 基于 JobParameters 和执行状态实现

### 核心组件交互

```
XmlJobParser ←→ jobRegistry ←→ DynamicJobRunner
     ↓                              ↓
  JobBuilder                   JobLauncher
     ↓                              ↓
ReflectionTasklet ←→ Spring Batch ←→ 元数据表
     ↓
业务处理类 (Tasklet)
```

### 扩展点

1. **自定义 Tasklet**: 实现业务逻辑
2. **自定义 Decider**: 实现条件分支
3. **自定义 Listener**: 监听 Job/Step 事件
4. **自定义 ItemReader/Writer**: 实现 Chunk 处理

---

**文档版本**: 1.0  
**最后更新**: 2024-01-01
