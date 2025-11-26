# SQL 脚本组织结构

> 遵循最佳实践：DDL/DML 分离，系统表/业务表分离

---

## 📁 目录结构

```
scripts/
├── ddl/                          # DDL（数据定义语言）脚本
│   ├── 01-create-database.sql    # 创建数据库
│   ├── 02-create-business-tables.sql  # 创建业务表
│   └── 03-create-batch-tables.sql     # 创建 Spring Batch 系统表
├── dml/                          # DML（数据操作语言）脚本
│   └── 01-insert-test-data.sql   # 插入测试数据
├── init-all.sql                  # 完整初始化（DDL + DML）
├── init-ddl-only.sql             # 仅 DDL 初始化（生产环境）
├── init-sqlserver.sql            # 旧版单文件脚本（已废弃）
└── verify-tables.sql             # 验证脚本
```

---

## 🎯 设计原则

### 1. DDL 和 DML 分离

**DDL（Data Definition Language）**:
- 创建、修改、删除数据库对象（表、索引、约束等）
- 位于 `ddl/` 目录
- 适用于所有环境（开发、测试、生产）

**DML（Data Manipulation Language）**:
- 插入、更新、删除数据
- 位于 `dml/` 目录
- 通常只用于开发和测试环境

### 2. 系统表和业务表分离

**业务表**:
- `02-create-business-tables.sql` - 应用特定的业务表
- 例如：`USER_DATA`

**系统表**:
- `03-create-batch-tables.sql` - Spring Batch 框架表
- 例如：`BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION` 等

### 3. 按序号执行

脚本文件名以数字开头，确保按正确顺序执行：
1. 先创建数据库
2. 再创建业务表
3. 然后创建系统表
4. 最后插入数据

---

## 🚀 使用方法

### 开发环境（完整初始化）

包含 DDL + DML（测试数据）：

```bash
# Windows
sqlcmd -S localhost -U sa -P YourPassword -i scripts\init-all.sql

# Linux/Mac
sqlcmd -S localhost -U sa -P YourPassword -i scripts/init-all.sql
```

### 生产环境（仅 DDL）

只创建表结构，不插入测试数据：

```bash
# Windows
sqlcmd -S localhost -U sa -P YourPassword -i scripts\init-ddl-only.sql

# Linux/Mac
sqlcmd -S localhost -U sa -P YourPassword -i scripts/init-ddl-only.sql
```

### 单独执行某个脚本

```bash
# 只创建数据库
sqlcmd -S localhost -U sa -P YourPassword -i scripts\ddl\01-create-database.sql

# 只创建业务表
sqlcmd -S localhost -U sa -P YourPassword -i scripts\ddl\02-create-business-tables.sql

# 只创建 Spring Batch 系统表
sqlcmd -S localhost -U sa -P YourPassword -i scripts\ddl\03-create-batch-tables.sql

# 只插入测试数据
sqlcmd -S localhost -U sa -P YourPassword -i scripts\dml\01-insert-test-data.sql
```

---

## 📊 脚本详细说明

### DDL 脚本

#### 01-create-database.sql
- 创建 `BatchWeaverDB` 数据库
- 检查数据库是否已存在
- 切换到新创建的数据库

#### 02-create-business-tables.sql
- 创建业务表：`USER_DATA`
- 创建相关索引
- 可根据项目需求添加更多业务表

#### 03-create-batch-tables.sql
- 创建 Spring Batch 元数据表（6 张表）
- 创建外键约束
- 创建性能优化索引

**包含的表**:
- `BATCH_JOB_INSTANCE` - Job 实例
- `BATCH_JOB_EXECUTION` - Job 执行记录
- `BATCH_JOB_EXECUTION_PARAMS` - Job 参数
- `BATCH_STEP_EXECUTION` - Step 执行记录
- `BATCH_JOB_EXECUTION_CONTEXT` - Job 上下文
- `BATCH_STEP_EXECUTION_CONTEXT` - Step 上下文

### DML 脚本

#### 01-insert-test-data.sql
- 向 `USER_DATA` 表插入 3 条测试数据
- 验证数据插入成功
- **注意**: 仅用于开发和测试环境

---

## 🔄 版本控制最佳实践

### 添加新的业务表

1. 在 `ddl/` 目录创建新脚本：
   ```
   ddl/04-create-order-tables.sql
   ```

2. 更新 `init-all.sql` 和 `init-ddl-only.sql`，添加新脚本的引用

3. 如果需要测试数据，在 `dml/` 目录创建对应脚本：
   ```
   dml/02-insert-order-data.sql
   ```

### 修改现有表结构

1. 创建迁移脚本：
   ```
   ddl/05-alter-user-data-add-column.sql
   ```

2. 使用 `ALTER TABLE` 而不是 `DROP/CREATE`

3. 在脚本中添加版本注释：
   ```sql
   -- Version: 1.1.0
   -- Date: 2024-01-01
   -- Description: Add phone column to USER_DATA
   ```

---

## ✅ 验证

执行完初始化脚本后，运行验证脚本：

```bash
sqlcmd -S localhost -U sa -P YourPassword -i scripts\verify-tables.sql
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

---

## 🎯 环境对比

| 环境 | 使用脚本 | 包含内容 | 说明 |
|------|---------|---------|------|
| **开发** | `init-all.sql` | DDL + DML | 包含测试数据 |
| **测试** | `init-all.sql` | DDL + DML | 包含测试数据 |
| **UAT** | `init-ddl-only.sql` | 仅 DDL | 不包含测试数据 |
| **生产** | `init-ddl-only.sql` | 仅 DDL | 不包含测试数据 |

---

## 📝 注意事项

1. **生产环境**:
   - 使用 `init-ddl-only.sql`
   - 不要执行 DML 脚本
   - 由 DBA 审核后执行

2. **权限管理**:
   - DDL 操作需要 `CREATE TABLE` 权限
   - DML 操作需要 `INSERT` 权限
   - 生产环境应用账户通常只有 DML 权限

3. **备份**:
   - 执行脚本前备份数据库
   - 特别是在生产环境

4. **幂等性**:
   - 所有脚本都是幂等的（可重复执行）
   - 使用 `IF EXISTS` 检查对象是否存在

---

## 🔗 相关文档

- [快速启动指南](../QUICK_START.md)
- [Spring Batch 系统表说明](../doc/SPRING_BATCH_TABLES.md)
- [SQL Server 配置指南](../doc/SQLSERVER_SETUP.md)
- [启动脚本说明](README.md)

---

**文档版本**: 1.0  
**最后更新**: 2024-01-01
