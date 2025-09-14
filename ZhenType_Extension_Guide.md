# ZhenType 扩展设计使用指南

## 概述

新的 `ZhenType` 设计采用注册系统替代枚举，允许其他模组和开发者动态注册自定义阵法类型，大大提高了系统的可扩展性。

## 核心组件

### 1. ZhenType 类
- 不再是枚举，而是一个普通的 Java 类
- 包含 `id`、`displayName` 和 `description` 属性
- 支持相等性比较（基于 ID）

### 2. ZhenTypeRegistry 注册器
- 管理所有已注册的阵法类型
- 提供注册、查询、默认类型管理等功能
- 内置基础阵法类型：风、火、水、土、雷

### 3. ZhenTypes 初始化类
- 集中管理阵法类型的初始化
- 提供扩展示例

## 使用方法

### 基础用法
```java
// 使用内置阵法类型
new ZhenBlock(ZhenProperties.of()
    .destroyTime(2.0f)
    .explosionResistance(10.0f)
    .sound(SoundType.GRAVEL)
    .lightLevel(state -> 7)
    .zhenType(ZhenTypeRegistry.WIND));
```

### 注册自定义阵法类型
```java
// 在模组初始化时注册
public class MyModZhenTypes {
    public static final ZhenType CUSTOM_TYPE = ZhenTypeRegistry.register(
        new ZhenType("my_mod:custom", "自定义阵法", "我的模组的特殊阵法")
    );
}

// 使用自定义阵法类型
new ZhenBlock(ZhenProperties.of()
    .zhenType(MyModZhenTypes.CUSTOM_TYPE));
```

### 查询阵法类型
```java
// 根据 ID 查询
ZhenType type = ZhenTypeRegistry.getById("wind");

// 检查是否已注册
boolean exists = ZhenTypeRegistry.isRegistered("custom_type");

// 获取所有类型
Collection<ZhenType> allTypes = ZhenTypeRegistry.getAllTypes();
```

## 扩展性优势

1. **动态注册**：其他模组可以在运行时注册新的阵法类型
2. **命名空间支持**：使用 `modid:type_name` 格式避免冲突
3. **向后兼容**：现有代码无需大幅修改
4. **类型安全**：仍然保持编译时类型检查
5. **易于调试**：提供详细的注册日志

## 最佳实践

1. 使用模组ID前缀避免命名冲突
2. 在模组初始化早期注册阵法类型
3. 提供有意义的描述信息
4. 为自定义类型提供对应的阵法行为实现

## 迁移指南

从枚举版本迁移到注册系统版本：

### 旧代码
```java
ZhenType.WIND
ZhenType.fromId("wind")
```

### 新代码  
```java
ZhenTypeRegistry.WIND
ZhenTypeRegistry.getById("wind")
```

大部分 API 保持一致，主要是将 `ZhenType` 替换为 `ZhenTypeRegistry`。