# 更新日志

## [未发布] - 2024-01-01

### 新增功能

#### 1. 跨平台启动脚本支持

- ✅ 新增 `scripts/run-job.bat` - Windows 启动脚本
- ✅ 新增 `scripts/run-job.sh` - Linux/Mac 启动脚本
- ✅ 脚本自动切换到项目根目录，确保相对路径正确
- ✅ 移除项目根目录的旧 `run-job.bat` 文件

**使用方法**:
```bash
# Windows
scripts\run-job.bat jobName=importJob

# Linux/Mac
./scripts/run-job.sh jobName=importJob
```

#### 2. 默认 Job 支持

- ✅ 支持无参数启动，自动运行默认 Job (`demoJob`)
- ✅ 在 `BatchApplication.main()` 中处理默认参数
- ✅ 启动时显示使用提示
- ✅ 支持只传递其他参数时自动添加默认 jobName

**使用方法**:
```bash
# 使用默认 Job
java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar

# 或从 IDE 直接运行 BatchApplication.main()
```

**控制台输出示例**:
```
========================================
No jobName provided. Using default job: demoJob
To run a specific job, use: java -jar app.jar jobName=<jobName>
Or set program arguments in your IDE: jobName=<jobName>
========================================
Starting job: demoJob
```

**默认 Job 配置**:
可以在 `BatchApplication.java` 中修改默认 Job：
```java
private static final String DEFAULT_JOB_NAME = "demoJob";  // 修改这里
```

#### 3. IDE 直接启动支持

- ✅ 支持从 IntelliJ IDEA、Eclipse 等 IDE 直接运行
- ✅ 无需配置参数即可启动（使用默认 Job）
- ✅ 可在运行配置中添加参数指定 Job

**IntelliJ IDEA 配置**:
1. Run → Edit Configurations
2. 找到 BatchApplication
3. 在 "Program arguments" 中输入: `jobName=importJob`

**Eclipse 配置**:
1. Run → Run Configurations
2. 找到 BatchApplication
3. 在 "Arguments" 标签的 "Program arguments" 中输入: `jobName=importJob`

### 代码改进

#### BatchApplication.java

新增默认参数处理逻辑:
```java
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
```

**优势**:
- ✅ 在应用入口统一处理默认参数
- ✅ 更加直观和易于维护
- ✅ 支持修改默认 Job 名称
- ✅ 支持只传递其他参数时自动添加 jobName

#### XmlJobParser.java

新增方法:
```java
/**
 * 获取所有可用的 Job 名称
 * @return Job 名称集合
 */
public Set<String> getAvailableJobNames() {
    return jobRegistry.keySet();
}
```

#### DynamicJobRunner.java

简化逻辑:
- 移除默认 Job 处理（已在 BatchApplication 中处理）
- 当没有 jobName 时显示可用的 Job 列表
- 保持原有的 Job 执行逻辑

### 文档更新

#### 新增文档

- ✅ `scripts/README.md` - 启动脚本详细说明
  - 脚本使用方法
  - 直接运行 JAR 的方法
  - 从 IDE 启动的方法
  - 可用的 Job 列表
  - 参数说明
  - 示例和故障排查

#### 更新文档

- ✅ `README.md` - 更新快速开始部分
  - 新增 3 种运行方式
  - 更新命令示例
  - 添加 scripts/README.md 链接

- ✅ `doc/QUICK_REFERENCE.md` - 更新命令速查
  - 更新脚本路径
  - 新增 Linux/Mac 命令
  - 新增默认 Job 运行方式
  - 新增 IDE 启动说明

### 目录结构变化

```
项目根目录/
├── scripts/                    # 新增：启动脚本目录
│   ├── run-job.bat            # Windows 启动脚本
│   ├── run-job.sh             # Linux/Mac 启动脚本
│   └── README.md              # 脚本使用说明
├── run-job.bat                # 已删除（移至 scripts/）
└── ...
```

### 使用场景

#### 场景 1: 开发调试

在 IDE 中直接运行 `BatchApplication.main()`，快速测试默认 Job。

#### 场景 2: 命令行运行

使用启动脚本运行指定的 Job，支持 Windows 和 Linux/Mac。

#### 场景 3: 生产部署

直接运行 JAR 文件，通过参数指定 Job 和配置。

### 兼容性

- ✅ 完全向后兼容
- ✅ 原有的命令行参数方式仍然有效
- ✅ 新增的默认 Job 功能不影响现有使用方式

### 注意事项

1. **Linux/Mac 用户**: 首次使用需要添加执行权限
   ```bash
   chmod +x scripts/run-job.sh
   ```

2. **脚本路径**: 启动脚本已移至 `scripts/` 目录，请更新相关文档和脚本

3. **默认 Job**: 如果项目中没有 `demoJob`，系统会跳过执行并提示

### 后续计划

- [ ] 支持通过配置文件指定默认 Job
- [ ] 支持 Job 参数的配置文件
- [ ] 添加 Job 列表查询命令
- [ ] 支持 Job 执行历史查询

---

## 历史版本

### [0.0.1-SNAPSHOT] - 初始版本

- 基础的 XML Job 配置
- Spring Batch 集成
- Druid 连接池
- H2 数据库
- 断点续传功能
