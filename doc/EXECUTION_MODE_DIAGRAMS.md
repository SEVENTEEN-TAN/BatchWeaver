# 执行模式流程图

本文档通过 Mermaid 图表展示 BatchWeaver 四种执行模式的工作流程和状态转换。

---

## 1. 整体架构流程

```mermaid
flowchart TB
    subgraph Input["用户输入"]
        CLI["java -jar app.jar<br/>jobName=xxx _mode=xxx"]
    end

    subgraph Core["核心处理层"]
        Runner["DynamicJobRunner<br/>参数解析 & 协调"]
        Validator["ExecutionModeValidator<br/>规则校验"]
        Registry["JobMetadataRegistry<br/>@BatchJob 元数据"]
        Builder["DynamicJobBuilderService<br/>Job 动态构建"]
        Status["ExecutionStatusService<br/>历史状态查询"]
    end

    subgraph SpringBatch["Spring Batch 层"]
        Launcher["JobLauncher"]
        Repo["JobRepository"]
        Explorer["JobExplorer"]
    end

    subgraph Output["执行结果"]
        Success["COMPLETED"]
        Failed["FAILED"]
        Skipped["COMPLETED<br/>(with skipped)"]
    end

    CLI --> Runner
    Runner --> Registry
    Runner --> Validator
    Validator --> Status
    Validator --> Registry
    Runner --> Builder
    Builder --> Status
    Builder --> Registry
    Builder --> Launcher
    Launcher --> Repo
    Status --> Explorer
    Launcher --> Success
    Launcher --> Failed
    Launcher --> Skipped
```

---

## 2. 执行模式决策流程

```mermaid
flowchart TD
    Start([开始]) --> ParseParams["解析参数<br/>jobName, _mode, id, _target_steps"]
    ParseParams --> CheckMode{_mode?}
    
    CheckMode -->|STANDARD| StdCheck{"携带 ID?"}
    CheckMode -->|RESUME| ResCheck{"携带 ID?"}
    CheckMode -->|SKIP_FAIL| SkipCheck{"携带 ID?"}
    CheckMode -->|ISOLATED| IsoCheck{"携带 _target_steps?"}
    CheckMode -->|未指定| StdCheck
    
    StdCheck -->|是| RejectStd["拒绝: STANDARD 不能带 ID"]
    StdCheck -->|否| ExecStd["执行原生 Job Flow"]
    
    ResCheck -->|否| RejectRes["拒绝: RESUME 必须带 ID"]
    ResCheck -->|是| CheckHistory{"查询历史模式"}
    CheckHistory -->|SKIP_FAIL/ISOLATED| RejectResHist["拒绝: 流程已破坏"]
    CheckHistory -->|STANDARD/RESUME| CheckFailed{"有失败 Step?"}
    CheckFailed -->|否| RejectNoFail["拒绝: 无需续传"]
    CheckFailed -->|是| ExecRes["构建续传 Flow<br/>从失败 Step 开始"]
    
    SkipCheck -->|是| RejectSkip["拒绝: SKIP_FAIL 不能带 ID"]
    SkipCheck -->|否| CheckCond{"条件流 Job?"}
    CheckCond -->|是| RejectCond["拒绝: 条件流不支持 SKIP_FAIL"]
    CheckCond -->|否| ExecSkip["构建容错 Flow<br/>失败跳过继续"]
    
    IsoCheck -->|否| RejectIso["拒绝: 缺少 _target_steps"]
    IsoCheck -->|是| ValidateSteps{"校验 Step 名称"}
    ValidateSteps -->|无效| RejectStep["拒绝: 无效 Step 名称"]
    ValidateSteps -->|有效| ExecIso["构建指定 Step Flow"]
    
    ExecStd --> Launch["JobLauncher.run()"]
    ExecRes --> Launch
    ExecSkip --> Launch
    ExecIso --> Launch
    
    Launch --> End([结束])
    RejectStd --> End
    RejectRes --> End
    RejectResHist --> End
    RejectNoFail --> End
    RejectSkip --> End
    RejectCond --> End
    RejectIso --> End
    RejectStep --> End
```

---

## 3. 模式状态转换图

```mermaid
stateDiagram-v2
    [*] --> NewExecution: 无 ID
    [*] --> HistoricalExecution: 带 ID

    state NewExecution {
        [*] --> STANDARD
        [*] --> SKIP_FAIL: 仅线性流
        [*] --> ISOLATED_NEW
        
        STANDARD --> STD_COMPLETED: 成功
        STANDARD --> STD_FAILED: 失败
        
        SKIP_FAIL --> SF_COMPLETED: 完成(可能含跳过)
        
        ISOLATED_NEW --> ISO_COMPLETED: 完成
        ISOLATED_NEW --> ISO_FAILED: 失败
    }

    state HistoricalExecution {
        [*] --> CheckHistoricalMode
        
        CheckHistoricalMode --> RESUME: 历史=STD/RES + 有失败
        CheckHistoricalMode --> ISOLATED_HIST: 任意历史
        CheckHistoricalMode --> REJECTED: 历史=SF/ISO + mode=RESUME
        
        RESUME --> RES_COMPLETED: 成功
        RESUME --> RES_FAILED: 失败
        
        ISOLATED_HIST --> ISO_H_COMPLETED: 完成
    }

    STD_FAILED --> HistoricalExecution: 修补
    RES_FAILED --> HistoricalExecution: 再次续传
    ISO_FAILED --> HistoricalExecution: 修补
```

---

## 4. 四种模式对比

```mermaid
graph LR
    subgraph STANDARD["STANDARD 标准模式"]
        S1["Step1"] --> S2["Step2"] --> S3["Step3"]
        S2 -.->|失败| SX["Job 终止"]
    end

    subgraph RESUME["RESUME 断点续传"]
        R1["Step1 ✓"] -.->|已完成| R2["Step2 ✗"]
        R2 -->|续传| R2R["Step2"] --> R3["Step3"]
    end

    subgraph SKIP_FAIL["SKIP_FAIL 容错执行"]
        K1["Step1"] --> K2["Step2 ✗"]
        K2 -->|跳过| K3["Step3"]
        K3 --> KE["Job COMPLETED<br/>(with warnings)"]
    end

    subgraph ISOLATED["ISOLATED 独立执行"]
        I1["指定: Step2, Step3"]
        I1 --> I2["Step2"] --> I3["Step3"]
        IX["Step1 跳过"]
    end
```

---

## 5. ID 规则矩阵

```mermaid
graph TB
    subgraph Rules["ID 规则"]
        direction LR
        
        subgraph NoID["无 ID (新执行)"]
            N_STD["STANDARD ✅"]
            N_SF["SKIP_FAIL ✅"]
            N_ISO["ISOLATED ✅"]
            N_RES["RESUME ❌"]
        end
        
        subgraph WithID["带 ID (历史修补)"]
            W_STD["STANDARD ❌"]
            W_SF["SKIP_FAIL ❌"]
            W_ISO["ISOLATED ✅"]
            W_RES["RESUME ✅<br/>(需校验历史模式)"]
        end
    end
```

---

## 6. 模式后续限制

```mermaid
flowchart LR
    subgraph History["历史执行模式"]
        H_STD["STANDARD<br/>(失败)"]
        H_RES["RESUME<br/>(失败)"]
        H_SF["SKIP_FAIL"]
        H_ISO["ISOLATED"]
    end

    subgraph Next["后续可用模式"]
        RESUME_OK["RESUME ✅"]
        RESUME_NO["RESUME ❌"]
        ISO_OK["ISOLATED ✅"]
    end

    H_STD --> RESUME_OK
    H_STD --> ISO_OK
    
    H_RES --> RESUME_OK
    H_RES --> ISO_OK
    
    H_SF --> RESUME_NO
    H_SF --> ISO_OK
    
    H_ISO --> RESUME_NO
    H_ISO --> ISO_OK
```

---

## 7. SKIP_FAIL 容错流程详解

```mermaid
sequenceDiagram
    participant User as 用户
    participant Runner as DynamicJobRunner
    participant Builder as DynamicJobBuilderService
    participant Flow as FaultTolerantFlow
    participant Step1 as Step1
    participant Step2 as Step2
    participant Step3 as Step3
    participant Listener as SkipFailSummaryListener

    User->>Runner: _mode=SKIP_FAIL
    Runner->>Builder: buildSkipFailJob()
    Builder->>Flow: 构建容错 Flow
    
    Flow->>Step1: 执行
    Step1-->>Flow: COMPLETED
    
    Flow->>Step2: 执行
    Step2-->>Flow: FAILED
    Note over Flow: on("*").to(Step3)
    
    Flow->>Step3: 继续执行
    Step3-->>Flow: COMPLETED
    
    Flow->>Listener: afterJob()
    Listener-->>Listener: 汇总: Step2 被跳过
    Listener->>User: COMPLETED (with 1 skipped failure)
```

---

## 8. RESUME 断点续传详解

```mermaid
sequenceDiagram
    participant User as 用户
    participant Runner as DynamicJobRunner
    participant Validator as ExecutionModeValidator
    participant Status as ExecutionStatusService
    participant Builder as DynamicJobBuilderService
    participant Loader as ExecutionContextLoader
    participant Flow as ResumeFlow

    User->>Runner: _mode=RESUME id=123
    Runner->>Validator: validate(RESUME, id=123)
    Validator->>Status: getJobExecution(123)
    Status-->>Validator: 历史执行 (Step2 FAILED)
    Validator->>Validator: 检查历史模式 = STANDARD ✓
    Validator-->>Runner: 校验通过
    
    Runner->>Builder: buildResumeJob(id=123)
    Builder->>Status: getStepsToResumeFrom()
    Status-->>Builder: [Step2, Step3, Step4]
    Builder->>Builder: 构建续传 Flow
    Builder->>Loader: 注入历史上下文
    
    Builder-->>Runner: Job (续传 Flow)
    Runner->>Flow: 执行
    Flow->>Flow: Step2 → Step3 → Step4
    Loader->>Flow: 加载历史 ExecutionContext
    Flow-->>User: COMPLETED
```

---

## 9. 条件流限制说明

```mermaid
graph TB
    subgraph ConditionalJob["条件流 Job"]
        C1["Step1"] -->|成功| C2["Step2"]
        C1 -->|失败| C3["Step3"]
    end

    subgraph Problem["SKIP_FAIL 问题"]
        P1["Step1 失败但被跳过"]
        P1 -->|?| P2["走 Step2?"]
        P1 -->|?| P3["走 Step3?"]
        PX["语义冲突!"]
    end

    subgraph Solution["解决方案"]
        S["条件流 Job 禁止 SKIP_FAIL"]
        S --> S1["使用 STANDARD"]
        S --> S2["使用 ISOLATED 手动指定"]
    end

    ConditionalJob -.-> Problem
    Problem -.-> Solution
```

---

## 快速参考

| 模式 | ID 规则 | 线性流 | 条件流 | 典型场景 |
|------|---------|--------|--------|---------|
| **STANDARD** | 不能带 | ✅ | ✅ | 常规执行 |
| **RESUME** | 必须带 | ✅ | ✅ | 失败后续传 |
| **SKIP_FAIL** | 不能带 | ✅ | ❌ | 容错推进 |
| **ISOLATED** | 可选 | ✅ | ✅ | 数据修复 |
