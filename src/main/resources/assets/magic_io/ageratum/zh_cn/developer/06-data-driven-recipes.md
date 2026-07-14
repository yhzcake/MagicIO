---
title: "数据驱动配方"
navigation:
  title: "第六章"
---

# 第 06 章：数据驱动配方

本章说明 MagicIO 26.1 当前源码中的阵配方体系。这里的“数据驱动”不是直接委托给原版 `RecipeManager` 完成全部工作，而是由服务端扫描数据包资源、解析为 `ZhenRecipe`、存入独立缓存，再同步给客户端。编写扩展时必须以这一实际链路为准。

## 1. 配方处理链路

一份阵配方依次经过以下阶段：

1. JSON 位于 `data/<命名空间>/recipe/**/*.json`。
2. `MagicIO.loadRecipesToManager` 从服务端 `ResourceManager` 的 `recipe` 目录枚举资源。
3. 当前实现只接受 `magic_io` 命名空间下的资源。
4. `ZhenRecipeLoader.loadRecipeFromJson` 手工解析 JSON，并校验阵类型、物品、流体和战利品表。
5. 有效配方加入 `ZhenRecipeManager`，按阵功能基础名分组。
6. `RecipeProcessor` 根据机器的阵类型、槽位分区和当前资源寻找并执行配方。
7. 服务端通过 `ZhenRecipeSyncPayload` 把完整缓存发给客户端，供 JEI 展示。

关键源码：

- `MagicIO#loadRecipesToManager`
- `ZhenRecipeLoader`
- `ZhenRecipe`
- `ZhenRecipeSerializer`
- `ZhenRecipeManager`
- `RecipeProcessor`

## 2. 最小配方示例

项目中的不稳定筛阵配方是完整且直观的参考：

```json
{
  "type": "magic_io:zhen_block",
  "zhen_type": "magic_io:unstable_sieve_zhen",
  "inputs": {
    "item_input_all": [
      {"item": "minecraft:coarse_dirt", "count": 1}
    ]
  },
  "outputs": {
    "item_output_all": [
      {"item": "minecraft:dirt", "count": 1}
    ],
    "drop_output": [
      {"loot_table": "magic_io:sift_metal_drop"}
    ]
  },
  "processing_time": 100
}
```

字段含义：

| 字段 | 当前行为 |
| --- | --- |
| `type` | 数据中保留的配方类型标识；手工加载器当前不读取它进行分派 |
| `zhen_type` | 必填，必须能在 `ZhenType.ZHEN_TYPES` 自定义注册表中找到 |
| `inputs` | 物品输入，按槽位分区组织，每项的 `item` 包含标准 Ingredient 和数量 |
| `outputs` | 物品或战利品输出，键是槽位区域名 |
| `fluid_inputs` | 流体输入，按区域分组 |
| `fluid_outputs` | 流体输出，按区域分组 |
| `processing_time` | 基础处理 tick 数，20 tick 通常约为 1 秒 |

## 3. 区域化输入输出

配方并不直接记录物理槽位编号，而是记录 `SlotZone` 的名称。执行时，`SlotPartition` 把区域映射到具体槽位。常用区域包括：

- `item_input_all`：通用物品输入区。
- `item_output_all`：通用物品输出区。
- `fluid_input_all`：通用流体输入区。
- `fluid_output_all`：通用流体输出区。
- `drop_output`：不进入容器、直接向世界产出的区域。

这种设计让同一功能的多个等级可以共享配方。`ZhenRecipeManager` 会经由 `ZhenLevel.baseName` 去掉等级前缀，再去掉 `_zhen` 后缀。例如 `unstable_sieve_zhen` 与更高等级的 `sieve_zhen` 最终都归入 `sieve` 组。

新增配方前，应先检查目标 `ZhenType` 使用的 `SlotPartition` 是否真的声明了相应区域。JSON 中写入一个机器没有的区域，不等于机器会自动获得该槽位。

## 4. 物品输入

推荐使用按区域分组的对象格式：

```json
"inputs": {
  "item_input_all": [
    {"item": "minecraft:iron_ingot", "count": 2},
    {"item": "#c:dusts/redstone", "count": 1}
  ]
}
```

每项输入保存完整 `Ingredient` 和独立的正整数 `count`。省略 `count` 时按 1 处理，小于 1 会作为无效配方数据拒绝。`item` 由标准 Codec 解析，可表达固定物品、标签以及 NeoForge 已注册的自定义 Ingredient。

## 5. 物品与流体输出

固定物品输出写法：

```json
"outputs": {
  "item_output_all": [
    {"item": "minecraft:copper_ingot", "count": 3}
  ]
}
```

流体输入既支持具体流体，也支持以 `#` 开头的流体标签：

```json
"fluid_inputs": {
  "fluid_input_all": [
    {"fluid": "#minecraft:water", "amount": 1000}
  ]
}
```

流体输出当前要求具体流体 ID：

```json
"fluid_outputs": {
  "fluid_output_all": [
    {"fluid": "minecraft:water", "amount": 250}
  ]
}
```

`amount` 使用 NeoForge `FluidStack` 的整数容量单位。配方是否能执行，还取决于机器流体区域、容量以及 `RecipeProcessor` 的输出预检，不能只验证 JSON 能否解析。

## 6. 内存模型与序列化

`ZhenRecipe` 保存四类核心信息：阵类型 ID、`RecipeInput<?>` 列表、`RecipeOutput<?>` 列表和基础处理时间。输入输出通过 `IOType` 区分物品与流体，使处理器可以用统一结构遍历不同资源。

`ZhenRecipeSerializer` 同时定义：

- `MapCodec<ZhenRecipe>`：结构化数据编解码。
- `StreamCodec<RegistryFriendlyByteBuf, ZhenRecipe>`：服务端到客户端的网络编解码。
- `RecipeSerializer<ZhenRecipe>`：将两种编解码器包装为 Minecraft 配方序列化器。

网络格式按固定顺序写入配方 ID、阵类型、物品输入需求及数量、物品输出、流体输入、流体输出和处理时间。修改数据模型时必须同步修改编码与解码两侧，否则客户端会发生错位读取或断线。

## 7. 匹配、缓存与倍率

`RecipeProcessor` 不会每 tick 无条件扫描所有配方。它保存上次有效配方和输入哈希，在输入未变化时优先复用缓存；没有匹配项时也采用间隔检查，降低空机器成本。

配方完成前会执行以下工作：

1. 确认当前世界是 `ServerLevel`。
2. 滚动固定输出与战利品输出。
3. 应用等级或插件提供的产出倍率。
4. 检查物品与流体输出空间。
5. 消耗输入。
6. 写入输出区域或生成世界掉落。

`RecipeModifiers` 允许多个 `RecipeModifier` 以乘法叠加处理时间倍率和产出倍率。扩展方应避免返回负数、零或非有限数，并明确多个修饰器叠加后的平衡结果。

## 8. 调试步骤

1. 确认文件路径是 `data/magic_io/recipe/...json`；当前扫描器不会加载其他命名空间。
2. 确认 `zhen_type` 已注册，并且名称包含正确的等级前缀与 `_zhen` 后缀。
3. 确认区域名与目标阵的 `SlotPartition` 一致。
4. 确认所有物品和流体 ID 存在，输入标签已在注册表中绑定成员。
5. 执行 `/reload`，观察 `Loaded ... recipes into ZhenRecipeManager` 日志。
6. 若解析失败，查看 `Failed to load recipe from JSON` 或具体资源路径错误日志。
7. 若能加载但不执行，继续检查输入数量、流体容量、等级限制和输出空间。
8. 若服务端执行正常但 JEI 不更新，转到第 08、09 章检查同步与运行时刷新。

## 9. 当前限制

- 扫描器把命名空间固定为 `magic_io`，第三方数据包不能仅靠自己的命名空间直接注入阵配方。
- JSON 顶层结构由手工 Gson 读取，物品需求字段统一委托 `ItemRequirement` Codec 和标准 `Ingredient.CODEC` 解析。
- 战利品表必须声明在明确的物品输出区域中。
- `ZhenRecipeManager` 是进程内单例缓存，服务端与客户端分别维护自己的内容。

扩展这些能力时，应先统一“磁盘格式、内存模型、网络格式、JEI 展示”四个层面，避免只修改其中一层。
