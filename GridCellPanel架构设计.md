# GridCellPanel（网格面板）架构设计

## 1. 概述

### 1.1 目标

实现一个**平面方块**（类似 ZhenBus 的薄片碰撞箱），该方块被划分为 **4×4 = 16 个单元格（Cell）**，每个单元格可独立交互。玩家主手持木棍（stick，调试用）时，根据**副手物品**的不同，对点击的单元格施加不同的**单元格动作（CellAction）**。当 16 个单元格全部被填充后，按照**网格解析规则（GridParseRule）**自动解析并产生结果。

### 1.2 核心概念

```
┌─────────────────────────────────────────────┐
│             GridCellPanel（方块）             │
│  ┌────┬────┬────┬────┐                       │
│  │ C0 │ C1 │ C2 │ C3 │  每个单元格存储一个    │
│  ├────┼────┼────┼────┤  CellAction 的 ID     │
│  │ C4 │ C5 │ C6 │ C7 │                       │
│  ├────┼────┼────┼────┤  16 格全部填满后       │
│  │ C8 │ C9 │ C10│ C11│  → 触发 GridParseRule │
│  ├────┼────┼────┼────┤                       │
│  │ C12│ C13│ C14│ C15│                       │
│  └────┴────┴────┴────┘                       │
│  碰撞箱: 平面薄片 (1/16 高)                   │
└─────────────────────────────────────────────┘
```

### 1.3 与现有架构的对应关系

| 现有系统 | GridCellPanel 系统 | 说明 |
|---------|-------------------|------|
| `ZhenBusBlock` | `GridCellPanelBlock` | 方块类，平面碰撞箱 |
| `ZhenBusBlockEntity` | `GridCellPanelBlockEntity` | 方块实体，管理 16 个单元格 |
| `ZhenType` | `CellAction` | 自定义注册表：单元格动作类型 |
| - | `GridParseRule` | 自定义注册表：16 格填满后的解析规则 |
| `ZhenBusContainer` | `GridCellStorage` | 网格数据存储与序列化 |
| `ElementType` | （参考其注册表模式） | 注册表构建方式一致 |

---

## 2. 自定义注册表设计

本项目需要新建 **2 个自定义 NeoForge Registry**，完全沿用现有 `IOType` / `ElementType` / `ZhenType` 的注册模式。

### 2.1 CellAction（单元格动作注册表）

**作用：** 定义副手物品与单元格动作的映射关系。每个 `CellAction` 实例代表一种可以被"填入"单元格的动作类型。

**注册模式（与 `IOType` 完全一致）：**

```java
// === CellAction.java ===
public class CellAction {
    public static final ResourceKey<Registry<CellAction>> CELL_ACTION_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "cell_action"));
    public static Registry<CellAction> CELL_ACTIONS;

    private final String name;
    // 可根据需要扩展字段，例如：
    // private final int color;           // 单元格显示颜色
    // private final Supplier<Item> icon; // 显示图标

    public CellAction(String name) { ... }

    @SubscribeEvent
    public static void register(NewRegistryEvent event) {
        CELL_ACTIONS = new RegistryBuilder<>(CELL_ACTION_REGISTRY_KEY)
                .defaultKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "empty"))
                .create();
        event.register(CELL_ACTIONS);
    }
}

// === ModCellActions.java ===
public class ModCellActions {
    public static final DeferredRegister<CellAction> CELL_ACTIONS =
            DeferredRegister.create(CellAction.CELL_ACTION_REGISTRY_KEY, MagicIO.MOD_ID);

    // 调试用基础动作
    public static Supplier<CellAction> FIRE;
    public static Supplier<CellAction> WATER;
    public static Supplier<CellAction> EARTH;
    public static Supplier<CellAction> WIND;
    // ... 预留更多

    public static void register(IEventBus eventBus) {
        FIRE  = CELL_ACTIONS.register("fire",  () -> new CellAction("fire"));
        WATER = CELL_ACTIONS.register("water", () -> new CellAction("water"));
        EARTH = CELL_ACTIONS.register("earth", () -> new CellAction("earth"));
        WIND  = CELL_ACTIONS.register("wind",  () -> new CellAction("wind"));
        CELL_ACTIONS.register(eventBus);
    }
}
```

### 2.2 GridParseRule（网格解析规则注册表）

**作用：** 定义 16 格全部填满后的解析规则。每条规则检查 4×4 网格的 `CellAction` 分布模式，匹配则执行对应逻辑。

**注册模式：**

```java
// === GridParseRule.java ===
public class GridParseRule {
    public static final ResourceKey<Registry<GridParseRule>> GRID_PARSE_RULE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "grid_parse_rule"));
    public static Registry<GridParseRule> GRID_PARSE_RULES;

    private final String name;
    private final Predicate<CellAction[][]> matcher;    // 匹配条件：4×4 网格
    private final Consumer<GridParseContext> handler;   // 命中后的执行逻辑

    public GridParseRule(String name,
                         Predicate<CellAction[][]> matcher,
                         Consumer<GridParseContext> handler) { ... }

    @SubscribeEvent
    public static void register(NewRegistryEvent event) {
        GRID_PARSE_RULES = new RegistryBuilder<>(GRID_PARSE_RULE_REGISTRY_KEY)
                .defaultKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "empty"))
                .create();
        event.register(GRID_PARSE_RULES);
    }
}

// === GridParseContext.java ===
// 传递给解析规则处理器的上下文
public record GridParseContext(
    Level level,
    BlockPos pos,
    BlockState state,
    GridCellPanelBlockEntity blockEntity,
    CellAction[][] grid    // 4×4 的只读快照
) {}
```

---

## 3. 方块设计

### 3.1 方块类：`GridCellPanelBlock`

```java
public class GridCellPanelBlock extends BaseEntityBlock {
    // 碰撞箱：与 ZhenBus 一致的平面薄片
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1.0/16, 1);

    @Override
    public VoxelShape getShape(...) { return SHAPE; }

    @Override
    public VoxelShape getCollisionShape(...) { return SHAPE; }

    @Override
    protected boolean isCollisionShapeFullBlock(...) { return false; }
}
```

**核心交互流程（`useItemOn`）：**

```
玩家右击方块
  │
  ├─ 主手 != 木棍(stick)
  │    └─ 返回 PASS（交给其他逻辑处理）
  │
  └─ 主手 == 木棍(stick)
       │
       ├─ 副手为空
       │    └─ 返回 PASS 或清除当前单元格
       │
       └─ 副手有物品
            ├─ 根据 hit 坐标计算点击了哪个 Cell (0~15)
            ├─ 查找副手物品 → CellAction 映射
            │    （可通过 Map<Item, CellAction> 或 DataComponent 实现）
            ├─ 将 CellAction 设置到目标单元格
            └─ 检查是否 16 格全满 → 触发 GridParseRule 解析
```

**单元格坐标计算：**

```java
// 平面碰撞箱假设朝上放置（默认DOWN面朝下）
// hit 坐标为相对方块西北角的 0~1 浮点值
int cellX = MathHelper.clamp((int)(hitLocation.x * 4), 0, 3);
int cellZ = MathHelper.clamp((int)(hitLocation.z * 4), 0, 3);
int cellIndex = cellX + cellZ * 4;  // 0~15
```

### 3.2 注册方式

参照 `ModZhenBusBlocks` 的模式，在 `ModGridCellBlocks` 中注册：

```java
public class ModGridCellBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MagicIO.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MagicIO.MOD_ID);

    public static final DeferredBlock<Block> GRID_CELL_PANEL =
            BLOCKS.registerBlock("grid_cell_panel",
                    p -> new GridCellPanelBlock(p.noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GridCellPanelBlockEntity>>
            GRID_CELL_PANEL_BE = ...;
}
```

---

## 4. 方块实体设计

### 4.1 数据存储：`GridCellStorage`

核心数据结构：

```java
public class GridCellStorage {
    // 4×4 网格，存储每个单元格的 CellAction
    // null 表示该格未填充
    private final CellAction[][] grid = new CellAction[4][4];

    // 副手物品 → CellAction 映射表
    // 调试阶段使用硬编码映射，后续可扩展为 DataComponent 或 JSON 配置
    private static final Map<Item, Supplier<CellAction>> ITEM_TO_ACTION = Map.of(
        Items.FIRE_CHARGE,  ModCellActions.FIRE,
        Items.WATER_BUCKET, ModCellActions.WATER,
        Items.DIRT,         ModCellActions.EARTH,
        Items.FEATHER,      ModCellActions.WIND
    );

    public void setCell(int x, int z, @Nullable CellAction action) { grid[x][z] = action; }
    public @Nullable CellAction getCell(int x, int z) { return grid[x][z]; }

    /** 检查是否 16 格全部非空 */
    public boolean isFullyFilled() { ... }

    /** 获取只读快照供 GridParseRule 匹配 */
    public CellAction[][] getSnapshot() { ... }

    /** 查找副手物品对应的 CellAction */
    public static @Nullable CellAction getActionForOffhand(ItemStack offhand) { ... }

    // NBT 序列化：将 16 个 CellAction 的 ID 写入/读出
    public void writeToNBT(ValueOutput output) { ... }
    public void readFromNBT(ValueInput input) { ... }
}
```

### 4.2 方块实体：`GridCellPanelBlockEntity`

```java
public class GridCellPanelBlockEntity extends BlockEntity {
    private final GridCellStorage storage = new GridCellStorage();
    private boolean parseTriggered = false;  // 防止重复触发解析

    // Server tick: 检查 16 格是否全满且未触发解析
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   GridCellPanelBlockEntity be) {
        if (be.storage.isFullyFilled() && !be.parseTriggered) {
            be.parseTriggered = true;
            be.runParseRules();
        }
    }

    /** 遍历所有 GridParseRule，匹配第一个成功的规则 */
    private void runParseRules() {
        CellAction[][] snapshot = storage.getSnapshot();
        for (GridParseRule rule : GridParseRule.GRID_PARSE_RULES) {
            if (rule.matcher().test(snapshot)) {
                rule.handler().accept(new GridParseContext(
                    level, worldPosition, getBlockState(), this, snapshot));
                break;  // 只执行第一个匹配的规则
            }
        }
    }
}
```

### 4.3 解析触发时机

```
16 格全部填充完成
    │
    ├─ 触发条件满足（isFullyFilled() == true）
    │
    ├─ parseTriggered = false（首次触发）
    │    └─ 遍历 GridParseRule 列表
    │         ├─ 规则 A 匹配？ → 执行 A.handler() → 停止
    │         ├─ 规则 B 匹配？ → 执行 B.handler() → 停止
    │         └─ 都不匹配 → 无操作
    │
    └─ parseTriggered = true（防止重复触发）
         └─ 需要重置网格才能再次触发
```

**重置机制：** 当玩家手持木棍点击一个**已有 CellAction** 的单元格时，可以清除该格，并将 `parseTriggered` 重置为 false。

---

## 5. 交互流程详细说明

### 5.1 填充单元格

```
[条件] 主手 = stick + 副手 != 空 + 点击位置有 BE
  1. 计算 cellX, cellZ (0~3)
  2. 获取副手物品对应的 CellAction
  3. 将 CellAction 设置到 grid[cellX][cellZ]
  4. 服务端发送更新包给客户端
  5. markForUpdate()
  6. 检查 isFullyFilled()
```

### 5.2 清除单元格

```
[条件] 主手 = stick + 副手 = 空 + 点击位置有 BE
  1. 计算 cellX, cellZ
  2. 将 grid[cellX][cellZ] 设为 null
  3. parseTriggered = false
  4. markForUpdate()
```

### 5.3 副手物品映射

调试阶段使用 `Map<Item, Supplier<CellAction>>` 硬编码：

| 副手物品 | CellAction | 说明 |
|---------|-----------|------|
| 火焰弹 (FireCharge) | FIRE | 火 |
| 水桶 (WaterBucket) | WATER | 水 |
| 泥土 (Dirt) | EARTH | 土 |
| 羽毛 (Feather) | WIND | 风 |
| ... | ... | 后续可扩展 |

---

## 6. 文件清单与目录结构

所有新增文件均遵循现有项目的包命名规范：

```
src/main/java/cn/yhzcake/magicio/
├── block/
│   └── gridcell/                          # 新增包：网格面板
│       ├── GridCellPanelBlock.java        # 方块类
│       ├── GridCellPanelBlockEntity.java  # 方块实体
│       ├── GridCellStorage.java           # 网格数据存储
│       ├── ModGridCellBlocks.java         # 方块 & BE 注册
│       ├── CellAction.java                # CellAction 类型 & 注册表
│       ├── ModCellActions.java            # CellAction 条目注册
│       ├── GridParseRule.java             # 网格解析规则 & 注册表
│       ├── ModGridParseRules.java         # 解析规则条目注册
│       └── GridParseContext.java          # 解析规则执行上下文
```

---

## 7. 实现步骤

### Stage 1：注册表基础设施

| 步骤 | 内容 | 说明 |
|------|------|------|
| 1.1 | 创建 `CellAction.java` | 定义注册表键、Registry 字段、`@SubscribeEvent` 注册方法（同 `IOType` 模式） |
| 1.2 | 创建 `ModCellActions.java` | 使用 `DeferredRegister` 注册基础 CellAction（FIRE/WATER/EARTH/WIND） |
| 1.3 | 创建 `GridParseRule.java` | 定义注册表键、Registry 字段、规则类（含 matcher + handler） |
| 1.4 | 创建 `GridParseContext.java` | 解析上下文 record |
| 1.5 | 创建 `ModGridParseRules.java` | 注册基础解析规则 |
| 1.6 | 在 `MagicIO` 构造函数中注册 | 调用 `modEventBus.register(CellAction.class)`、`ModCellActions.register()` 等 |

### Stage 2：方块与方块实体

| 步骤 | 内容 | 说明 |
|------|------|------|
| 2.1 | 创建 `GridCellStorage.java` | 4×4 网格存储、副手→动作映射、NBT 序列化 |
| 2.2 | 创建 `GridCellPanelBlockEntity.java` | BE 实现，含 serverTick 和解析触发逻辑 |
| 2.3 | 创建 `GridCellPanelBlock.java` | 方块类，平面碰撞箱，交互逻辑 |
| 2.4 | 创建 `ModGridCellBlocks.java` | 注册方块和 BE |
| 2.5 | 注册 Item Block | 类似 `ZHEN_BUS_ITEM` 的注册方式 |

### Stage 3：调试与验证

| 步骤 | 内容 | 说明 |
|------|------|------|
| 3.1 | 配置副手物品映射 | 使用 Items.FIRE_CHARGE 等调试 |
| 3.2 | 编写一个解析规则 | 例如：全为 FIRE → 爆炸效果 |
| 3.3 | 客户端渲染 | 确保方块碰撞箱、选中框显示正常 |

---

## 8. 关键设计要点

### 8.1 碰撞箱

直接复用 `ZhenBusContainer.DEFAULT_FACE` 的尺寸：
```java
Shapes.box(0, 0, 0, 1, 1.0/16, 1)
```
这是一个**底面平贴**的薄片，玩家点击时可以精确计算 hit 位置来定位单元格。

### 8.2 单元格点击定位

利用 `BlockHitResult` 中的 `getLocation()` 获取点击位置：
- 点击方块底面/顶面时，取 `x` 和 `z` 坐标的小数部分
- 乘以 4 并取整 → cellX, cellZ
- 这种方法仅在方块朝上/朝下放置时有效（薄片多为水平放置）

### 8.3 解析规则的匹配模式

`GridParseRule` 的 `Predicate<CellAction[][]>` 可以支持多种匹配模式：

```java
// 示例：全相同模式
Predicate<CellAction[][]> allSame = grid -> {
    CellAction first = grid[0][0];
    for (int x = 0; x < 4; x++)
        for (int z = 0; z < 4; z++)
            if (grid[x][z] != first) return false;
    return true;
};

// 示例：对角线模式
Predicate<CellAction[][]> diagonal = grid -> {
    for (int i = 0; i < 4; i++)
        if (grid[i][i] != CellAction.XX) return false;
    return true;
};
```

### 8.4 网络同步

参考 `ZhenBusBlockEntity` 的同步模式：

```java
@Override
public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    CompoundTag tag = super.getUpdateTag(registries);
    storage.writeToNBT(tag);  // 同步网格数据到客户端
    return tag;
}

@Override
public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
}
```

---

## 9. 扩展性预留

| 扩展方向 | 预留方式 | 说明 |
|---------|---------|------|
| CellAction 视觉效果 | CellAction 增加颜色/纹理字段 | 每个单元格渲染不同颜色/图标 |
| 更复杂的匹配规则 | GridParseRule 增加权重/优先级 | 支持多条规则按优先级匹配 |
| 非木棍触发 | 增加可配置的触发物品列表 | 通过 Config 或 DataComponent 配置 |
| 非副手映射 | 支持 NBT 匹配 / 物品标签匹配 | 更灵活的映射条件 |
| 解析结果多样化 | GridParseContext 携带更多上下文 | 可产生物品、实体、事件等 |
| 单元格 UI | 支持在单元格上方显示悬浮物品 | 使用 ItemStack 渲染 |

---

## 10. 与现有系统的关联

| 文件 | 修改内容 |
|------|---------|
| `MagicIO.java` | 注册 `@SubscribeEvent` 和 `ModCellActions`/`ModGridParseRules` |
| `ModCreativeModeTabs.java` | 注册 `grid_cell_panel` 到创造模式标签页 |
| `README.MD` | 后者补充此设计文档的说明 |

---

> **文档状态：** 规划设计阶段
> **目标版本：** NeoForge 1.21.x
> **相关文档：** [多面并行阵架构设计.md](多面并行阵架构设计.md)
