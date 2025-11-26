# BatchWeaver 快速参考

> 常用代码片段和配置模板

## 目录

1. [创建新 Job](#创建新-job)
2. [常用 Tasklet 模板](#常用-tasklet-模板)
3. [XML 配置模板](#xml-配置模板)
4. [数据库操作](#数据库操作)
5. [文件操作](#文件操作)
6. [常见问题](#常见问题)

---

## 创建新 Job

### 完整流程

```bash
# 1. 创建 Java 处理类
# 位置: src/main/java/com/example/batch/steps/MyTasklet.java

# 2. 创建 XML 配置
# 位置: src/main/resources/jobs/my-job.xml

# 3. 编译打包
mvn clean package -DskipTests

# 4. 运行
.\run-job.bat jobName=myJob
```

---

## 常用 Tasklet 模板

### 1. 基础 Tasklet

```java
package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MyTasklet {
    
    private static final Logger log = LoggerFactory.getLogger(MyTasklet.class);
    
    public void execute() {
        log.info("开始执行业务逻辑");
        
        // 你的业务代码
        
        log.info("执行完成");
    }
}
```

### 2. 带属性注入的 Tasklet

```java
package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConfigurableTasklet {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigurableTasklet.class);
    
    // 属性字段
    private String inputFile;
    private String outputFile;
    private int batchSize;
    
    // Setter 方法（必须）
    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }
    
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public void execute() {
        log.info("输入文件: {}", inputFile);
        log.info("输出文件: {}", outputFile);
        log.info("批次大小: {}", batchSize);
        
        // 使用配置的属性
    }
}
```

### 3. 数据库操作 Tasklet

```java
package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseTasklet {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseTasklet.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public void execute() {
        // 查询数据
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT * FROM USER_DATA WHERE NAME LIKE ?", 
            "A%"
        );
        
        log.info("查询到 {} 条记录", results.size());
        
        // 插入数据
        jdbcTemplate.update(
            "INSERT INTO USER_DATA (NAME, EMAIL) VALUES (?, ?)",
            "Charlie", "charlie@example.com"
        );
        
        // 更新数据
        int updated = jdbcTemplate.update(
            "UPDATE USER_DATA SET EMAIL = ? WHERE NAME = ?",
            "newemail@example.com", "Charlie"
        );
        
        log.info("更新了 {} 条记录", updated);
        
        // 删除数据
        jdbcTemplate.update("DELETE FROM USER_DATA WHERE NAME = ?", "Charlie");
    }
}
```

### 4. 文件读取 Tasklet

```java
package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Component
public class FileReaderTasklet {
    
    private static final Logger log = LoggerFactory.getLogger(FileReaderTasklet.class);
    
    private String filePath;
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public void execute() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                log.info("第 {} 行: {}", lineNumber, line);
                
                // 处理每一行
                processLine(line);
            }
            
            log.info("共处理 {} 行", lineNumber);
            
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + filePath, e);
        }
    }
    
    private void processLine(String line) {
        // 处理逻辑
    }
}
```

### 5. 文件写入 Tasklet

```java
package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Component
public class FileWriterTasklet {
    
    private static final Logger log = LoggerFactory.getLogger(FileWriterTasklet.class);
    
    private String outputFile;
    
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }
    
    public void execute() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // 写入 Header
            writer.write("Name,Email");
            writer.newLine();
            
            // 写入数据
            List<String> data = getData();
            for (String line : data) {
                writer.write(line);
                writer.newLine();
            }
            
            log.info("成功写入 {} 行到文件: {}", data.size(), outputFile);
            
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + outputFile, e);
        }
    }
    
    private List<String> getData() {
        // 获取要写入的数据
        return List.of(
            "Alice,alice@example.com",
            "Bob,bob@example.com"
        );
    }
}
```

### 6. HTTP 请求 Tasklet

```java
package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpTasklet {
    
    private static final Logger log = LoggerFactory.getLogger(HttpTasklet.class);
    
    private String apiUrl;
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    public void execute() {
        RestTemplate restTemplate = new RestTemplate();
        
        // GET 请求
        String response = restTemplate.getForObject(apiUrl, String.class);
        log.info("API 响应: {}", response);
        
        // POST 请求
        // MyRequest request = new MyRequest();
        // MyResponse response = restTemplate.postForObject(apiUrl, request, MyResponse.class);
    }
}
```

---

## XML 配置模板

### 1. 单步骤 Job

```xml
<job id="simpleJob">
    <step id="step1" 
          className="com.example.batch.steps.MyTasklet" 
          methodName="execute"/>
</job>
```

### 2. 多步骤 Job

```xml
<job id="multiStepJob">
    <step id="step1" 
          className="com.example.batch.steps.Step1Tasklet" 
          methodName="execute"/>
    
    <step id="step2" 
          className="com.example.batch.steps.Step2Tasklet" 
          methodName="execute"/>
    
    <step id="step3" 
          className="com.example.batch.steps.Step3Tasklet" 
          methodName="execute"/>
</job>
```

### 3. 带属性的 Job

```xml
<job id="configurableJob">
    <step id="process" 
          className="com.example.batch.steps.ConfigurableTasklet" 
          methodName="execute">
        <property name="inputFile" value="data/input.csv"/>
        <property name="outputFile" value="data/output.csv"/>
        <property name="batchSize" value="1000"/>
    </step>
</job>
```

### 4. ETL 流程 Job

```xml
<job id="etlJob">
    <!-- Extract: 数据提取 -->
    <step id="extract" 
          className="com.example.batch.steps.DataExtractor" 
          methodName="execute">
        <property name="sourceFile" value="data/source.csv"/>
    </step>
    
    <!-- Transform: 数据转换 -->
    <step id="transform" 
          className="com.example.batch.steps.DataTransformer" 
          methodName="execute">
        <property name="rules" value="config/transform-rules.json"/>
    </step>
    
    <!-- Load: 数据加载 -->
    <step id="load" 
          className="com.example.batch.steps.DataLoader" 
          methodName="execute">
        <property name="targetTable" value="USER_DATA"/>
    </step>
</job>
```

### 5. 带校验的导入 Job

```xml
<job id="importWithValidation">
    <!-- 校验文件 -->
    <step id="validate" 
          className="com.example.batch.steps.FileValidationTasklet" 
          methodName="execute">
        <property name="filePath" value="data/input.csv"/>
    </step>
    
    <!-- 导入数据 -->
    <step id="import" 
          className="com.example.batch.steps.CsvImportTasklet" 
          methodName="execute">
        <property name="filePath" value="data/input.csv"/>
    </step>
    
    <!-- 数据验证 -->
    <step id="verify" 
          className="com.example.batch.steps.DataVerificationTasklet" 
          methodName="execute">
        <property name="expectedCount" value="100"/>
    </step>
</job>
```

---

## 数据库操作

### 查询单个值

```java
Integer count = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM USER_DATA", 
    Integer.class
);
```

### 查询单行

```java
Map<String, Object> user = jdbcTemplate.queryForMap(
    "SELECT * FROM USER_DATA WHERE ID = ?", 
    1
);
String name = (String) user.get("NAME");
```

### 查询多行

```java
List<Map<String, Object>> users = jdbcTemplate.queryForList(
    "SELECT * FROM USER_DATA WHERE NAME LIKE ?", 
    "A%"
);

for (Map<String, Object> user : users) {
    log.info("用户: {}", user.get("NAME"));
}
```

### 使用 RowMapper

```java
List<User> users = jdbcTemplate.query(
    "SELECT * FROM USER_DATA",
    (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("ID"));
        user.setName(rs.getString("NAME"));
        user.setEmail(rs.getString("EMAIL"));
        return user;
    }
);
```

### 批量插入

```java
String sql = "INSERT INTO USER_DATA (NAME, EMAIL) VALUES (?, ?)";

List<Object[]> batchArgs = new ArrayList<>();
batchArgs.add(new Object[]{"Alice", "alice@example.com"});
batchArgs.add(new Object[]{"Bob", "bob@example.com"});
batchArgs.add(new Object[]{"Charlie", "charlie@example.com"});

int[] updateCounts = jdbcTemplate.batchUpdate(sql, batchArgs);
log.info("批量插入了 {} 条记录", updateCounts.length);
```

### 事务处理

```java
@Autowired
private PlatformTransactionManager transactionManager;

public void execute() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    
    transactionTemplate.execute(status -> {
        try {
            // 事务内的操作
            jdbcTemplate.update("INSERT INTO USER_DATA ...");
            jdbcTemplate.update("UPDATE USER_DATA ...");
            return null;
        } catch (Exception e) {
            status.setRollbackOnly();
            throw e;
        }
    });
}
```

---

## 文件操作

### CSV 解析

```java
String line = "Alice,alice@example.com,30";
String[] parts = line.split(",");

String name = parts[0].trim();
String email = parts[1].trim();
int age = Integer.parseInt(parts[2].trim());
```

### JSON 解析

```java
// 添加依赖: Jackson
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();

// 读取 JSON
MyObject obj = mapper.readValue(new File("data.json"), MyObject.class);

// 写入 JSON
mapper.writeValue(new File("output.json"), obj);
```

### 文件遍历

```java
import java.nio.file.*;

Path dir = Paths.get("data");
try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.csv")) {
    for (Path file : stream) {
        log.info("处理文件: {}", file.getFileName());
        processFile(file.toFile());
    }
}
```

### 文件复制

```java
import java.nio.file.*;

Path source = Paths.get("data/input.csv");
Path target = Paths.get("data/backup/input.csv");

Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
```

---

## 常见问题

### 1. 如何传递参数？

**命令行传参**:
```bash
.\run-job.bat jobName=myJob param1=value1 param2=value2
```

**在 Tasklet 中获取**:
```java
@Component
public class MyTasklet {
    
    // 方式 1: 通过 XML 属性注入
    private String param1;
    
    public void setParam1(String param1) {
        this.param1 = param1;
    }
    
    // 方式 2: 通过 ChunkContext 获取
    public void execute(StepContribution contribution, ChunkContext chunkContext) {
        Map<String, Object> jobParams = chunkContext.getStepContext()
            .getJobParameters();
        String param2 = (String) jobParams.get("param2");
    }
}
```

### 2. 如何实现断点续传？

```bash
# 首次执行
.\run-job.bat jobName=myJob

# 如果失败，使用 resume=true 续传
.\run-job.bat jobName=myJob resume=true
```

### 3. 如何查看执行历史？

```sql
-- 查看 Job 执行记录
SELECT * FROM BATCH_JOB_EXECUTION 
ORDER BY CREATE_TIME DESC;

-- 查看 Step 执行记录
SELECT * FROM BATCH_STEP_EXECUTION 
WHERE JOB_EXECUTION_ID = ?;
```

### 4. 如何处理大文件？

```java
@Component
public class LargeFileTasklet {
    
    private String filePath;
    private int batchSize = 1000;
    
    public void execute() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            List<String> batch = new ArrayList<>();
            String line;
            
            while ((line = reader.readLine()) != null) {
                batch.add(line);
                
                // 达到批次大小时处理
                if (batch.size() >= batchSize) {
                    processBatch(batch);
                    batch.clear();
                }
            }
            
            // 处理剩余数据
            if (!batch.isEmpty()) {
                processBatch(batch);
            }
        }
    }
    
    private void processBatch(List<String> batch) {
        // 批量处理
    }
}
```

### 5. 如何添加日志？

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MyTasklet {
    
    private static final Logger log = LoggerFactory.getLogger(MyTasklet.class);
    
    public void execute() {
        log.debug("调试信息");
        log.info("普通信息");
        log.warn("警告信息");
        log.error("错误信息", exception);
    }
}
```

**配置日志级别** (application.yml):
```yaml
logging:
  level:
    com.example.batch: DEBUG
    org.springframework.batch: INFO
```

### 6. 如何处理异常？

```java
@Component
public class SafeTasklet {
    
    public void execute() {
        try {
            // 可能抛出异常的代码
            riskyOperation();
            
        } catch (SpecificException e) {
            // 处理特定异常
            log.error("特定异常", e);
            throw new RuntimeException("处理失败", e);
            
        } catch (Exception e) {
            // 处理通用异常
            log.error("未知异常", e);
            throw e;
        }
    }
}
```

### 7. 如何测试 Tasklet？

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MyTaskletTest {
    
    @Autowired
    private MyTasklet tasklet;
    
    @Test
    void testExecute() {
        tasklet.setInputFile("test-data/input.csv");
        tasklet.execute();
        
        // 验证结果
    }
}
```

---

## 命令速查

```bash
# 编译打包
mvn clean package -DskipTests

# 运行 Job (Windows)
scripts\run-job.bat jobName=myJob

# 运行 Job (Linux/Mac)
./scripts/run-job.sh jobName=myJob

# 带参数运行
scripts\run-job.bat jobName=myJob param1=value1 param2=value2

# 断点续传
scripts\run-job.bat jobName=myJob resume=true

# 直接运行 JAR
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=myJob

# 使用默认 Job (demoJob)
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar

# 从 IDE 启动
# 直接运行 BatchApplication.main() 方法（默认运行 demoJob）
# 或在运行配置中添加参数: jobName=myJob
```

---

**文档版本**: 1.0  
**最后更新**: 2024-01-01
