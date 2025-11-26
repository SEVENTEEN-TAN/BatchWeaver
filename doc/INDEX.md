# BatchWeaver 文档索引

> 快速找到你需要的文档

## 📚 文档导航

### 🚀 快速开始

| 文档 | 适合人群 | 内容概要 |
|------|---------|---------|
| [README.md](../README.md) | 所有人 | 项目概览、快速开始、核心特性 |
| [快速参考](QUICK_REFERENCE.md) | 开发者 | 常用代码片段、配置模板、命令速查 |

### 📖 使用指南

| 文档 | 适合人群 | 内容概要 |
|------|---------|---------|
| [完整文档](WALKTHROUGH.md) | 用户 | 详细的使用指南和 API 说明 |
| [设计文档](JOB_DESIGN.md) | 开发者 | Job/Step 架构设计和最佳实践 |

### 🏗️ 架构设计

| 文档 | 适合人群 | 内容概要 |
|------|---------|---------|
| [代码架构](CODE_ARCHITECTURE.md) | 开发者 | 从代码角度深入理解项目实现 |
| [执行流程](EXECUTION_FLOW.md) | 开发者 | 可视化展示完整执行链路 |
| [实施计划](IMPLEMENTATION_PLAN.md) | 架构师 | 技术方案说明 |

---

## 🎯 按场景查找

### 我想了解项目

1. 先看 [README.md](../README.md) - 了解项目是什么
2. 再看 [设计文档](JOB_DESIGN.md) - 理解核心概念
3. 最后看 [代码架构](CODE_ARCHITECTURE.md) - 深入技术细节

### 我想快速上手

1. 看 [README.md](../README.md) 的"快速开始"部分
2. 参考 [快速参考](QUICK_REFERENCE.md) 的代码模板
3. 运行示例 Job 验证环境

### 我想开发新功能

1. 参考 [快速参考](QUICK_REFERENCE.md) 的 Tasklet 模板
2. 查看 [设计文档](JOB_DESIGN.md) 了解最佳实践
3. 参考 [代码架构](CODE_ARCHITECTURE.md) 的扩展指南

### 我想理解原理

1. 阅读 [代码架构](CODE_ARCHITECTURE.md) - 了解组件设计
2. 阅读 [执行流程](EXECUTION_FLOW.md) - 理解执行链路
3. 查看源代码 - 深入实现细节

### 我遇到了问题

1. 查看 [快速参考](QUICK_REFERENCE.md) 的"常见问题"
2. 查看 [执行流程](EXECUTION_FLOW.md) 的"异常处理流程"
3. 查看日志和数据库元数据表

---

## 📋 文档详情

### 1. README.md

**位置**: 项目根目录  
**内容**:
- 项目简介
- 核心特性
- 快速开始
- Job 定义示例
- 技术栈
- 示例 Job

**适合场景**:
- 第一次接触项目
- 了解项目能做什么
- 快速搭建环境

---

### 2. 快速参考 (QUICK_REFERENCE.md)

**位置**: doc/QUICK_REFERENCE.md  
**内容**:
- 创建新 Job 的完整流程
- 6 种常用 Tasklet 模板
- 5 种 XML 配置模板
- 数据库操作示例
- 文件操作示例
- 常见问题解答
- 命令速查

**适合场景**:
- 需要快速编写代码
- 查找配置模板
- 解决常见问题

**亮点**:
- ✅ 即拿即用的代码片段
- ✅ 覆盖 90% 的常见场景
- ✅ 包含完整的示例代码

---

### 3. 设计文档 (JOB_DESIGN.md)

**位置**: doc/JOB_DESIGN.md  
**内容**:
- 架构设计（1 XML = 1 Job = N Steps）
- 示例结构
- 执行流程
- Step 属性说明
- 创建新 Job 的步骤
- 断点续传机制
- 最佳实践

**适合场景**:
- 理解 Job/Step 概念
- 学习最佳实践
- 设计复杂的 Job 流程

**亮点**:
- ✅ 清晰的概念说明
- ✅ 丰富的示例
- ✅ 实用的最佳实践

---

### 4. 代码架构 (CODE_ARCHITECTURE.md)

**位置**: doc/CODE_ARCHITECTURE.md  
**内容**:
- 项目结构
- 核心架构图
- 关键组件详解（6 个核心类）
- 执行流程
- 数据模型
- 配置说明
- 扩展指南

**适合场景**:
- 深入理解实现原理
- 修改核心代码
- 扩展框架功能

**亮点**:
- ✅ 详细的代码分析
- ✅ 完整的架构图
- ✅ 实用的扩展指南

**核心组件**:
1. **BatchApplication** - 应用入口
2. **XmlJobParser** - XML 解析和 Job 构建
3. **ReflectionTasklet** - 反射执行器
4. **DynamicJobRunner** - 命令行运行器
5. **FileValidator** - 文件校验器
6. **DruidConfig** - 数据源配置

---

### 5. 执行流程 (EXECUTION_FLOW.md)

**位置**: doc/EXECUTION_FLOW.md  
**内容**:
- 启动流程（时序图）
- Job 加载流程（流程图）
- Job 执行流程（详细流程图）
- 断点续传流程
- 异常处理流程

**适合场景**:
- 理解系统运行机制
- 调试问题
- 优化性能

**亮点**:
- ✅ 可视化流程图
- ✅ 详细的执行示例
- ✅ 完整的异常处理说明

**包含的流程图**:
- 启动时序图
- Job 加载流程图
- Job 执行流程图
- 断点续传流程图
- 异常传播链

---

### 6. 完整文档 (WALKTHROUGH.md)

**位置**: doc/WALKTHROUGH.md  
**内容**:
- 详细的使用指南
- API 说明
- 配置参数
- 高级特性

**适合场景**:
- 系统学习
- 查阅 API
- 了解高级特性

---

### 7. 实施计划 (IMPLEMENTATION_PLAN.md)

**位置**: doc/IMPLEMENTATION_PLAN.md  
**内容**:
- 目标描述
- 技术方案
- 拟议变更
- 验证计划

**适合场景**:
- 了解项目背景
- 理解技术选型
- 项目规划

---

## 🔍 关键概念速查

### Job
- **定义**: 一个完整的批处理任务
- **对应**: 1 个 XML 文件
- **组成**: 多个 Step 按顺序执行
- **示例**: importJob, multiStepJob

### Step
- **定义**: Job 中的一个执行单元
- **对应**: 1 个 Java 类的方法
- **属性**: id, className, methodName, properties
- **示例**: validate, import, transform

### Tasklet
- **定义**: Step 的具体实现类
- **要求**: 必须有 execute() 方法
- **注解**: @Component
- **示例**: FileValidationTasklet, CsvImportTasklet

### ReflectionTasklet
- **定义**: 通用的 Tasklet 执行器
- **功能**: 通过反射调用业务类的方法
- **优势**: 无需为每个 Step 创建 Tasklet 实现

### JobParameters
- **定义**: Job 执行时的参数
- **来源**: 命令行参数
- **用途**: 传递配置、实现断点续传
- **示例**: jobName=importJob date=2024-01-01

---

## 🛠️ 常用操作速查

### 创建新 Job
```bash
# 1. 创建 Java 类
src/main/java/com/example/batch/steps/MyTasklet.java

# 2. 创建 XML 配置
src/main/resources/jobs/my-job.xml

# 3. 编译运行
mvn clean package -DskipTests
.\run-job.bat jobName=myJob
```

### 查看执行历史
```sql
SELECT * FROM BATCH_JOB_EXECUTION ORDER BY CREATE_TIME DESC;
SELECT * FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ?;
```

### 断点续传
```bash
.\run-job.bat jobName=myJob resume=true
```

### 查看日志
```bash
# 日志位置: 控制台输出
# 日志级别配置: src/main/resources/application.yml
```

---

## 📞 获取帮助

### 文档问题
- 查看本索引找到对应文档
- 查看 [快速参考](QUICK_REFERENCE.md) 的常见问题

### 代码问题
- 查看 [代码架构](CODE_ARCHITECTURE.md) 的组件详解
- 查看 [执行流程](EXECUTION_FLOW.md) 的流程图

### 使用问题
- 查看 [设计文档](JOB_DESIGN.md) 的最佳实践
- 查看 [快速参考](QUICK_REFERENCE.md) 的示例代码

---

## 📝 文档更新日志

### v1.0 (2024-01-01)
- ✅ 新增代码架构文档
- ✅ 新增执行流程文档
- ✅ 新增快速参考文档
- ✅ 新增文档索引

### 未来计划
- 📋 添加性能优化指南
- 📋 添加监控和告警配置
- 📋 添加常见错误排查手册
- 📋 添加视频教程

---

**文档版本**: 1.0  
**最后更新**: 2024-01-01  
**维护者**: BatchWeaver Team
