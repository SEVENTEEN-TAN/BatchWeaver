# BatchWeaver 启动脚本

本目录包含 BatchWeaver 的启动脚本，支持 Windows 和 Linux/Mac 平台。

## 脚本说明

### Windows (run-job.bat)

**使用方法**:
```bash
# 运行指定的 Job
scripts\run-job.bat jobName=importJob

# 带参数运行
scripts\run-job.bat jobName=importJob date=2024-01-01

# 断点续传
scripts\run-job.bat jobName=importJob resume=true
```

### Linux/Mac (run-job.sh)

**首次使用需要添加执行权限**:
```bash
chmod +x scripts/run-job.sh
```

**使用方法**:
```bash
# 运行指定的 Job
./scripts/run-job.sh jobName=importJob

# 带参数运行
./scripts/run-job.sh jobName=importJob date=2024-01-01

# 断点续传
./scripts/run-job.sh jobName=importJob resume=true
```

## 直接运行 JAR

如果不使用脚本，也可以直接运行 JAR 文件：

```bash
# 运行指定的 Job
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=importJob

# 不指定 jobName 时，默认运行 demoJob
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar
```

## 从 IDE 直接启动

在 IDE（如 IntelliJ IDEA）中可以直接运行 `BatchApplication.main()` 方法：

### 方式 1: 无参数启动（使用默认 Job）

直接运行 `BatchApplication.main()`，系统会：
1. 自动运行 `demoJob`（默认 Job）
2. 在控制台显示使用提示

**修改默认 Job**:
如果想修改默认 Job，编辑 `BatchApplication.java`：
```java
private static final String DEFAULT_JOB_NAME = "importJob";  // 修改为你想要的 Job
```

### 方式 2: 带参数启动

在 IDE 的运行配置中添加程序参数：

**IntelliJ IDEA**:
1. Run → Edit Configurations
2. 找到 BatchApplication
3. 在 "Program arguments" 中输入: `jobName=importJob`
4. 点击 Apply 和 OK

**Eclipse**:
1. Run → Run Configurations
2. 找到 BatchApplication
3. 在 "Arguments" 标签的 "Program arguments" 中输入: `jobName=importJob`
4. 点击 Apply 和 Run

## 可用的 Job

项目内置了以下示例 Job：

- `demoJob` - 简单的演示 Job（默认）
- `importJob` - CSV 数据导入（带文件校验）
- `multiStepJob` - 多步骤处理演示

## 参数说明

### 必需参数

- `jobName` - 要运行的 Job 名称（不指定时默认为 demoJob）

### 可选参数

- `resume=true` - 断点续传，从失败的 Step 继续执行
- 其他自定义参数 - 根据具体 Job 的需求传递

## 示例

### 示例 1: 运行演示 Job

```bash
# Windows
scripts\run-job.bat jobName=demoJob

# Linux/Mac
./scripts/run-job.sh jobName=demoJob

# 或者不指定 jobName（默认运行 demoJob）
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar
```

### 示例 2: 运行数据导入 Job

```bash
# Windows
scripts\run-job.bat jobName=importJob

# Linux/Mac
./scripts/run-job.sh jobName=importJob
```

### 示例 3: 断点续传

```bash
# Windows
scripts\run-job.bat jobName=multiStepJob resume=true

# Linux/Mac
./scripts/run-job.sh jobName=multiStepJob resume=true
```

### 示例 4: 带自定义参数

```bash
# Windows
scripts\run-job.bat jobName=importJob date=2024-01-01 batchSize=1000

# Linux/Mac
./scripts/run-job.sh jobName=importJob date=2024-01-01 batchSize=1000
```

## 注意事项

1. **编译打包**: 运行脚本前需要先编译打包项目
   ```bash
   mvn clean package -DskipTests
   ```

2. **工作目录**: 脚本会自动切换到项目根目录，确保相对路径正确

3. **日志输出**: 所有日志会输出到控制台，可以通过重定向保存到文件
   ```bash
   # Windows
   scripts\run-job.bat jobName=importJob > logs\job.log 2>&1
   
   # Linux/Mac
   ./scripts/run-job.sh jobName=importJob > logs/job.log 2>&1
   ```

4. **权限问题**: Linux/Mac 下首次使用需要添加执行权限
   ```bash
   chmod +x scripts/run-job.sh
   ```

## 故障排查

### 问题 1: 找不到 JAR 文件

**错误信息**: `Error: Unable to access jarfile target/batch-weaver-0.0.1-SNAPSHOT.jar`

**解决方法**: 先编译打包项目
```bash
mvn clean package -DskipTests
```

### 问题 2: Job 未找到

**错误信息**: `Job not found: xxx`

**解决方法**: 
1. 检查 Job 名称是否正确
2. 查看 `src/main/resources/jobs/` 目录下是否有对应的 XML 文件
3. 运行无参数命令查看可用的 Job 列表

### 问题 3: 权限不足（Linux/Mac）

**错误信息**: `Permission denied`

**解决方法**: 添加执行权限
```bash
chmod +x scripts/run-job.sh
```

## 更多信息

- 查看 [项目文档](../doc/INDEX.md) 了解更多使用方法
- 查看 [快速参考](../doc/QUICK_REFERENCE.md) 获取代码示例
