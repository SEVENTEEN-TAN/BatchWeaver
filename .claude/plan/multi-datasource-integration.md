# BatchWeaver 多数据源集成实施计划

## 📌 项目目标
在 BatchWeaver 项目中集成 **MyBatis-Flex 2.x**，配置 4 个 SQL Server 数据源（DB1/DB2/DB3/DB4），实现基于 Spring Batch 的多数据源批处理编排，确保单 Step 单数据源事务一致性，同时保证 Spring Batch 元数据管理独立性。

## 🎯 技术方案
**方案 A：MyBatis-Flex 原生路由**
- 4 个 HikariDataSource + 1 个 FlexDataSource 路由包装器
- 服务层使用 `@UseDataSource("dbX")` 声明式路由
- 每个 Step 注入独立的 `PlatformTransactionManager`
- Spring Batch JobRepository 绑定 `db1DataSource` + `db1TransactionManager`

## 📊 数据源配置
- **DB1（Primary）**: `jdbc:sqlserver://124.223.172.114:1433;databaseName=BatchWeaverDB`
  - 角色：Spring Batch 元数据库 + 业务数据库
  - 连接池：`maximum-pool-size=5`
- **DB2/DB3/DB4（Business）**:
  - DB2: `databaseName=DB2_Business`
  - DB3: `databaseName=DB3_Business`
  - DB4: `databaseName=DB4_Business`
  - 连接池：各 `maximum-pool-size=5`
- 统一认证：`sa/YourStrong!Passw0rd`

## 🔧 实施步骤

### 阶段 1：依赖与配置（约 30 分钟）
1. **更新 pom.xml**
   - 添加 `mybatis-flex-spring-boot3-starter`（2.x 最新版本）
   - 验证与现有依赖无冲突

2. **扩展 application.yml**
   - 添加 4 数据源配置块（`spring.datasource.db1~db4`）
   - 配置 HikariCP 参数（连接池大小、超时、验证查询）
   - 添加 MyBatis-Flex 配置（`mapper-locations`, `type-aliases-package`）

### 阶段 2：基础设施层（约 60 分钟）
3. **创建 DataSourceConfig.java**
   - 定义 4 个 `@Bean DataSource`（`db1DataSource~db4DataSource`）
   - 创建 `FlexDataSource` 路由器，注册 4 个数据源
   - 定义 4 个 `@Bean PlatformTransactionManager`（`db1TxManager~db4TxManager`）
   - 配置 `FlexSqlSessionFactory`
   - 启用 `@MapperScan`（扫描 `com.example.batch.demo.infrastructure.mapper`）

4. **创建 BatchConfig.java**
   - 继承 `DefaultBatchConfiguration`
   - 重写 `getDataSource()` 返回 `db1DataSource`
   - 重写 `getTransactionManager()` 返回 `db1TxManager`
   - 确保 JobRepository 与业务事务隔离

### 阶段 3：数据访问层（约 90 分钟）
5. **创建 Entity 层**
   - `DemoUserEntity.java`（统一业务实体，使用 `@Table` 注解）

6. **创建 Mapper 层**（4 个 Mapper 接口）
   - `Db1UserMapper.java`（`@UseDataSource("db1")`）
   - `Db2UserMapper.java`（`@UseDataSource("db2")`）
   - `Db3UserMapper.java`（`@UseDataSource("db3")`）
   - `Db4UserMapper.java`（`@UseDataSource("db4")`）
   - 继承 `BaseMapper<DemoUserEntity>`，添加自定义查询方法

7. **创建 Service 层**（4 个业务服务）
   - `Db1BusinessService.java`（元数据同步逻辑）
   - `Db2BusinessService.java`（用户激活逻辑）
   - `Db3BusinessService.java`（发票对账逻辑 + **失败注入点**）
   - `Db4BusinessService.java`（分析数据推送逻辑）
   - 每个服务使用 `@Transactional(transactionManager="dbXTxManager")`

### 阶段 4：批处理作业层（约 60 分钟）
8. **创建 MultiDataSourceJobConfig.java**
   - 定义 `multiDataSourceJob`（4 步顺序执行）
   - **Step1 (db1Step)**：
     - 注入 `db1TxManager` + `Db1BusinessService`
     - 执行元数据刷新（如记录作业启动时间）
   - **Step2 (db2Step)**：
     - 注入 `db2TxManager` + `Db2BusinessService`
     - 批量激活 DB2 中的待处理用户
   - **Step3 (db3Step)** 【失败注入点】：
     - 注入 `db3TxManager` + `Db3BusinessService`
     - 对账发票，根据 `JobParameter("simulateFail")` 决定是否抛异常
     - 失败时抛出 `IllegalStateException("Simulated DB3 failure")`
   - **Step4 (db4Step)**：
     - 注入 `db4TxManager` + `Db4BusinessService`
     - 推送汇总数据到分析库

### 阶段 5：演示与验证（约 30 分钟）
9. **创建 Demo 脚本**
   - `scripts/demo-success.bat`：
     ```batch
     java -jar target/batch-weaver-0.0.1-SNAPSHOT.jar jobName=multiDataSourceJob simulateFail=false
     ```
   - `scripts/demo-fail-resume.bat`：
     ```batch
     # 第一次运行：Step3 失败
     java -jar ... jobName=multiDataSourceJob simulateFail=true id=100

     # 查看元数据状态
     # SELECT * FROM BATCH_STEP_EXECUTION WHERE STEP_NAME='db3Step'

     # 第二次运行：修正后成功
     java -jar ... jobName=multiDataSourceJob simulateFail=false id=100
     ```

10. **更新文档**
    - 在 `README.md` 中添加"多数据源配置"章节
    - 说明如何配置 4 个数据库连接
    - 演示失败恢复流程

### 阶段 6：测试与优化（约 30 分钟）
11. **功能测试**
    - 运行成功场景，验证 4 个 Step 全部完成
    - 运行失败场景，验证 Step3 回滚但元数据已记录
    - 重新运行失败作业，验证断点续传（从 Step3 继续）

12. **日志优化**
    - 在 Service 层添加日志：`log.info("[DB{}] Processing...", datasource)`
    - 验证 HikariCP 连接池状态（`com.zaxxer.hikari: DEBUG`）

## 📦 交付物清单

### 新增文件
- `src/main/java/com/example/batch/config/DataSourceConfig.java`
- `src/main/java/com/example/batch/config/BatchConfig.java`
- `src/main/java/com/example/batch/infrastructure/entity/DemoUserEntity.java`
- `src/main/java/com/example/batch/infrastructure/mapper/Db1UserMapper.java`
- `src/main/java/com/example/batch/infrastructure/mapper/Db2UserMapper.java`
- `src/main/java/com/example/batch/infrastructure/mapper/Db3UserMapper.java`
- `src/main/java/com/example/batch/infrastructure/mapper/Db4UserMapper.java`
- `src/main/java/com/example/batch/job/service/multids/Db1BusinessService.java`
- `src/main/java/com/example/batch/job/service/multids/Db2BusinessService.java`
- `src/main/java/com/example/batch/job/service/multids/Db3BusinessService.java`
- `src/main/java/com/example/batch/job/service/multids/Db4BusinessService.java`
- `src/main/java/com/example/batch/job/config/MultiDataSourceJobConfig.java`
- `scripts/demo-success.bat`
- `scripts/demo-fail-resume.bat`

### 修改文件
- `pom.xml`（添加 MyBatis-Flex 依赖）
- `src/main/resources/application.yml`（扩展 4 数据源配置）
- `README.md`（添加多数据源使用说明）

## ⚠️ 关键风险与注意事项

1. **事务边界管理**
   - 风险：Step 内使用错误的 TransactionManager 导致数据源路由失败
   - 缓解：严格按照 `db*Step` → `db*TxManager` → `db*Service(@UseDataSource)` 链路配置

2. **元数据隔离**
   - 风险：业务事务回滚影响 Spring Batch 元数据提交
   - 缓解：`BatchConfig` 强制 JobRepository 使用 `db1DataSource` + `db1TxManager`

3. **连接池泄漏**
   - 风险：事务未正确关闭导致连接池耗尽
   - 缓解：启用 HikariCP 监控日志（`com.zaxxer.hikari: DEBUG`），观察 `Active` 连接数

4. **ThreadLocal 路由问题**
   - 风险：异步 Step 或多线程 Chunk 中 `@UseDataSource` 失效
   - 缓解：当前 Step 为 Tasklet 同步执行，无风险；如需多线程需启用 `UseDataSourcePropagator`

5. **数据库连接信息**
   - 风险：DB2/DB3/DB4 数据库不存在导致启动失败
   - 缓解：提供 SQL 初始化脚本创建测试数据库

## 🚀 成功标准

- ✅ 应用启动时成功初始化 4 个 HikariCP 连接池
- ✅ `multiDataSourceJob` 成功场景下 4 个 Step 全部执行完成
- ✅ Step3 失败时，DB3 业务数据回滚，但 `BATCH_STEP_EXECUTION` 表记录失败状态
- ✅ 重新运行失败作业时，从 Step3 继续执行（跳过 Step1/2）
- ✅ 日志清晰显示每个 Step 操作的数据源名称
- ✅ HikariCP 连接池无泄漏（作业结束后 `Active` 连接归零）

## 📅 预估时间
总计约 **4-5 小时**（包含编码、测试、调试）

---

**审批确认**：请确认此计划是否符合预期，批准后将进入实施阶段。
