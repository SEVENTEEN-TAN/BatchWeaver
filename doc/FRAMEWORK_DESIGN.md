# 框架设计文档

## 核心设计理念
本项目采用**动态配置驱动**的设计模式，旨在降低 Spring Batch Job 的开发和维护成本。通过 XML 定义 Job 流程，结合反射机制自动执行业务逻辑，实现了 Job 定义与代码实现的解耦。

## 核心组件

### 1. XmlJobParser
- **功能**: 负责启动时扫描 `classpath:jobs/*.xml` 文件。
- **原理**: 使用 Jackson XML 解析器将 XML 转换为 Java 对象 (`JobXml`, `StepXml`)，然后通过 Spring Batch 的 `JobBuilder` 和 `StepBuilder` 动态构建 Job 实例并注册到 Spring 容器中。
- **优势**: 新增 Job 无需修改 Java 代码，只需添加 XML 配置文件。

### 2. ReflectionTasklet
- **功能**: 通用的 Tasklet 实现，用于执行具体的业务逻辑。
- **原理**: 
    - 接收 XML 中配置的 `className` 和 `methodName`。
    - 在运行时通过反射（Reflection）实例化目标类（或从 Spring 容器获取 Bean）并调用指定方法。
    - 支持将 XML 中的 `properties` 注入到方法参数或 Bean 属性中。
- **优势**: 开发者只需编写普通的 Java Service 方法，无需实现复杂的 Spring Batch 接口。

## 目录结构
- `src/main/resources/jobs/`: 存放 Job 的 XML 配置文件。
- `src/main/java/com/example/batch/service/`: 存放具体的业务逻辑 Service。
- `src/main/java/com/example/batch/core/`: 核心框架代码 (`XmlJobParser`, `ReflectionTasklet`)。

## 扩展性
- **新增业务**: 编写新的 Service 类 -> 在 XML 中配置新的 Step。
- **新增流程**: 新建 XML 文件 -> 定义 Steps 顺序。
