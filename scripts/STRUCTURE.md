# SQL 脚本目录结构

```
scripts/
│
├── 📁 ddl/                                    # DDL（数据定义语言）
│   ├── 01-create-database.sql                # ✅ 创建数据库
│   ├── 02-create-business-tables.sql         # ✅ 创建业务表
│   └── 03-create-batch-tables.sql            # ✅ 创建 Spring Batch 系统表
│
├── 📁 dml/                                    # DML（数据操作语言）
│   └── 01-insert-test-data.sql               # ✅ 插入测试数据
│
├── 📄 init-all.sql                            # 🚀 完整初始化（开发环境）
├── 📄 init-ddl-only.sql                       # 🏭 仅 DDL（生产环境）
├── 📄 verify-tables.sql                       # ✔️  验证脚本
│
├── 📄 init-sqlserver.sql                      # ⚠️  已废弃
│
├── 📄 run-job.bat                             # 🪟 Windows 启动脚本
├── 📄 run-job.sh                              # 🐧 Linux/Mac 启动脚本
│
├── 📖 README.md                               # 启动脚本说明
├── 📖 SQL_SCRIPTS_README.md                   # SQL 脚本详细说明
└── 📖 STRUCTURE.md                            # 本文件
```

---

## 🎯 快速使用

### 开发环境

```bash
# 完整初始化（DDL + DML）
sqlcmd -S localhost -U sa -P YourPassword -i scripts\init-all.sql
```

### 生产环境

```bash
# 仅 DDL 初始化
sqlcmd -S localhost -U sa -P YourPassword -i scripts\init-ddl-only.sql
```

### 验证

```bash
# 验证所有表是否正确创建
sqlcmd -S localhost -U sa -P YourPassword -i scripts\verify-tables.sql
```

---

## 📊 执行流程

### init-all.sql（开发环境）

```
1. ddl/01-create-database.sql
   └─> 创建 BatchWeaverDB 数据库

2. ddl/02-create-business-tables.sql
   └─> 创建 USER_DATA 表
   └─> 创建索引

3. ddl/03-create-batch-tables.sql
   └─> 创建 6 张 Spring Batch 系统表
   └─> 创建外键约束
   └─> 创建性能索引

4. dml/01-insert-test-data.sql
   └─> 插入 3 条测试数据

5. 验证所有表
```

### init-ddl-only.sql（生产环境）

```
1. ddl/01-create-database.sql
   └─> 创建 BatchWeaverDB 数据库

2. ddl/02-create-business-tables.sql
   └─> 创建 USER_DATA 表
   └─> 创建索引

3. ddl/03-create-batch-tables.sql
   └─> 创建 6 张 Spring Batch 系统表
   └─> 创建外键约束
   └─> 创建性能索引

4. 验证所有表

⚠️ 不包含测试数据
```

---

## 🔍 脚本详情

| 脚本 | 类型 | 用途 | 环境 |
|------|------|------|------|
| `ddl/01-create-database.sql` | DDL | 创建数据库 | 所有 |
| `ddl/02-create-business-tables.sql` | DDL | 创建业务表 | 所有 |
| `ddl/03-create-batch-tables.sql` | DDL | 创建系统表 | 所有 |
| `dml/01-insert-test-data.sql` | DML | 插入测试数据 | 开发/测试 |
| `init-all.sql` | 主脚本 | 完整初始化 | 开发/测试 |
| `init-ddl-only.sql` | 主脚本 | 仅 DDL | 生产/UAT |
| `verify-tables.sql` | 验证 | 验证表创建 | 所有 |

---

## ✅ 最佳实践

1. **版本控制**: 所有脚本都纳入 Git 版本控制
2. **幂等性**: 所有脚本可重复执行
3. **分离原则**: DDL 和 DML 完全分离
4. **环境区分**: 开发和生产使用不同脚本
5. **顺序执行**: 脚本按序号命名，确保执行顺序
6. **文档完善**: 每个脚本都有清晰的注释

---

## 📚 相关文档

- [SQL 脚本详细说明](SQL_SCRIPTS_README.md)
- [启动脚本说明](README.md)
- [快速启动指南](../QUICK_START.md)
- [Spring Batch 系统表说明](../doc/SPRING_BATCH_TABLES.md)
