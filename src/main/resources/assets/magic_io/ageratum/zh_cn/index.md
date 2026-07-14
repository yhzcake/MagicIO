---
title: "MagicIO 教程文档"
navigation:
  title: "MagicIO 教程文档"
---

# MagicIO 教程文档

本目录同时收录当前版本教程、规划玩法教程和专业开发教程。三部分的事实边界不同，阅读时请勿将规划内容视为当前版本已经实现的功能。

## 阅读入口

### 当前版本

面向希望体验现有功能的玩家、测试人员和整合包作者。内容严格依据当前源码与资源编写。

1. [第一章：项目现状与阅读边界](current/01-project-status-and-reading-scope.md)
2. [第二章：阵等级、元素与功能目录](current/02-zhen-tiers-elements-and-function-catalog.md)
3. [第三章：单体阵的放置与加工](current/03-individual-zhen-placement-and-processing.md)
4. [第四章：阵总线与六面处理器](current/04-zhen-bus-and-six-sided-processors.md)
5. [第五章：网格单元与阵纹解析](current/05-grid-cells-and-zhen-pattern-parsing.md)
6. [第六章：数据包阵配方开发](current/06-zhen-recipes-with-datapacks.md)
7. [第七章：兼容展示、调试与已知限制](current/07-compatibility-debugging-and-known-limitations.md)

### 规划玩法

面向希望了解 MagicIO 最终玩法愿景的读者。该部分描述规划内容，不代表当前版本已经实现。

1. [第一章：世界成长](planned/01-world-and-progression-path.md)
2. [第二章：LV0](planned/02-lv-0-post-apocalyptic-survival.md)
3. [第三章：法阵](planned/03-zhen-system.md)
4. [第四章：元素](planned/04-element-system.md)
5. [第五章：供能升级](planned/05-power-and-upgrades.md)
6. [第六章：自动化](planned/06-automation-storage-and-logistics.md)
7. [第七章：高阶终局](planned/07-advanced-era-and-elemental-endgame.md)
8. [第八章：状态索引](planned/08-planned-status-index.md)

### 开发人员

面向附属模组作者、整合包作者和数据包作者。内容覆盖 Java 扩展、数据驱动、资源重载及兼容集成。

1. [第一章：环境与项目结构](developer/01-development-environment-and-project-structure.md)
2. [第二章：Java 注册表](developer/02-java-registry-system.md)
3. [第三章：阵扩展](developer/03-extending-zhen.md)
4. [第四章：IO 与 Capability](developer/04-io-and-capability.md)
5. [第五章：阵总线](developer/05-zhen-bus.md)
6. [第六章：数据驱动配方](developer/06-data-driven-recipes.md)
7. [第七章：战利品表输出](developer/07-loot-table-outputs.md)
8. [第八章：资源重载与网络同步](developer/08-resource-reload-and-network-synchronization.md)
9. [第九章：JEI 与 Jade 集成](developer/09-jei-and-jade-integration.md)
10. [第十章：API 设计与模组兼容](developer/10-api-design-and-mod-compatibility.md)

## 版本边界

- 当前项目目标环境为 Minecraft 26.1.2、NeoForge 26.1.2.41-beta 和 Java 25。
- JEI 与 Jade 是可选依赖，不安装时不影响 MagicIO 基础加载。
- 当前版本尚未提供完整的生存获取路线，部分内容需要创造模式、命令或外部自动化设备进行测试。
- 当前公开 Java 类型尚未形成独立且承诺稳定兼容的 API 模块，附属模组应锁定准确版本并进行兼容性测试。
- 规划教程中的世界、材料、能源和终局系统以设计目标为准，实际发布内容可能随实现与平衡测试调整。
