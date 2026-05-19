
# 通用 IO 系统重构规划

## 1. 设计背景

### 现状问题

当前代码中，输入输出处理逻辑存在以下问题：

1. **紧耦合** — 所有 I/O 操作（`insertItem`、`extractItem`、`fillTank`、`drainTank` 等）
   全部实现在 `AbstractZhenBlockEntity` 中，与方块实体高度耦合，无法独立复用。

2. **扩展困难** — 要添加新的 I/O 类型（如能量 `Energy`、经验 `Experience`），需要同步修改：
   - `SlotZone` — 添加 Zone 常量
   - `SlotPartition` — 添加验证和查询
   - `AbstractZhenBlockEntity` — 添加大量重复方法
   - `ZhenRecipe` — 添加字段和匹配逻辑
   - `ZhenRecipeSerializer` — 添加序列化逻辑
   - `ZhenRecipeLoader` — 添加 JSON 解析逻辑

3. **职责不清晰** — `AbstractZhenBlockEntity` 同时承担：容器管理、面访问控制、I/O 操作、配方处理、Tick 调度，违反单一职责原则。

4. **硬编码流水线** — `serverTick` 中的处理流程是硬编码的，不同 I/O 类型的逻辑交织在一起。

### 重构目标

```
改造前：
  SlotZone(name, isItemInput, isItemOutput, isLiquitInput, isLiquitOutput)
  ↑ 把"类型"和"区域"混在一起

改造后：
  IOType(name)                 → 用 Registry 注册，描述"这是什么类型的数据"
  SlotZone(name)               → 纯命名区域，描述"这是哪个区域"
  SlotPartition → IOType × SlotZone × [slot索引]
  ↑ "类型"和"区域"解耦，任意组合
```

---

## 2. 设计决策

| 决策项 | 选择 | 原因 |
|--------|------|------|
| **IOType 注册方式** | 🏛️ NeoForge Registry | 与 `ElementType` 风格一致，支持数据驱动 |
| **SlotZone 定位** | 🏷️ 纯命名区域 | 只表示"区域名"，不再区分 item/fluid |
| **IOType ↔ SlotZone 关系** | 🔗 多对多关联 | 一个区域可以有多种 I/O 类型 |

---

## 3. 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                   IOProcessor (处理器)                    │
│  统一入口：read() / write() / canRead() / canWrite()     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│   ┌──────────┐  ┌───────────┐  ┌────────────┐          │
│   │ ItemIO   │  │ FluidIO   │  │ EnergyIO   │  ...     │
│   │ Component│  │ Component │  │ Component  │          │
│   └────┬─────┘  └─────┬─────┘  └─────┬──────┘          │
│        │              │               │                  │
│   ┌────▼─────┐  ┌─────▼─────┐  ┌─────▼──────┐          │
│   │ 物品槽位  │  │ 流体槽位   │  │ 能量存储   │  ...     │
│   └──────────┘  └───────────┘  └────────────┘          │
│                                                          │
├─────────────────────────────────────────────────────────┤
│                   RecipeIO (配方I/O抽象)                  │
│  统一描述：输入需求列表 / 输出规格列表                     │
└─────────────────────────────────────────────────────────┘
```

---

## 4. 阶段与待办清单

### 🔴 P0 — 核心基础设施（3 项）

定义 I/O 系统的"语言"和"契约"，不依赖任何现有代码变更。

| 待办 | 文件 | 内容 | 风险 |
|------|------|------|------|
| P0-1 | 新建 `io/IOType.java` | 基于 NeoForge Registry 的输入输出类型注册表（类似 ElementType） | 🟢 |
| P0-2 | 新建 `io/ModIOTypes.java` | 注册内置 IOType（ITEM、FLUID），预留扩展点 | 🟢 |
| P0-3 | 新建 `io/IOComponent.java` | 定义泛型接口：extract、insert、canFit、matches、slotCount | 🟢 |

---

### 🟡 P1 — 组件提取与 SlotZone 重构（4 项）

**影响最大的阶段**，SlotZone 的重构会改变整个区域系统的语义。

| 待办 | 文件 | 内容 | 风险 |
|------|------|------|------|
| P1-1 | 修改 `SlotZone.java` | 从 `SlotZone(name, isItemInput, isItemOutput, isLiquitInput, isLiquitOutput)` → `SlotZone(name)`，移除 4 个布尔字段 + 5 个 isXxx 方法 | 🟡 |
| P1-2 | 修改 `SlotPartition.java` | 从按 item/fluid 两套查询，改为按 `IOType + SlotZone` 的多维映射 | 🟡 |
| P1-3 | 新建 `io/ItemIOComponent.java` | 从 AbstractZhenBlockEntity 迁移：insertItem、extractItem、canFitOutput、canConsume、consumeInputs | 🟢 |
| P1-4 | 新建 `io/FluidIOComponent.java` | 从 AbstractZhenBlockEntity 迁移：fillTank、drainTank、canFillFluidOutput、consumeFluidInputs | 🟢 |

---

### 🟠 P2 — IOProcessor 与 BlockEntity 重构（3 项）

**系统的核心枢纽**，将之前的组件整合为统一入口。

| 待办 | 文件 | 内容 | 风险 |
|------|------|------|------|
| P2-1 | 新建 `io/IOProcessor.java` | 管理所有 IOComponent，提供统一 API：extract(IOType)、insert(IOType)、canProcess(RecipeIO) | 🟡 |
| P2-2 | 修改 `AbstractZhenBlockEntity.java` | 内部使用 IOProcessor，对外保留旧方法为委托，面访问控制与 IOType 关联，清理大量重复 I/O 方法 | 🟡 |
| P2-3 | 修改 `AbstractZhenBlockEntity.java` | 将 serverTick 中硬编码的 item/fluid 处理改为遍历 IOProcessor 的通用流程 | 🟡 |

---

### 🔵 P3 — 配方系统解耦（5 项）

将配方的 I/O 部分抽象化，使配方支持任意 I/O 组合。

| 待办 | 文件 | 内容 | 风险 |
|------|------|------|------|
| P3-1 | 新建 `crafting/RecipeIO.java` | RecipeInput<T>(IOType, zoneName, requirement) / RecipeOutput<T>(IOType, zoneName, specification) | 🟡 |
| P3-2 | 修改 `ZhenRecipe.java` | 用 List<RecipeInput<?>> + List<RecipeOutput<?>> 替代分立的 item/fluid 字段 | 🔴 |
| P3-3 | 修改 `ZhenRecipeSerializer.java` | Codec 和 StreamCodec 改为按 IOType 分发的可扩展模式 | 🔴 |
| P3-4 | 修改 `ZhenRecipeLoader.java` | JSON 解析改为通用的 I/O 需求解析 | 🔴 |
| P3-5 | 修改 `ZhenType.java`、`ZhenTypes.java`、`SmallSiftMethod.java` | 更新区域定义 | 🟡 |

---

### 🟢 P4 — 扩展示例（3 项）

展示扩展能力，同时也是对新系统的验证。

| 待办 | 文件 | 内容 | 风险 |
|------|------|------|------|
| P4-1 | 新建 `io/EnergyIOComponent.java`、修改 `ModIOTypes.java` | 完整展示三步扩展法：注册 IOType → 实现 Component → 配置区域 | 🟢 |
| P4-2 | 修改 `SlotPartition.java` | 确保新 I/O 类型也能享受自动分区验证 | 🟢 |
| P4-3 | 相关文件 | 说明如何新增一个 I/O 类型（三步：注册 IOType → 实现 IOComponent → 配置 SlotPartition） | 🟢 |

---

## 5. 文件影响矩阵

| 待办 | 新建文件 | 修改文件 | 删除内容 |
|------|---------|---------|---------|
| P0-1 | `io/IOType.java` | — | — |
| P0-2 | `io/ModIOTypes.java` | — | — |
| P0-3 | `io/IOComponent.java` | — | — |
| P1-1 | — | `SlotZone.java` | 4 个布尔字段 + 5 个 isXxx 方法 |
| P1-2 | — | `SlotPartition.java` | 按 item/fluid 的分类映射 |
| P1-3 | `io/ItemIOComponent.java` | — | — |
| P1-4 | `io/FluidIOComponent.java` | — | — |
| P2-1 | `io/IOProcessor.java` | — | — |
| P2-2 | — | `AbstractZhenBlockEntity.java` | 大量重复 I/O 方法 |
| P2-3 | — | `AbstractZhenBlockEntity.java` | serverTick 中的硬编码逻辑 |
| P3-1 | `crafting/RecipeIO.java` | — | — |
| P3-2 | — | `ZhenRecipe.java` | 分立的 item/fluid 字段 `zoneInputs`、`zoneOutputs`、`fluidInputs`、`fluidOutputs` |
| P3-3 | — | `ZhenRecipeSerializer.java` | 手写的 StreamCodec 序列化/反序列化逻辑 |
| P3-4 | — | `ZhenRecipeLoader.java` | JSON 解析中的硬编码 item/fluid 分支 |
| P3-5 | — | `ZhenType.java`、`ZhenTypes.java`、`SmallSiftMethod.java` | 对旧 SlotZone 布尔字段的引用 |
| P4-1 | `io/EnergyIOComponent.java` | `ModIOTypes.java` | — |
| P4-2 | — | `SlotPartition.java` | — |
| P4-3 | — | 相关文件 | — |

### 汇总

| 指标 | 数量 |
|------|------|
| **新建文件** | 9 个（IOType、ModIOTypes、IOComponent、ItemIOComponent、FluidIOComponent、IOProcessor、RecipeIO、EnergyIOComponent） |
| **修改文件** | 9 个（SlotZone、SlotPartition、AbstractZhenBlockEntity×2、ZhenRecipe、ZhenRecipeSerializer、ZhenRecipeLoader、ZhenType/ZhenTypes/SmallSiftMethod） |
| **风险等级分布** | 🟢 低：9 项 / 🟡 中：6 项 / 🔴 高：3 项 |

---

## 6. 审查中发现的遗漏缺口（7 项）

### 🔴 关键遗漏

| 缺口 | 描述 | 影响 | 修正方案 |
|------|------|------|---------|
| 缺口-1 | **IOComponent 缺少变更通知回调** — `consumeInputs` 需要触发 `setChanged()` + `inputsChanged=true`，但 IOComponent 不应直接耦合 BlockEntity | 消费操作后无法触发重检 | IOComponent 增加 `setChangeCallback(Runnable)` |
| 缺口-2 | **DROP_OUTPUT 未被建模为独立 IOComponent** — 当前在 serverTick 中硬编码处理世界投掷，不走库存 I/O | 新增 I/O 类型无法区分"世界输出"与"库存输出" | 新增 `IOType.WORLD_DROP` + `WorldDropIOComponent` |
| 缺口-3 | **面访问控制未纳入 IOType 维度** — 当前 item/fluid 两套独立 Map，新 I/O 类型无法被外部访问 | 能量等新类型无法通过漏斗/管道交互 | 统一为 `Map<Direction, Map<IOType, Set<Integer>>>` |
| 缺口-4 | **流体 NeoForge Capability 直接依赖 BlockEntity 方法** — `registerCapabilities` 调用 `getTankCapacity()` / `getTanks()`，不走 IOProcessor | 重构后流体管道失效 | Capability 注册改为通过 IOProcessor 获取 |
| 缺口-5 | **NBT 序列化未委托给 IOComponent** — `saveAdditional`/`loadAdditional` 中硬编码 item/fluid 序列化 | 新 I/O 类型无法持久化 | `IOComponent` 增加 `saveNBT(ValueOutput)` / `loadNBT(ValueInput)` |

### 🟡 已有 Bug

| 缺口 | 描述 | 影响 | 修正方案 |
|------|------|------|---------|
| 缺口-6 | **配方 JSON 区域名称不匹配** — `small_sift_zhen_metal.json` 使用 `"input_all"` 但 SlotZone 定义为 `"item_input_all"`，匹配永远失败 | 该配方无法工作 | 统一区域命名规范，或解析时自动映射 |
| 缺口-7 | **配方不支持 `/reload` 重载** — `onServerStarting` 只加载一次，数据包重载不触发 | 开发时修改配方需要重启服务端 | 改用 `AddServerReloadListenerEvent` |

---

## 7. AbstractZhenBlockEntity 非 I/O 部分问题（11 项）

### 🔴 可能导致崩溃

| # | 问题 | 位置 |
|---|------|------|
| 1 | **未检查类型转换** — `(ZhenRecipe) lastRecipe.value()` 如果 `lastRecipe` 被设为非 ZhenRecipe 类型会 ClassCastException | `AbstractZhenBlockEntity` L622 |
| 2 | **`createMenu()` 抛出 UnsupportedOperationException** — 任何打开 GUI 的路径都会崩溃 | `AbstractZhenBlockEntity` L847 |

### 🟡 功能缺陷

| # | 问题 | 位置 |
|---|------|------|
| 3 | **配方处理期间不重新验证输入** — `currentRecipe` 设定后不再检查输入是否被外部取走 | L637-L654 |
| 4 | **输出满时无退避** — 每 tick 重试匹配同一个配方，无冷却 | L670-L693 |
| 5 | **输出满时进度归零** — `canFitZoneOutputs` 失败后 `processTime` 直接归零，已投入时间浪费 | L690 |
| 6 | **`fillStackedContents` 包含输出物品** — 输出槽物品被当作可用材料，干扰配方书 | L838 |
| 7 | **`quickCheck` 缓存未使用** — 每次 tick 遍历所有配方，未用 CachedCheck | L66-L67 |
| 8 | **网络同步不完整** — `getUpdateTag` 只同步 `processTime`，不同步配方和物品 | L815-L819 |

### 🟢 代码质量

| # | 问题 | 位置 |
|---|------|------|
| 9 | **`autoInput` / `autoOutput` 从未使用** — 死代码 | L73-L74 |
| 10 | **`@SuppressWarnings("unused")` 掩盖未使用方法** | L58 |
| 11 | **`getZoneForSlot()` 对无效槽位抛异常而非返回空** — 调用方失误即崩溃 | `SlotPartition` L95-L102 |

---

## 8. 向后兼容策略

| 方面 | 策略 |
|------|------|
| **对外接口（API）** | 保留旧方法为委托，确保外部调用不受影响 |
| **配方 JSON 格式** | 解析器兼容新旧两种格式，逐步迁移 |
| **存档数据（NBT）** | `saveAdditional` / `loadAdditional` 格式不变 |
| **网络同步数据** | StreamCodec 兼容旧格式 |
| **依赖 SlotZone 表达式引用的代码** | 同步更新所有引用 |

---

## 9. 风险与应对

| 风险 | 阶段 | 影响 | 应对措施 |
|------|------|------|---------|
| SlotZone 重构导致多处编译错误 | P1 | 需同步修改 10+ 文件 | 先建分支，统一修改再测试 |
| tick 逻辑重构不等价 | P2 | 运行时行为异常 | 分步替换：先加委托，测试通过再删除旧逻辑 |
| 配方序列化格式不兼容 | P3 | 网络协议不匹配报错 | 递增网络协议版本号 |
| JSON 配方格式变更 | P3 | 旧配方文件无法加载 | 解析器兼容旧格式，支持自动迁移 |

---

## 10. 实时待办追踪

> 最后更新：2026-05-18
> 状态说明：⏳ pending = 待开始 · 🔄 in_progress = 进行中 · ✅ completed = 已完成

### 🔴 P0 — 核心基础设施（3 项）

| 待办 | 内容 | 状态 |
|------|------|:----:|
| P0-1 | 创建 IOType Registry 系统 — 基于 NeoForge Registry 的输入输出类型注册表（类似 ElementType） | ⏳ pending |
| P0-2 | 创建 ModIOTypes — 注册内置 IOType（ITEM、FLUID），预留扩展点 | ⏳ pending |
| P0-3 | 创建 IOComponent\<T\> 接口 — 定义通用的 I/O 操作契约（extract/insert/canFit/matches） | ⏳ pending |

### 🟡 P1 — 组件提取与 SlotZone 重构（4 项）

| 待办 | 内容 | 状态 |
|------|------|:----:|
| P1-1 | 重构 SlotZone — 移除 item/fluid 布尔字段，简化为纯命名区域 | ⏳ pending |
| P1-2 | 重构 SlotPartition — 适配纯命名 SlotZone，从按 item/fluid 分区改为按 IOType + SlotZone 分区 | ⏳ pending |
| P1-3 | 创建 ItemIOComponent — 从 AbstractZhenBlockEntity 提取物品 I/O 逻辑（insertItem/extractItem/canFitOutput/canConsume/consumeInputs） | ⏳ pending |
| P1-4 | 创建 FluidIOComponent — 从 AbstractZhenBlockEntity 提取流体 I/O 逻辑（fillTank/drainTank/canFillFluidOutput/consumeFluidInputs） | ⏳ pending |

### 🟠 P2 — IOProcessor 与 BlockEntity 重构（3 项）

| 待办 | 内容 | 状态 |
|------|------|:----:|
| P2-1 | 创建 IOProcessor — 统一 I/O 处理器，管理所有 IOComponent，提供统一 API | ⏳ pending |
| P2-2 | 重构 AbstractZhenBlockEntity — 将 I/O 方法委托给 IOProcessor，清理冗余方法，面访问控制与 IOType 关联 | ⏳ pending |
| P2-3 | 重构 serverTick 流水线 — 基于 IOProcessor 的通用配方处理流程（查找→预检→消耗→产出） | ⏳ pending |

### 🔵 P3 — 配方系统解耦（5 项）

| 待办 | 内容 | 状态 |
|------|------|:----:|
| P3-1 | 创建 RecipeIO 抽象 — 将配方 I/O 需求从具体类型中解耦（RecipeInput\<T\> / RecipeOutput\<T\>） | ⏳ pending |
| P3-2 | 重构 ZhenRecipe — 使用统一的 List\<RecipeInput\<?\>\> / List\<RecipeOutput\<?\>\> 替代分立的 item/fluid 字段 | ⏳ pending |
| P3-3 | 重构 ZhenRecipeSerializer — 适配新的通用配方结构，Codec / StreamCodec 改为按 IOType 分发 | ⏳ pending |
| P3-4 | 重构 ZhenRecipeLoader — 适配新的通用配方序列化格式 | ⏳ pending |
| P3-5 | 适配 ZhenType / SmallSiftMethod — 更新区域定义和 tick 工厂方法 | ⏳ pending |

### 🟢 P4 — 扩展示例（3 项）

| 待办 | 内容 | 状态 |
|------|------|:----:|
| P4-1 | 示例：创建 EnergyIOComponent — 能量 I/O 类型，展示如何按需扩展（含注册 IOType + 实现 Component + 区域配置） | ⏳ pending |
| P4-2 | 示例：扩展 SlotPartition 验证 — 确保新 I/O 类型也能享受分区验证和错误提示 | ⏳ pending |
| P4-3 | 编写扩展文档/注释 — 说明如何新增一个 I/O 类型（三步：注册 IOType → 实现 IOComponent → 配置 SlotPartition） | ⏳ pending |

### 🔴 P5 — 审查遗漏补充（7 项）

| 待办 | 内容 | 状态 |
|------|------|:----:|
| P5-1 | IOComponent 增加变更通知回调 `setChangeCallback(Runnable)` — 消费操作后触发 setChanged + inputsChanged | ⏳ pending |
| P5-2 | 新增 `IOType.WORLD_DROP` + `WorldDropIOComponent` — 世界投掷输出独立建模 | ⏳ pending |
| P5-3 | 面访问控制统一为 `Map<Direction, Map<IOType, Set<Integer>>>` — 支持任意 IOType 的外部访问 | ⏳ pending |
| P5-4 | 流体 NeoForge Capability 改为通过 IOProcessor 获取 — 适配解耦后的架构 | ⏳ pending |
| P5-5 | IOComponent 增加 `saveNBT(ValueOutput)` / `loadNBT(ValueInput)` — 各类型自管理持久化 | ⏳ pending |
| P5-6 | 修复配方 JSON 区域名称不匹配 Bug — `small_sift_zhen_metal.json` 使用错误名称无法匹配 | ⏳ pending |
| P5-7 | 配方加载改用 `AddServerReloadListenerEvent` — 支持 `/reload` 热重载 | ⏳ pending |

### 🟣 P6 — AbstractZhenBlockEntity 非 I/O 修复（11 项）

| 待办 | 内容 | 状态 |
|------|------|:----:|
| P6-1 | 修复 `(ZhenRecipe) lastRecipe.value()` 未检查类型转换 — 加入 instanceof 检查 | ⏳ pending |
| P6-2 | 实现 `createMenu()` — 防止 UnsupportedOperationException 崩溃 | ⏳ pending |
| P6-3 | 配方处理期间增加输入有效性验证 — 每几 tick 或每次处理前验证一次 | ⏳ pending |
| P6-4 | 输出满时增加退避冷却 — 避免每 tick 无限重试同一配方 | ⏳ pending |
| P6-5 | 输出满时保留进度 — `canFitZoneOutputs` 失败时不归零 processTime | ⏳ pending |
| P6-6 | 修复 `fillStackedContents` 仅包含输入槽物品 — 排除输出槽 | ⏳ pending |
| P6-7 | 启用 `quickCheck` 缓存加速配方查找 — 使用已有的 CachedCheck | ⏳ pending |
| P6-8 | 完善网络同步 — `getUpdateTag` 同步当前配方和物品内容 | ⏳ pending |
| P6-9 | 清理 `autoInput` / `autoOutput` 死代码 — 移除或实现 | ⏳ pending |
| P6-10 | 清理 `@SuppressWarnings("unused")` — 明确保留/移除意图 | ⏳ pending |
| P6-11 | 修复 `getZoneForSlot` 对无效槽位抛异常 — 改为返回 empty Set | ⏳ pending |
