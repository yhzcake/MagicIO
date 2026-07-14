---
title: "IO 与 Capability"
navigation:
  title: "第四章"
---

# 第 04 章：IO 与 Capability

## 1. 两层抽象

MagicIO 的资源交互分为内部 IO 层和 NeoForge Capability 层：

- 内部层以 `IOType`、`IOComponent`、`IOProcessor` 统一配方和阵总线的资源操作。
- 外部层以 NeoForge 的 Item、Fluid、Energy Block Capability 向管道或其他模组暴露受限视图。

Capability 不是存储本体。真正状态保存在 `ItemIOComponent`、`FluidIOComponent` 和 `EnergyIOComponent`，外部查询获得的 Handler 只是链接到这些状态的访问窗口。

## 2. IOType 与 IOProcessor

`IOType` 是自定义注册表对象，当前包含 item、fluid、energy。`IOProcessor` 以 `Map<IOType, IOComponent<?, ?>>` 聚合一个阵实例拥有的组件，并提供按类型查询、枚举和批量安装变更回调。

注册新组件时，同一 IOType 会覆盖旧组件，因此一个处理器每种类型只有一个权威组件。调用端从 `get` 取得泛型接口后通常仍需做具体类型检查，因为 Java 类型擦除不能证明 Registry 对象和组件实现的一一对应。

## 3. IOComponent 契约

`IOComponent<R,T>` 抽象资源需求 `R` 与实际值 `T`，定义：

- `canSupply`、`hasSupply`：检查能否满足需求；
- `canFit`：检查目标槽是否容纳输出；
- `extract`、`insert`：支持模拟和实际操作；
- `consume`、`produce`：配方提交阶段执行变更；
- `setChangeCallback`：通知宿主标脏并刷新输入；
- `saveNBT`、`loadNBT`：组件持久化。

任何新实现都必须保证模拟操作无副作用。“先模拟全部输入输出，再实际提交”是防止配方只消耗一半资源或只产出一半结果的关键事务边界。

## 4. 三种内建组件

### 4.1 ItemIOComponent

使用 `NonNullList<ItemStack>` 保存物品，并以 `SlotPartition` 判断输入输出区域。它还提供 NeoForge `ResourceHandler<ItemResource>`。物品比较、Ingredient 需求、堆叠上限和容器序列化均集中在此处，业务代码不应直接复制这些规则。

### 4.2 FluidIOComponent

使用 `NonNullList<FluidStack>` 和可空 tankCapacity。流体槽同样由分区区分输入与输出。总线方块实体持久化流体时额外保存 tank 索引，避免过滤空槽后丢失原槽位位置。

### 4.3 EnergyIOComponent

内部包装 `SimpleEnergyHandler`，容量、输入速率、输出速率和初始能量由构造参数决定。能量改变会触发组件回调。独立阵当前即使类型没有能量容量也会建立容量为 0 的组件，而总线 `AbstractSideProcessor` 只在容量非空时注册能量组件；扩展代码应意识到这一区别。

## 5. 面访问控制

`ZhenType` 先把“方向→IO 类型→区域名”解析为槽位索引。运行实例再把结果写入 `FaceAccessController`。外部 Capability 查询同时受三层限制：

1. 查询方向必须非空；
2. 该方向必须在类型的面访问表中暴露对应 IOType；
3. 暴露槽位还要与输入或输出槽集合求交集。

因此，面访问决定“从哪一面能看见哪些槽”，分区决定“这些槽能插入还是提取”。两者不能互相替代。

## 6. Linked Handler

`LinkedItemHandler` 与 `LinkedFluidHandler` 不复制资源，而是链接宿主的列表，并保存允许插入和允许提取的槽位集合。变更回调负责：

- 调用组件 `notifyChanged`；
- 标记方块实体已改变；
- 进而使配方输入缓存失效。

若直接把底层列表包装成不受限 Handler，就会绕过面和输入/输出权限。若返回资源副本，则管道写入不会落到真实存储。

## 7. Capability 注册

所有方块 Capability 在 `MagicIO.registerCapabilities` 中集中注册。

### 7.1 普通阵

- Item：读取指定方向的可插入槽与可提取槽，均为空时返回 `null`。
- Fluid：额外要求类型存在 tankCapacity，然后创建带容量和槽位限制的 LinkedFluidHandler。
- Energy：存在能量组件时返回其 Handler；当前路径没有按方向过滤，新增方向规则时需同步调整。

### 7.2 阵总线

先按查询方向取得该面的 `SideProcessor`，再读取处理器针对世界方向的访问表。物品和流体会与 `AbstractSideProcessor` 的输入/输出槽缓存求交集。能量通过通用 `registerZhenBusCap` 辅助逻辑取得对应组件并转换为外部 Handler。

安装、移除或替换总线处理器后必须调用 `level.invalidateCapabilities(pos)`，因为 NeoForge 可以缓存 Capability 查询结果；只调用 `setChanged` 不能使旧 Handler 失效。

## 8. 变更、持久化与同步

资源变化至少涉及三个概念：

- **持久化标脏**：`setChanged` 或宿主回调，保证区块保存状态。
- **配方缓存失效**：`inputsChanged = true`，让处理器重新匹配配方。
- **客户端同步**：`markForUpdate`、更新包或更新标签，用于碰撞箱、渲染和观察信息。

三者用途不同。仅标脏不会立即更新客户端，仅发包也不会保证存档保存。组件回调负责前两项，结构变化通常还需显式客户端同步和 Capability 失效。

## 9. 新增 IO 类型的完整路线

1. 注册新的 `IOType` 条目，确定稳定 ID 和本地化键。
2. 设计 `IOComponent<R,T>` 实现，明确空值、数量、相等性、模拟和序列化语义。
3. 在独立阵和 `AbstractSideProcessor` 的组件初始化中按类型需求注册组件。
4. 扩展 `SlotZone` 或复用可表达该资源输入/输出的区域，并让 `SlotPartition` 声明连续槽位。
5. 为配方输入输出解析和 `RecipeProcessor` 增加该资源类型处理。
6. 若 NeoForge 有对应 Capability，注册受方向和槽位约束的外部适配器。
7. 扩展 `VirtualPort.getAmount`、`withAmount` 及输出推送逻辑；当前只识别 ItemStack、FluidStack、Integer。
8. 补齐 NBT、更新包、Jade/JEI 展示和存档迁移策略。

只完成第 1、2 步并不能让新资源自动贯穿系统，尤其要检查虚拟端口中的运行时类型分派。

## 10. 调试方法

- 管道看不到能力：检查查询方向、面访问区域名和槽位交集。
- 能插入输出槽：检查 `SlotZone` 命名及 Linked Handler 的 insertSlots 构造。
- 配方不刷新：检查实际写入是否触发组件回调和 `inputsChanged`。
- 重载后流体错槽：检查序列化是否保留 tank 索引。
- 替换总线面后仍访问旧库存：检查是否调用 `invalidateCapabilities`。
- 模拟导致资源减少：检查组件 `simulate=true` 分支是否完全无副作用。
- 自动输出丢资源：检查 VirtualPort 返回值是否严格表示“未接受部分”。

## 11. 验证矩阵

每种 IO 至少验证：六个方向查询、空方向查询、允许/禁止插入、允许/禁止提取、模拟操作、满容量、部分接收、保存重载、处理器替换后的缓存失效、独立阵与阵总线的一致性。

## 12. 小结

内部 `IOComponent` 负责可靠资源语义，Capability 只负责把特定方向和槽位的受限视图暴露给外部。扩展时必须同时维护分区、面访问、事务模拟、变更回调、持久化、Capability 缓存和虚拟端口，不能只增加一个 Handler。
