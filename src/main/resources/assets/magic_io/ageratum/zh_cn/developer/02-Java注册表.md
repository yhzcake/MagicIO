# 第 02 章：Java 注册表体系

## 1. 注册体系全景

MagicIO 同时使用 Minecraft 内建注册表和三个自定义注册表。注册表保存的是稳定的“类型定义”，方块实体中的物品、流体、进度等实例状态不应放入注册表。

| 类别 | 注册表/入口 | 主要内容 |
|---|---|---|
| 内建 | `MagicIO.BLOCKS` | 阵方块、网格面板 |
| 内建 | `MagicIO.ITEMS` | 普通物品、阵 BlockItem、总线 BlockItem |
| 内建 | `ModBlockEntities.BLOCK_ENTITIES` | 普通阵和网格面板方块实体类型 |
| 内建 | `ModZhenBusBlocks` | 阵总线方块与方块实体类型 |
| 内建 | `CREATIVE_MODE_TABS` | MagicIO 创造模式页 |
| 自定义 | `ElementType.ELEMENT_TYPE_REGISTRY_KEY` | 地、水、风、火等元素 |
| 自定义 | `IOType.IO_TYPE_REGISTRY_KEY` | item、fluid、energy |
| 自定义 | `ZhenType.ZHEN_TYPE_REGISTRY_KEY` | 所有等级展开后的阵类型 |

## 2. 自定义 Registry 的两阶段创建

三个类型类采用相同模式：

1. 使用 `ResourceKey.createRegistryKey` 声明注册表键。
2. 静态 `@SubscribeEvent` 方法监听 `NewRegistryEvent`。
3. 用 `RegistryBuilder` 创建 Registry 并设置默认键。
4. 调用 `event.register` 让 NeoForge 接管该 Registry。
5. 另一个 `DeferredRegister<T>` 向刚创建的 Registry 注册条目。

因此，`modEventBus.register(ElementType.class)`、`modEventBus.register(ZhenType.class)`、`modEventBus.register(IOType.class)` 不是注册条目，而是让创建 Registry 的静态监听器生效；`ElementTypes.register`、`ModIOTypes.register` 和 `ZhenTypes.register` 才负责条目。

## 3. DeferredRegister 的正确语义

`DeferredRegister` 在静态声明阶段只记录注册意图，真正对象由注册事件统一创建。开发时应保存 `Supplier<T>`、`DeferredHolder`、`DeferredBlock` 或 `DeferredItem`，不要保存静态初始化时强制取得的裸对象。

当前代码中的典型形式：

```java
public static final DeferredRegister<IOType> IO_TYPES =
        DeferredRegister.create(IOType.IO_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);
```

随后在 `register(IEventBus)` 中声明 `item`、`fluid`、`energy`，最后执行 `IO_TYPES.register(eventBus)`。该顺序保证供应器只在 Registry 已创建后求值。

## 4. 阵类型驱动方块注册

阵方块不是逐个手写注册。`ZhenFunctions.register` 先把每个功能按可用等级展开为多个 `ZhenType` 条目；`ModBlocks.registerZhenBlocks` 再遍历 `ZhenTypes.ZHEN_TYPES.getEntries()`，为每个条目创建同名 `ZhenBlock`；最后 `registerZhenBlockItems` 为这些方块生成同名 BlockItem。

数据流如下：

```text
ZhenFunction
  → 等级展开后的 ZhenType DeferredHolder
  → 同路径 ZhenBlock DeferredBlock
  → 同路径 BlockItem DeferredItem
  → 创造模式页遍历 ZHEN_BLOCK_ITEMS
```

这解释了为什么阵类型注册必须先于阵方块声明，也解释了为什么新增标准阵功能通常不需要逐个添加方块 Java 字段。

## 5. 方块实体类型

所有普通阵方块共享 `ModBlockEntities.ZHEN_BLOCK`。它的合法方块集合来自 `ModBlocks.getZhenBlockList()`，实例工厂统一为 `ZhenBlockEntity::new`。放置后，方块实体根据世界中的方块注册 ID 反查对应 `ZhenType`，而不是为每种阵创建一个方块实体类。

阵总线使用独立的 `ModZhenBusBlocks.ZHEN_BUS_BE`，因为其存储模型是“六个方向各一个 SideProcessor”，与普通阵单处理器模型不同。网格面板同样拥有独立方块实体类型。

## 6. 查找与回退

`ZhenTypes.getType` 支持完整 ID 和省略命名空间的路径。查找失败时优先回退到 `magic_io:unstable_sieve_zhen`，其次回退到 `GRID_CELL`。`ModIOTypes.getType` 则回退到物品类型。

回退可提高旧存档或不完整输入的容错性，但扩展代码不应依赖回退掩盖拼写错误。注册后验证时应比较实际 Registry ID；持久化数据应写完整类型 ID，避免跨命名空间歧义。

## 7. 新增普通物品或方块

### 7.1 普通物品

1. 在统一的 `MagicIO.ITEMS` 上声明 `DeferredItem`。
2. 若需要创造模式展示，将供应器加入页的 `displayItems`。
3. 添加语言键、物品模型及必要的数据组件。
4. 不要把注册拆到 `ModItems` 的第二个 DeferredRegister；当前 `ModItems` 仅是引用入口，权威注册器是 `MagicIO.ITEMS`。

### 7.2 普通方块

1. 在 `MagicIO.BLOCKS` 上注册方块。
2. 在 `MagicIO.ITEMS` 上注册对应 BlockItem。
3. 若有方块实体，在 `ModBlockEntities` 注册类型并把方块列入合法集合。
4. 在 `RegisterCapabilitiesEvent` 注册该方块实体的能力提供者。
5. 补齐方块状态、模型、战利品表和语言资源。

## 8. 新增自定义类型 Registry

若未来需要新的可扩展类别，应遵循现有三类注册表的结构：

1. 定义不可变类型对象及 Registry Key。
2. 监听 `NewRegistryEvent` 创建 Registry，选择合理默认键。
3. 建立专门的 `DeferredRegister<新类型>`。
4. 在公共入口先注册类型类监听器，再声明条目并挂接 DeferredRegister。
5. 序列化时保存 `Identifier`，不要保存 Java 类名或本地化名称。
6. 明确未知 ID 的行为：报错、跳过或回退，避免静默产生错误类型。

## 9. 常见陷阱

- 过早调用 `get()`：静态初始化时 Registry 尚未就绪。
- 重复注册器：同一对象散落在多个 DeferredRegister，初始化顺序难以维护。
- ID 不一致：阵类型、方块和 BlockItem 依靠同路径关联，任意一处额外后缀都会断开映射。
- 晚注册阵类型：阵方块列表已经从 DeferredRegister 条目生成后再添加，可能不会生成对应方块。
- 方块实体合法集合遗漏：方块存在但无法创建目标 BlockEntity。
- 注册表对象承载状态：类型是全局单例，不得写入某一方块的进度、库存或世界引用。

## 10. 验证路线

1. 启动时查看 `REGISTRIES` 调试日志是否存在重复键或缺失 Registry。
2. 检查 `ZhenType` 条目数量与 `ModBlocks.ZHEN_BLOCKS`、`ZHEN_BLOCK_ITEMS` 是否对应。
3. 放置新增方块，确认方块实体类型匹配。
4. 保存并重载世界，确认持久化 ID 能从 Registry 恢复。
5. 使用专用服务端验证公共注册代码没有客户端类加载问题。

## 11. 小结

MagicIO 的注册核心是“自定义类型先声明，具体阵按等级展开，再由类型集合派生方块和物品”。理解 DeferredRegister 的延迟求值和注册顺序，是扩展阵、IO 类型及方块实体的基础。
