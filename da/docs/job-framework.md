# SimuKraft 职业框架文档

## 目录结构

### 整体架构

```
job/
├── api/                    # 接口定义层（抽象层）
├── core/                   # 核心服务层（实现层）
├── jobs/                   # 职业实现层（具体职业）
└── ModJobs.java            # 职业注册入口
```

### 目录职责说明

| 目录 | 职责 | 特性 |
|------|------|------|
| `api/` | 定义职业系统的核心接口 | 稳定，不包含实现逻辑 |
| `core/` | 提供核心服务和基础实现 | 可复用的通用逻辑 |
| `jobs/` | 存放各职业的具体实现 | 业务逻辑隔离 |

---

## 核心接口说明

### 1. JobDefinition（职业定义）

```java
public interface JobDefinition {
    JobType type();                    // 职业类型标识
    Component displayName();           // 显示名称
    JobSchedule schedule();            // 工作时间表
    JobWorkflow workflow();            // 工作流程
    WorkTargetResolver<?> targetResolver();  // 工作目标解析器
    default RestWorkflow restWorkflow() { ... }  // 休息流程（默认实现）
    default JobPolicy policy() { ... }           // 职业策略（默认实现）
}
```

**设计意图**：定义职业的基本属性和行为契约，所有职业必须实现此接口。

### 2. JobWorkflow（工作流程）

```java
public interface JobWorkflow {
    boolean canWork(JobContext context);   // 是否可以开始工作
    void onStartWork(JobContext context);  // 开始工作回调
    void onStopWork(JobContext context);   // 停止工作回调
    JobResult tick(JobContext context);    // 每 Tick 执行
}
```

**设计意图**：封装职业的工作逻辑，实现工作状态的生命周期管理。

### 3. WorkTargetResolver（工作目标解析器）

```java
public interface WorkTargetResolver<T extends WorkTarget> {
    Optional<T> resolve(JobContext context);  // 根据上下文解析工作目标
}
```

**设计意图**：将工作目标的解析逻辑抽象出来，实现关注点分离。

### 4. WorkTarget（工作目标）

```java
public interface WorkTarget {
    boolean isValid();  // 验证目标是否有效
}
```

**设计意图**：定义工作目标的有效性检查契约。

### 5. JobContext（工作上下文）

```java
public interface JobContext {
    ServerLevel level();          // 当前世界
    CustomEntity npc();           // 执行工作的 NPC
    EmploymentAssignment assignment();  // 就业分配信息
    long gameTime();              // 当前游戏时间
}
```

**设计意图**：封装工作执行所需的上下文信息，提供统一访问入口。

### 6. JobPolicy（职业策略）

```java
public record JobPolicy(
    boolean allowRest,        // 是否允许休息
    boolean requireWorkplace, // 是否需要工作场所
    boolean allowRemoteWork,  // 是否允许远程工作
    int minLevel,             // 最低等级要求
    int maxDistance           // 最大工作距离
) {}
```

**设计意图**：配置职业的行为约束和限制条件。

### 7. JobSchedule（工作时间表）

```java
public record JobSchedule(long minDelay, long maxDelay) {}
```

**设计意图**：定义工作执行的时间间隔范围。

### 8. JobResult（工作结果）

```java
public sealed interface JobResult {
    static JobResult success() { ... }      // 工作成功
    static JobResult paused(String reason) { ... }  // 工作暂停
    static JobResult failed(String reason) { ... }  // 工作失败
    
    JobResultType type();
    String reason();
}
```

**设计意图**：表示单次工作执行的结果状态。

---

## 核心服务说明

### 1. JobRegistry（职业注册表）

```java
public final class JobRegistry {
    void register(JobDefinition definition);  // 注册职业
    Optional<JobDefinition> get(JobType type);  // 获取职业定义
    Collection<JobDefinition> getAll();      // 获取所有职业
}
```

**职责**：管理所有职业的注册和查询。

### 2. JobRuntimeService（职业运行时服务）

```java
public final class JobRuntimeService {
    void tick(ServerLevel level);            // 每 Tick 更新
    void startWork(CustomEntity npc);        // 开始工作
    void stopWork(CustomEntity npc);         // 停止工作
}
```

**职责**：协调职业的运行时执行和状态管理。

### 3. JobStateMachine（状态机）

```java
public final class JobStateMachine {
    void transition(CustomEntity npc, JobState state);  // 状态转换
    JobState getState(CustomEntity npc);                // 获取当前状态
}
```

**职责**：管理 NPC 的工作状态流转。

---

## 创建新职业指南

### 步骤 1：创建职业定义类

```java
public final class MyJobDefinition implements JobDefinition {
    private final MyTargetResolver targetResolver = new MyTargetResolver();
    private final MyWorkflow workflow = new MyWorkflow(targetResolver);

    @Override
    public JobType type() {
        return JobType.MY_JOB;  // 需要在 JobType 中定义
    }

    @Override
    public Component displayName() {
        return Component.translatable("job.simukraft.my_job");
    }

    @Override
    public JobSchedule schedule() {
        return new JobSchedule(2000, 5000);  // 2-5 秒执行一次
    }

    @Override
    public JobWorkflow workflow() {
        return workflow;
    }

    @Override
    public WorkTargetResolver<?> targetResolver() {
        return targetResolver;
    }

    // 可选：覆盖默认策略
    @Override
    public JobPolicy policy() {
        return new JobPolicy(true, true, false, 1, 10);
    }
}
```

### 步骤 2：创建工作目标类

```java
public record MyWorkTarget(
    ServerLevel level,
    BlockPos mainPos,
    // 添加自定义字段
) implements WorkTarget {
    @Override
    public boolean isValid() {
        return level != null && mainPos != null;
    }
}
```

### 步骤 3：创建目标解析器

```java
public final class MyTargetResolver implements WorkTargetResolver<MyWorkTarget> {
    @Override
    public Optional<MyWorkTarget> resolve(JobContext context) {
        var assignment = context.assignment();
        var level = context.level();
        if (assignment == null || assignment.workplacePos() == null || level == null) {
            return Optional.empty();
        }
        // 解析自定义目标逻辑
        return Optional.of(new MyWorkTarget(level, assignment.workplacePos()));
    }
}
```

### 步骤 4：创建工作流程

```java
public final class MyWorkflow implements JobWorkflow {
    private final MyTargetResolver targetResolver;

    public MyWorkflow(MyTargetResolver targetResolver) {
        this.targetResolver = targetResolver;
    }

    @Override
    public boolean canWork(JobContext context) {
        return targetResolver.resolve(context)
                .map(MyWorkTarget::isValid)
                .orElse(false);
    }

    @Override
    public void onStartWork(JobContext context) {
        CustomEntity npc = context.npc();
        if (npc != null) {
            npc.setJob("my_job");
            // 初始化工作状态
        }
    }

    @Override
    public void onStopWork(JobContext context) {
        // 清理工作状态
    }

    @Override
    public JobResult tick(JobContext context) {
        return targetResolver.resolve(context)
                .filter(MyWorkTarget::isValid)
                .map(target -> {
                    // 执行具体工作逻辑
                    return JobResult.success();
                })
                .orElseGet(() -> JobResult.paused("missing_target"));
    }
}
```

### 步骤 5：注册职业

在 `ModJobs.java` 的 `registerJobs()` 方法中添加：

```java
JobRegistry.INSTANCE.register(new MyJobDefinition());
```

---

## 框架工作流程

### 完整执行链路

```
Server Tick
     │
     ▼
JobRuntimeService.tick()
     │
     ├─► 获取所有有工作的 NPC
     │
     ├─► 遍历每个 NPC
     │     │
     │     ├─► 检查职业定义
     │     │
     │     ├─► 获取 JobContext
     │     │
     │     ├─► 调用 JobWorkflow.canWork()
     │     │
     │     ├─► 状态检查 (JobStateMachine)
     │     │
     │     └─► 调用 JobWorkflow.tick()
     │           │
     │           ├─► WorkTargetResolver.resolve()
     │           │
     │           └─► 执行具体工作逻辑
     │
     ▼
返回 JobResult
```

### 状态流转

```
IDLE ──startWork()──► WORKING
  ▲                       │
  │                       │ tick() 返回 paused
  │                       ▼
  └────────────────── PAUSED
            │
            │ tick() 返回 failed
            ▼
        FAILED
```

---

## 现有职业列表

| 职业 | 包路径 | 职责 |
|------|--------|------|
| Farmer | `jobs.farmer` | 农业生产 |
| Builder | `jobs.builder` | 建筑建造 |
| Planner | `jobs.planner` | 规划设计 |
| WarehouseManager | `jobs.warehousemanager` | 仓库管理 |
| CommercialGeneric | `jobs.commercialgeneric` | 商业通用 |
| IndustrialGeneric | `jobs.industrialgeneric` | 工业通用 |

---

## 扩展建议

### 最佳实践

1. **职责单一**：每个类只负责一件事
2. **依赖抽象**：依赖接口而非具体实现
3. **可测试性**：设计易于单元测试的代码结构
4. **命名规范**：
   - 职业定义：`{JobName}JobDefinition`
   - 工作流程：`{JobName}Workflow`
   - 目标解析器：`{JobName}TargetResolver`
   - 工作目标：`{JobName}WorkTarget`

### 常见扩展场景

- **添加新职业**：按照「创建新职业指南」步骤实现
- **扩展现有职业**：继承或组合现有实现
- **自定义策略**：覆盖 `JobDefinition.policy()` 方法
- **自定义休息行为**：覆盖 `JobDefinition.restWorkflow()` 方法

---

## 版本兼容性

本框架遵循向后兼容原则：
- 接口变更使用默认方法保持兼容
- 新增接口不影响现有实现
- 核心服务保持稳定的 API