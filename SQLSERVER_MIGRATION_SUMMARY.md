# SQL Server 2022 迁移总结

> BatchWeaver 已成功从 H2 迁移到 SQL Server 2022

## ✅ 完成的工作

### 1. 创建新分支
- **分支名**: `feature/sqlserver-2022`
- **基于**: `main` 分支

### 2. 代码变更

#### pom.xml
- ✅ 添加 SQL Server JDBC 驱动 (`mssql-jdbc`)
- ✅ 将 H2 依赖范围改为 `test`（仅用于测试）

#### application.yml
- ✅ 更新数据源配置为 SQL Server
- ✅ 配置 Druid 连接池使用 SQL Server
- ✅ 添加 SQL Server 连接参数（encrypt, trustServerCertificate）

#### schema-all.sql
- ✅ 转换为 SQL Server 语法
- ✅ 使用 `IF OBJECT_ID` 替代 `DROP TABLE IF EXISTS`
- ✅ 使用 `NVARCHAR` 替代 `VARCHAR`（支持 Unicode）
- ✅ 添加 `GO` 批处理分隔符

### 3. 新增文档

#### doc/SQLSERVER_SETUP.md
完整的 SQL Server 配置指南，包含：
- 安装步骤
- 数据库创建
- 用户配置
- 连接字符串说明
- 故障排查
- 性能优化
- 安全建议
- 备份恢复

#### doc/MIGRATION_H2_TO_SQLSERVER.md
详细的迁移文档，包含：
- 迁移概述
- 主要变更
- 迁移步骤
- 数据类型映射
- 注意事项
- 兼容性处理
- 常见问题
- 迁移检查清单

#### scripts/init-sqlserver.sql
数据库初始化脚本，包含：
- 创建数据库
- 创建业务表
- 创建索引
- 插入测试数据
- 验证脚本

### 4. 更新文档

#### README.md
- ✅ 更新数据库部分（H2 → SQL Server 2022）
- ✅ 更新技术栈说明
- ✅ 添加 SQL Server 配置文档链接

---

## 📋 配置信息

### 默认配置

```yaml
数据库服务器: localhost:1433
数据库名称: BatchWeaverDB
用户名: sa
密码: YourStrong@Password (需要修改)
```

### 连接字符串

```
jdbc:sqlserver://localhost:1433;databaseName=BatchWeaverDB;encrypt=true;trustServerCertificate=true
```

---

## 🚀 快速开始

### 1. 安装 SQL Server 2022

下载并安装 SQL Server 2022 Developer Edition（免费）：
https://www.microsoft.com/sql-server/sql-server-downloads

### 2. 初始化数据库

```bash
# 使用 sqlcmd
sqlcmd -S localhost -U sa -P YourStrong@Password -i scripts/init-sqlserver.sql

# 或使用 SSMS 打开并执行 scripts/init-sqlserver.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`，更新密码：

```yaml
spring:
  datasource:
    password: 你的实际密码
    druid:
      password: 你的实际密码
```

### 4. 编译运行

```bash
# 编译
mvn clean package -DskipTests

# 运行
scripts\run-job.bat jobName=demoJob
```

---

## 📊 变更统计

| 类型 | 数量 |
|------|------|
| 修改文件 | 4 |
| 新增文件 | 3 |
| 新增文档 | 2 |
| 新增脚本 | 1 |
| 代码行数 | +726 / -15 |

---

## 🔄 Git 操作

### 查看分支

```bash
git branch
# * feature/sqlserver-2022
#   main
```

### 查看变更

```bash
git log --oneline -1
# d53caba feat: 迁移数据库从 H2 到 SQL Server 2022
```

### 合并到主分支（可选）

```bash
# 切换到主分支
git checkout main

# 合并 feature 分支
git merge feature/sqlserver-2022

# 推送到远程
git push origin main
```

---

## 📚 相关文档

| 文档 | 说明 |
|------|------|
| [SQL Server 配置指南](doc/SQLSERVER_SETUP.md) | 完整的安装和配置说明 |
| [迁移文档](doc/MIGRATION_H2_TO_SQLSERVER.md) | 从 H2 迁移的详细步骤 |
| [快速参考](doc/QUICK_REFERENCE.md) | 常用代码和命令 |
| [代码架构](doc/CODE_ARCHITECTURE.md) | 项目架构说明 |

---

## ⚠️ 注意事项

### 1. 密码安全

**不要**在生产环境使用默认密码 `YourStrong@Password`！

建议：
- 使用强密码
- 使用环境变量
- 使用密钥管理服务

### 2. 连接配置

开发环境使用 `trustServerCertificate=true` 是可以的，但生产环境应该：
- 配置正确的 SSL 证书
- 移除 `trustServerCertificate=true`
- 启用 `encrypt=true`

### 3. 数据迁移

如果有现有的 H2 数据需要迁移，请参考 [迁移文档](doc/MIGRATION_H2_TO_SQLSERVER.md)。

---

## 🐛 故障排查

### 无法连接到 SQL Server

1. 确认 SQL Server 服务正在运行
2. 确认 TCP/IP 协议已启用
3. 确认防火墙允许 1433 端口
4. 查看详细的故障排查：[SQLSERVER_SETUP.md](doc/SQLSERVER_SETUP.md#故障排查)

### 登录失败

1. 确认启用了 SQL Server 身份验证（混合模式）
2. 确认密码正确
3. 确认 `sa` 账户未被禁用

---

## ✨ 优势

### 为什么选择 SQL Server 2022？

1. **企业级特性**
   - 完整的 ACID 事务支持
   - 高级备份和恢复
   - 完善的安全机制

2. **性能优势**
   - 更好的并发处理
   - 查询优化器
   - 内存优化表

3. **生产就绪**
   - 成熟稳定
   - 丰富的监控工具
   - 完善的文档支持

4. **开发体验**
   - SQL Server Management Studio (SSMS)
   - Azure Data Studio
   - 丰富的第三方工具

---

## 📞 获取帮助

如果遇到问题：
1. 查看 [SQL Server 配置指南](doc/SQLSERVER_SETUP.md)
2. 查看 [迁移文档](doc/MIGRATION_H2_TO_SQLSERVER.md)
3. 查看 SQL Server 错误日志
4. 查看应用程序日志

---

**迁移完成时间**: 2024-01-01  
**分支**: feature/sqlserver-2022  
**状态**: ✅ 完成  
**测试**: ⏳ 待测试
