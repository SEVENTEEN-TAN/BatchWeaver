# Job 和 Step 的设计说明

## 架构设计

### 核心概念
```
1个 XML 文件 = 1个 Job
1个 Job = 多个 Step（按顺序执行）
1个 Step = 1个 Java 类 + 1个方法
```

### 示例结构

#### 单个 XML 文件：`multi-step-job.xml`
```xml
<job id="multiStepJob">
    <!-- Step 1: 数据预处理 -->
    <step id="preprocessData" 
          className="com.example.batch.steps.Step1Processor" 
          methodName="execute"/>
    
    <!-- Step 2: 数据转换 -->
    <step id="transformData" 
          className="com.example.batch.steps.Step2Processor" 
          methodName="execute"/>
    
    <!-- Step 3: 数据加载 -->
    <step id="loadData" 
          className="com.example.batch.steps.Step3Processor" 
          methodName="execute"/>
</job>
```

#### 对应的 3 个 Java 类
1. `Step1Processor.java` - 数据预处理
2. `Step2Processor.java` - 数据转换
3. `Step3Processor.java` - 数据加载

### 执行流程
```
用户执行: .\run-job.bat jobName=multiStepJob

系统行为:
1. XmlJobParser 解析 multi-step-job.xml
2. 创建包含 3 个 Step 的 Job
3. 依次执行:
   Step 1: Step1Processor.execute()
   Step 2: Step2Processor.execute()
   Step 3: Step3Processor.execute()
```

## 实际案例：数据导入 Job

```xml
<job id="importJob">
    <!-- Step 1: 文件校验 -->
    <step id="validate" 
          className="com.example.batch.steps.FileValidationTasklet" 
          methodName="execute">
        <property name="filePath" value="data/input.csv"/>
    </step>
    
    <!-- Step 2: CSV 导入 -->
    <step id="import" 
          className="com.example.batch.steps.CsvImportTasklet" 
          methodName="execute">
        <property name="filePath" value="data/input.csv"/>
    </step>
</job>
```

**对应的 Java 类：**
- `FileValidationTasklet.java` - 负责文件校验
- `CsvImportTasklet.java` - 负责数据导入

## Step 属性说明

### 必需属性
- `id`: Step 的唯一标识
- `className`: 处理类的完整类名
- `methodName`: 要执行的方法名（默认为 `execute`）

### 可选属性
- `<property>`: 向 Step 类注入参数

示例：
```xml
<step id="process" className="com.example.DataProcessor" methodName="process">
    <property name="inputFile" value="data.csv"/>
    <property name="batchSize" value="1000"/>
</step>
```

对应的 Java 类需要有 setter 方法：
```java
@Component
public class DataProcessor {
    private String inputFile;
    private int batchSize;
    
    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public void process() {
        // 使用 inputFile 和 batchSize
    }
}
```

## 创建新 Job 的步骤

### 1. 创建 Java 处理类
在 `src/main/java/com/example/batch/steps/` 下创建类：
```java
@Component
public class MyProcessor {
    private static final Logger log = LoggerFactory.getLogger(MyProcessor.class);
    
    public void execute() {
        log.info("执行业务逻辑");
        // 您的业务代码
    }
}
```

### 2. 创建 XML 定义
在 `src/main/resources/jobs/` 下创建 XML：
```xml
<job id="myJob">
    <step id="step1" className="com.example.batch.steps.MyProcessor" methodName="execute"/>
</job>
```

### 3. 运行
```bash
.\run-job.bat jobName=myJob
```

## 断点续传

如果某个 Step 失败：
```
Job: multiStepJob
  ✅ Step 1: preprocessData (成功)
  ✅ Step 2: transformData (成功)
  ❌ Step 3: loadData (失败)
```

使用 `resume=true` 从失败的 Step 继续：
```bash
.\run-job.bat jobName=multiStepJob resume=true
```

系统会跳过已成功的 Step 1 和 Step 2，直接从 Step 3 继续执行。

## 最佳实践

### 1. Step 粒度
- 每个 Step 应该是一个独立的业务单元
- 避免单个 Step 过于复杂

### 2. Step 顺序
- Step 按 XML 中定义的顺序执行
- 确保 Step 之间的依赖关系正确

### 3. 错误处理
- 在 Java 类中抛出异常会导致 Step 失败
- Job 会记录失败位置，支持断点续传

### 4. 命名规范
- Job ID: 使用驼峰命名，如 `importDataJob`
- Step ID: 描述性命名，如 `validateFile`、`transformData`
- 类名: 使用名词+动词，如 `FileValidator`、`DataProcessor`

## 测试验证

运行示例 Job：
```bash
# 多步骤 Job
.\run-job.bat jobName=multiStepJob

# 数据导入 Job
.\run-job.bat jobName=importJob
```

查看日志确认所有 Step 依次执行。
