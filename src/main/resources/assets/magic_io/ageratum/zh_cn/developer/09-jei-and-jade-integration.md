---
title: "JEI 与 Jade 集成"
navigation:
  title: "第九章"
---

# 第 09 章：JEI 与 Jade 集成

MagicIO 将 JEI 和 Jade 都声明为可选依赖。JEI 负责展示阵配方，Jade 负责观察阵总线各面的状态、物品和流体。两者都必须做到“存在时启用、缺失时不影响核心模组加载”。

## 1. 依赖与服务发现

`build.gradle` 使用 `compileOnly`：

```gradle
compileOnly("mezz.jei:jei-26.1.2-neoforge-api:29.5.0.28")
compileOnly("curse.maven:jade-324717:8281991")
```

这只让源码在编译时可见，不把完整模组打入 MagicIO，也不默认加入运行环境。模板元数据将 `jei` 与 `jade` 标为 `optional`，并给出当前期望版本范围。

服务文件分别为：

```text
META-INF/services/mezz.jei.api.IModPlugin
META-INF/services/snownee.jade.api.IWailaPlugin
```

它们指向 `MagicIOJeiPlugin` 和 `MagicIOJadePlugin`。重命名插件类或包时必须同步服务文件，否则代码能够编译，却不会被对应模组发现。

## 2. JEI 插件生命周期

`MagicIOJeiPlugin` 使用 `@JeiPlugin` 并实现 `IModPlugin`，主要阶段为：

1. `registerCategories`：为每种阵功能建立 `ZhenRecipeCategory`。
2. `registerRecipes`：优先从客户端配方缓存注册当前配方。
3. `registerRecipeCatalysts`：把各等级阵方块注册为工作站。
4. `onRuntimeAvailable`：保存 `IJeiRuntime`，处理网络与 JEI 初始化顺序不确定的问题。

插件按功能基础名建立 `IRecipeType<ZhenRecipe>`。不同等级的同功能阵共用一个 JEI 类别，与服务端 `ZhenRecipeManager` 的分组规则一致。

熔炉阵 `forge` 是特殊情况：它不建立独立阵类别，而是把方块注册为原版熔炼类别的工作站。其他功能映射到 MagicIO 自己的配方类型。

## 3. 类别与布局

`ZhenRecipeCategory` 负责：

- 根据功能寻找对应阵方块作为图标。
- 用翻译键构造类别标题。
- 把物品 Ingredient 放入输入槽。
- 把流体及容量放入流体槽。
- 展示固定物品输出。
- 解析战利品表，并把随机候选物放入滚动网格。

流体槽调用 `setFluidRenderer`，其容量参数取配方中的流体量。布局调整时应同时检查槽位坐标、类别宽高、滚动区域和提示框，避免可点击区域与绘制位置错位。

## 4. JEI 动态刷新

服务端同步后，`refreshFromCache`：

1. 获取 JEI RecipeManager。
2. 查询每个 MagicIO 类型的现有配方。
3. 隐藏旧配方。
4. 从最新缓存添加配方。

`registerFrom` 当前按基础名去重，每类只保留第一份符合条件的配方，而不是把该类所有配方都加入 JEI。它还跳过 `forge`，并过滤没有固定物品、战利品或流体输出的配方。因此“服务端有多份同类配方但 JEI 只显示一份”是当前源码行为，不一定是同步失败。

另一个边界是刷新采用“隐藏旧项再新增”，不是移除。频繁重载时应验证 JEI 内部是否积累隐藏对象，以及焦点查询是否仍然正确。

## 5. 战利品展示限制

`LootTableParser` 从客户端 classpath 读取 JSON，只实现常见 item/tag 与权重期望值。它不等同于服务端 LootTable 求值器。服务器数据包覆盖、复杂条件、函数和嵌套表可能无法准确展示。

教程和用户界面应把这些数字理解为提示性期望值，而不是保证掉落率。真实结果始终由服务端 `OutputEntry.roll` 决定。

## 6. Jade 公共端注册

`MagicIOJadePlugin.register` 注册三类服务端提供器：

- `ZhenBusServerProvider`：发送各面的阵类型、处理时间和物品摘要。
- `ZhenBusItemProvider`：向 Jade 提供物品存储视图。
- `ZhenBusFluidProvider`：向 Jade 提供流体存储视图。

`ZhenBusServerProvider.shouldRequestData` 只在目标方块实体是 `ZhenBusBlockEntity` 时请求数据。`appendServerData` 遍历六个方向，把每个存在的 `SideProcessor` 编码到 `CompoundTag`。

服务端只应发送界面真正需要的数据。阵总线可能连接大量资源，若把完整存储每帧传给客户端，会造成明显网络和 NBT 分配压力。

## 7. Jade 客户端注册

`registerClient` 注册：

- `ZhenBusClientProvider`：渲染方块主体信息。
- `ZhenBusItemClientProvider`：渲染物品存储。
- `ZhenBusFluidClientProvider`：渲染流体存储。

这是典型的“服务端采集权威数据、客户端负责格式化与绘制”结构。客户端提供器不能直接读取远端服务端方块实体的完整状态，也不应自行推断机器配方进度。

## 8. 可选依赖隔离

兼容代码应集中在 `compat.jei` 和 `compat.jade` 包。核心逻辑不要在静态字段、公共签名或类初始化中直接引用可选模组类型，否则即使插件未被服务发现，JVM 仍可能在缺少依赖时解析失败。

建议检查以下场景：

| 环境 | 预期结果 |
| --- | --- |
| 不安装 JEI/Jade | MagicIO 核心功能正常，兼容插件不加载 |
| 仅安装 JEI | 配方类别和同步刷新正常，Jade 类不参与运行 |
| 仅安装 Jade | 阵总线观察信息正常，JEI 类不参与运行 |
| 两者都安装 | 两套插件独立工作 |
| 专用服务器 | 不加载纯客户端渲染类，公共端提供器可按 Jade 机制工作 |

## 9. 本地验证步骤

1. 检查构建文件中的 API 版本与运行时模组版本一致。
2. 检查两个 `META-INF/services` 文件内容与插件全限定类名一致。
3. 启动后确认 JEI 类别注册数量日志。
4. 进入世界后确认客户端收到服务端配方缓存。
5. 执行 `/reload`，确认 JEI 内容随缓存刷新。
6. 查看阵总线六个面，确认 Jade 显示与实际 SideProcessor 对应。
7. 分别在缺少 JEI、缺少 Jade 和两者都缺少的组合下验证核心模组启动。
8. 用专用服务器验证不存在客户端类加载异常。

## 10. 常见问题

- **插件完全不出现**：优先检查服务文件、可选模组版本和插件注解。
- **JEI 有类别无配方**：检查客户端缓存是否收到、`registerFrom` 是否过滤了该配方。
- **同类配方只显示一份**：当前按基础名 `putIfAbsent` 去重，是既有实现。
- **随机产物显示不全**：检查战利品表是否超出轻量解析器能力。
- **Jade 数据为空**：检查目标是否为 `ZhenBusBlockEntity`、对应方向是否存在处理器。
- **只在服务器崩溃**：检查核心类是否直接引用 JEI/Jade 客户端 API。
