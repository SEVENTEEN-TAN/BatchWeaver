# BatchWeaver 脚本目录

本目录包含 BatchWeaver 的数据库初始化脚本和启动脚本。

## 📋 脚本列表

### 数据库脚本

- `init-sqlserver.sql` - SQL Server 完整初始化脚本（包含业务表和 Spring Batch 系统表）
- `create-batch-tables-sqlserver.sql` - 单独创建 Spring Batch 系统表
- `verify-tables.sql` - 验证所有表是否正确创建

### 启动脚本

- `run-job.bat` - Windows 启动脚本
- `run-job.sh` - Linux/Mac 启动脚本

---

## 🗄️ 数据库初始化

### 步骤 1: 安装 SQL Server 2022

确保已安装 SQL Server 2022，并记录以下信息：
- 服务器地址: `localhost:1433`
- 用户名: `sa`
- 密码: `YourStrong@Password`

### 步骤 2: 运行初始化脚本

**方式 1: 使用 sqlcmd（推荐）**

```bash
# 开发环境（包含测试数据）
# Windows
sqlcmd -S localhost -U sa -P YourStrong@Password -i scripts\init-all.sql

# Linux/Mac
sqlcmd -S localhost -U sa -P YourStrong@Password -i scripts/init-all.sql

# 生产环境（仅 DDL，不含测试数据）
# Windows
sqlcmd -S localhost -U sa -P YourStrong@Password -i scripts\init-ddl-only.sql

# Linux/Mac
sqlcmd -S localhost -U sa -P YourStrong@Password -i scripts/init-ddl-only.sql
```

**方式 2: 使用 SQL Server Management Studio (SSMS)**

1. 打开 SSMS 并连接到服务器
2. 打开 `scripts/init-sqlserver.sql` 文件
3. 点击 "Execute" 运行脚本

### 步骤 3: 验证表创建

```bash
# 运行验证脚本
sqlcmd -S localhost -U sa -P YourStrong@Password -i scripts\verify-tables.sql
```

**预期输出**:
```
✓ USER_DATA 表存在
✓ BATCH_JOB_INSTANCE 表存在
✓ BATCH_JOB_EXECUTION 表存在
✓ BATCH_JOB_EXECUTION_PARAMS 表存在
✓ BATCH_STEP_EXECUTION 表存在
✓ BATCH_JOB_EXECUTION_CONTEXT 表存在
✓ BATCH_STEP_EXECUTION_CONTEXT 表存在
```

### 脚本组织结构

脚本已按最佳实践重新组织：

- **DDL 脚本**（`ddl/` 目录）：创建数据库和表结构
- **DML 脚本**（`dml/` 目录）：插入测试数据
- **主脚本**：
  - `init-all.sql` - 完整初始化（DDL + DML）
  - `init-ddl-only.sql` - 仅 DDL（生产环境）

详细说明请参考：[SQL 脚本组织结构](SQL_SCRIPTS_README.md)

### 初始化脚本包含的内容

初始化脚本会创建：

1. **数据库**: `BatchWeaverDB`
2. **业务表**: `USER_DATA`（示例表）
3. **Spring Batch 系统表**:
   - `BATCH_JOB_INSTANCE` - Job 实例
   - `BATCH_JOB_EXECUTION` - Job 执行记录
   - `BATCH_JOB_EXECUTION_PARAMS` - Job 参数
   - `BATCH_STEP_EXECUTION` - Step 执行记录
   - `BATCH_JOB_EXECUTION_CONTEXT` - Job 上下文
   - `BATCH_STEP_EXECUTION_CONTEXT` - Step 上下文
4. **索引**: 性能优化索引
5. **测试数据**: 3 条示例数据

### 关于 Spring Batch 系统表

Spring Batch 需要这些系统表来：
- 记录 Job 和 Step 的执行状态
- 支持断点续传功能
- 存储执行参数和上下文
- 提供执行历史查询

**自动创建 vs 手动创建**:

- **开发环境**: 使用 `initialize-schema: always`，Spring Boot 会自动创建
- **生产环境**: 建议手动运行 `init-sqlserver.sql`，然后设置 `initialize-schema: never`

详细说明请参考: [Spring Batch 系统表说明](../doc/SPRING_BATCH_TABLES.md)

---

## 🚀 启动脚本说明

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
