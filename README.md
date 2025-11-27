# Spring Batch 动态调度框架

本项目是一个基于 Spring Batch 的轻量级动态调度框架，支持通过 XML 配置定义 Job 流程，并利用反射机制调用业务逻辑。

## 核心特性
- **动态配置**: 无需修改代码，通过 XML 即可定义 Job。
- **反射调用**: 业务逻辑与框架解耦，支持调用任意 Bean 的方法。
- **断点续传**: 原生支持 Spring Batch 的断点续传机制。

## 文档导航
- **[框架设计](doc/FRAMEWORK_DESIGN.md)**: 了解核心设计原理。
- **[快速开始](doc/QUICK_START.md)**: 如何运行 Demo 和断点续传任务。

## 快速上手
1. 初始化数据库: 执行 `scripts/init.sql`。
2. 修改配置: 更新 `application.yml` 数据库连接。
3. 运行任务: `java -jar app.jar jobName=demoJob`。