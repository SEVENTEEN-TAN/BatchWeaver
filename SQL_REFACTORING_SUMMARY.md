# SQL 脚本重构总结

> 按照最佳实践重构 SQL 脚本结构

---

## 🎯 重构目标

根据你的建议，将 SQL 脚本进行了以下改进：

1. ✅ **DDL 和 DML 分离** - 数据定义和数据操作完全分离
2. ✅ **系统表和业务表分离** - 职责清晰，便于维护
3. ✅ **支持不同环境** - 开发环境和生产环境使用不同脚本
4. ✅ **脚本模块化** - 每个脚本职责单一，便于版本控制

---

## 📁 新的目录结构

```
scripts/
├── ddl/                                    # DDL 脚本
│   ├── 01-create-database.sql             # 创建数据库
│   ├── 02-create-business-tables.sql      # 创建业务表
│   └── 03-create-batch-tables.sql         # 创建 Spring Batch 系统表
│
├── dml/                                    # DML 脚本
│   └── 01-insert-test-data.sql            # 插入测试数据
│
├── init-all.sql                            # 完整初始化（开发环境）
├── init-ddl-only.sql                       # 仅 DDL（生产环境）
└── verify-tables.sql                       # 验证脚本
```

---

## 🔄 重构对比

### 重构前

```
scripts/
└── init-sqlserver.sql                      # 单一大文件
    ├── 创建数据库
    ├── 创建业务表
    ├── 创建系统表
    └── 插入测试数据（混在一起）
```

**问题**:
- ❌ DDL 和 DML 混在一起
- ❌ 系统表和业务表混在一起
- ❌ 无法区分开发和生产环境
- ❌ 难以维护和版本控制

### 重构后

```
scripts/
├── ddl/                                    # DDL 独立目录
│   ├── 01-create-database.sql
│   ├── 02-create-business-tables.sql      # 业务表独立
│   └── 03-create-batch-tables.sql         # 系统表独立
│
├── dml/                                    # DML 独立目录
│   └── 01-insert-test-data.sql
│
├── init-all.sql                            # 开发环境
└── init-ddl-only.sql                       # 生产环境
```

**优势**:
- ✅ DDL 和 DML 完全分离
- ✅ 系统表和业务表职责清晰
- ✅ 支持不同环境的初始化需求
- ✅ 便于维护和版本控制
- ✅ 脚本按序号命名，执行顺序清晰

---

## 🚀 使用方式

### 开发环境

```bash
# 完整初始化（包含测试数据）
sqlcmd -S localhost -U sa -P YourPassword -i scripts\init-all.sql
```

**执行内容**:
1. DDL: 创建数据库
2. DDL: 创建业务表
3. DDL: 创建系统表
4. DML: 插入测试数据 ✅

### 生产环境

```bash
# 仅 DDL 初始化（不含测试数据）
sqlcmd -S localhost -U sa -P YourPassword -i scripts\init-ddl-only.sql
```

**执行内容**:
1. DDL: 创建数据库
2. DDL: 创建业务表
3. DDL: 创建系统表
4. ~~DML: 插入测试数据~~ ❌（跳过）

---

## 📊 脚本详细说明

### DDL 脚本

| 脚本 | 内容 | 说明 |
|------|------|------|
| `01-create-database.sql` | 创建 `BatchWeaverDB` 数据库 | 所有环境通用 |
| `02-create-business-tables.sql` | 创建 `USER_DATA` 等业务表 | 可根据需求扩展 |
| `03-create-batch-tables.sql` | 创建 6 张 Spring Batch 系统表 | 框架必需 |

### DML 脚本

| 脚本 | 内容 | 说明 |
|------|------|------|
| `01-insert-test-data.sql` | 向 `USER_DATA` 插入 3 条测试数据 | 仅开发/测试环境 |

### 主脚本

| 脚本 | 用途 | 适用环境 |
|------|------|---------|
| `init-all.sql` | 完整初始化（DDL + DML） | 开发、测试 |
| `init-ddl-only.sql` | 仅 DDL 初始化 | 生产、UAT |

---

## ✅ 最佳实践

### 1. DDL 和 DML 分离

**原因**:
- DDL 操作需要高权限（CREATE TABLE）
- DML 操作只需要数据权限（INSERT）
- 生产环境通常只执行 DDL，不执行测试数据插入

**实现**:
- DDL 脚本放在 `ddl/` 目录
- DML 脚本放在 `dml/` 目录
- 主脚本根据环境选择性引用

### 2. 系统表和业务表分离

**原因**:
- 系统表（Spring Batch）由框架定义，不应随意修改
- 业务表根据项目需求变化
- 分离后便于独立维护和升级

**实现**:
- `02-create-business-tables.sql` - 业务表
- `03-create-batch-tables.sql` - 系统表

### 3. 脚本按序号命名

**原因**:
- 确保执行顺序正确
- 便于理解依赖关系
- 新增脚本时容易插入

**实现**:
- `01-` 创建数据库（最先执行）
- `02-` 创建业务表
- `03-` 创建系统表（可能依赖业务表）

### 4. 幂等性设计

**原因**:
- 脚本可以重复执行
- 便于调试和修复
- 避免执行失败后的清理工作

**实现**:
```sql
-- 检查对象是否存在
IF OBJECT_ID('USER_DATA', 'U') IS NOT NULL
    DROP TABLE USER_DATA;
GO

-- 创建对象
CREATE TABLE USER_DATA (...);
GO
```

---

## 🔍 环境对比

| 环境 | 使用脚本 | DDL | DML | 说明 |
|------|---------|-----|-----|------|
| **开发** | `init-all.sql` | ✅ | ✅ | 包含测试数据 |
| **测试** | `init-all.sql` | ✅ | ✅ | 包含测试数据 |
| **UAT** | `init-ddl-only.sql` | ✅ | ❌ | 不含测试数据 |
| **生产** | `init-ddl-only.sql` | ✅ | ❌ | 不含测试数据 |

---

## 📝 版本控制建议

### 添加新业务表

1. 创建新的 DDL 脚本：
   ```
   ddl/04-create-order-tables.sql
   ```

2. 更新主脚本 `init-all.sql` 和 `init-ddl-only.sql`

3. 如需测试数据，创建对应 DML 脚本：
   ```
   dml/02-insert-order-data.sql
   ```

### 修改现有表结构

1. 创建迁移脚本：
   ```
   ddl/05-alter-user-data-add-phone.sql
   ```

2. 使用 `ALTER TABLE` 而不是 `DROP/CREATE`

3. 添加版本注释：
   ```sql
   -- Version: 1.1.0
   -- Date: 2024-01-01
   -- Description: Add phone column to USER_DATA
   ```

---

## 🎉 重构成果

### 文件清单

**新增文件**:
- ✅ `scripts/ddl/01-create-database.sql`
- ✅ `scripts/ddl/02-create-business-tables.sql`
- ✅ `scripts/ddl/03-create-batch-tables.sql`
- ✅ `scripts/dml/01-insert-test-data.sql`
- ✅ `scripts/init-all.sql`
- ✅ `scripts/init-ddl-only.sql`
- ✅ `scripts/SQL_SCRIPTS_README.md`
- ✅ `scripts/STRUCTURE.md`

**更新文件**:
- ✅ `scripts/README.md` - 更新使用说明
- ✅ `QUICK_START.md` - 更新初始化步骤
- ✅ `scripts/init-sqlserver.sql` - 标记为废弃

### 代码统计

- **DDL 脚本**: 3 个文件，约 200 行
- **DML 脚本**: 1 个文件，约 20 行
- **主脚本**: 2 个文件，约 100 行
- **文档**: 2 个文件，约 500 行

---

## 📚 相关文档

- [SQL 脚本详细说明](scripts/SQL_SCRIPTS_README.md)
- [脚本结构可视化](scripts/STRUCTURE.md)
- [启动脚本说明](scripts/README.md)
- [快速启动指南](QUICK_START.md)
- [Spring Batch 系统表说明](doc/SPRING_BATCH_TABLES.md)

---

## 🙏 致谢

感谢你提出的宝贵建议！这次重构让脚本结构更加清晰、专业，符合数据库开发的最佳实践。

**重构原则**:
- ✅ 分离关注点（DDL/DML）
- ✅ 单一职责（系统表/业务表）
- ✅ 环境区分（开发/生产）
- ✅ 便于维护（模块化、版本控制）

---

**文档版本**: 1.0  
**重构日期**: 2024-01-01  
**提交记录**: `a555f6a`
