package asia.yhzcake.magicio.block.blocks;

import asia.yhzcake.magicio.MagicIO;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 元素组合管理器
 * 提供高级的元素组合和查询功能
 */
public class ElementCombinationManager {
    
    // 元素层级定义
    public enum ElementTier {
        BASIC(1, "基础元素"),
        SECONDARY(2, "二级元素"),
        TERTIARY(3, "三级元素"),
        QUATERNARY(4, "四级元素"),
        ULTIMATE(5, "终极元素");
        
        private final int level;
        private final String description;
        
        ElementTier(int level, String description) {
            this.level = level;
            this.description = description;
        }
        
        public int getLevel() { return level; }
        public String getDescription() { return description; }
    }
    
    // 元素分类定义
    public enum ElementCategory {
        MATTER("物质操控"),        // 土、水、火、风、木、金
        ENERGY("能量操控"),        // 火、雷、能量
        STRUCTURE("结构操控"),     // 秩序、混沌、冻
        SPACETIME("时空操控"),     // 时间、空间
        INFORMATION("信息操控"),   // 描述、意识
        CREATION("创造操控");      // 创造
        
        private final String description;
        
        ElementCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    // 元素信息映射
    private static final Map<ZhenType, ElementInfo> ELEMENT_INFO = new HashMap<>();
    
    /**
     * 元素信息类
     */
    public static class ElementInfo {
        private final ElementTier tier;
        private final ElementCategory category;
        private final Set<String> properties;
        private final String primaryFunction;
        
        public ElementInfo(ElementTier tier, ElementCategory category, String primaryFunction, String... properties) {
            this.tier = tier;
            this.category = category;
            this.primaryFunction = primaryFunction;
            this.properties = new HashSet<>(Arrays.asList(properties));
        }
        
        public ElementTier getTier() { return tier; }
        public ElementCategory getCategory() { return category; }
        public Set<String> getProperties() { return new HashSet<>(properties); }
        public String getPrimaryFunction() { return primaryFunction; }
    }
    
    static {
        initializeElementInfo();
    }
    
    /**
     * 初始化元素信息
     */
    private static void initializeElementInfo() {
        // 基础元素
        ELEMENT_INFO.put(ZhenTypes.EARTH, new ElementInfo(
            ElementTier.BASIC, ElementCategory.MATTER, "小分子固体操控",
            "筛选", "离心", "分离", "固体处理"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.WATER, new ElementInfo(
            ElementTier.BASIC, ElementCategory.MATTER, "流体操控",
            "储存", "流动", "液体", "气体", "传输"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.FIRE, new ElementInfo(
            ElementTier.BASIC, ElementCategory.ENERGY, "内能操控",
            "加热", "熔炉", "高炉", "能量转移", "守恒"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.WIND, new ElementInfo(
            ElementTier.BASIC, ElementCategory.ENERGY, "机械能操控",
            "粉碎", "传输", "加速", "机械能转换", "动力"
        ));
        
        // 二级元素
        ELEMENT_INFO.put(ZhenTypes.WOOD, new ElementInfo(
            ElementTier.SECONDARY, ElementCategory.MATTER, "大分子固体操控",
            "发酵", "有机物", "生物处理", "生长"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.FREEZE, new ElementInfo(
            ElementTier.SECONDARY, ElementCategory.STRUCTURE, "变化操控",
            "膨胀", "收缩", "物态变化", "相变", "温度控制"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.METAL, new ElementInfo(
            ElementTier.SECONDARY, ElementCategory.MATTER, "微观集团操控",
            "炼金术", "反应釜", "无机物", "原子操作"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.LIGHTNING, new ElementInfo(
            ElementTier.SECONDARY, ElementCategory.ENERGY, "电磁力操控",
            "磁化", "发电", "电磁", "能量转换"
        ));
        
        // 三级元素
        ELEMENT_INFO.put(ZhenTypes.ORDER, new ElementInfo(
            ElementTier.TERTIARY, ElementCategory.STRUCTURE, "运行操控",
            "顺序", "过滤", "轮询", "逻辑控制", "规则"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.SPACE, new ElementInfo(
            ElementTier.TERTIARY, ElementCategory.SPACETIME, "引力操控",
            "压缩", "空间", "并行", "并发", "维度"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.TIME, new ElementInfo(
            ElementTier.TERTIARY, ElementCategory.SPACETIME, "进程操控",
            "加速", "时间", "流速", "同步"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.CHAOS, new ElementInfo(
            ElementTier.TERTIARY, ElementCategory.STRUCTURE, "随机操控",
            "概率", "随机", "混沌", "不确定性", "变异"
        ));
        
        // 四级元素
        ELEMENT_INFO.put(ZhenTypes.ENERGY, new ElementInfo(
            ElementTier.QUATERNARY, ElementCategory.ENERGY, "基本力操控",
            "四大基本力", "维持", "稳定", "核心能量"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.DESCRIPTION, new ElementInfo(
            ElementTier.QUATERNARY, ElementCategory.INFORMATION, "信息操控",
            "数据", "信息", "描述", "记录", "编码"
        ));
        
        // 五级元素
        ELEMENT_INFO.put(ZhenTypes.CREATION, new ElementInfo(
            ElementTier.ULTIMATE, ElementCategory.CREATION, "创造操控",
            "创造", "生成", "无中生有", "全能"
        ));
        
        ELEMENT_INFO.put(ZhenTypes.CONSCIOUSNESS, new ElementInfo(
            ElementTier.ULTIMATE, ElementCategory.INFORMATION, "灵魂操控",
            "意识", "灵魂", "生物信息", "刷怪", "生命"
        ));
        
        MagicIO.LOGGER.info("元素信息初始化完成，共有 {} 个元素", ELEMENT_INFO.size());
    }
    
    /**
     * 获取元素信息
     * @param element 元素类型
     * @return 元素信息，如果未找到返回null
     */
    @Nullable
    public static ElementInfo getElementInfo(@Nonnull ZhenType element) {
        return ELEMENT_INFO.get(element);
    }
    
    /**
     * 根据分类获取元素列表
     * @param category 元素分类
     * @return 该分类下的所有元素
     */
    @Nonnull
    public static List<ZhenType> getElementsByCategory(@Nonnull ElementCategory category) {
        return ELEMENT_INFO.entrySet().stream()
            .filter(entry -> entry.getValue().getCategory() == category)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * 根据层级获取元素列表
     * @param tier 元素层级
     * @return 该层级下的所有元素
     */
    @Nonnull
    public static List<ZhenType> getElementsByTier(@Nonnull ElementTier tier) {
        return ELEMENT_INFO.entrySet().stream()
            .filter(entry -> entry.getValue().getTier() == tier)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * 查找具有特定属性的元素
     * @param property 要查找的属性
     * @return 具有该属性的元素列表
     */
    @Nonnull
    public static List<ZhenType> getElementsByProperty(@Nonnull String property) {
        return ELEMENT_INFO.entrySet().stream()
            .filter(entry -> entry.getValue().getProperties().contains(property))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取元素的进化路径
     * @param element 要查询的元素
     * @return 可以由该元素进化得到的元素列表
     */
    @Nonnull
    public static List<ZhenType> getEvolutionPaths(@Nonnull ZhenType element) {
        List<ZhenType> evolutionPaths = new ArrayList<>();
        
        // 检查所有组合，找到包含该元素的组合
        for (Map.Entry<String, ZhenType> entry : ZhenTypeRegistry.getAllCombinations().entrySet()) {
            String combinationKey = entry.getKey();
            if (combinationKey.contains(element.getId())) {
                evolutionPaths.add(entry.getValue());
            }
        }
        
        return evolutionPaths.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * 获取能够组合成目标元素的所有组合方式
     * @param target 目标元素
     * @return 组合方式列表，每个方式包含2-3个元素
     */
    @Nonnull
    public static List<List<ZhenType>> getCombinationRecipes(@Nonnull ZhenType target) {
        List<List<ZhenType>> recipes = new ArrayList<>();
        
        for (Map.Entry<String, ZhenType> entry : ZhenTypeRegistry.getAllCombinations().entrySet()) {
            if (entry.getValue().equals(target)) {
                String[] elementIds = entry.getKey().split("\\+");
                List<ZhenType> recipe = Arrays.stream(elementIds)
                    .map(ZhenTypeRegistry::getById)
                    .collect(Collectors.toList());
                recipes.add(recipe);
            }
        }
        
        return recipes;
    }
    
    /**
     * 检查元素组合的兼容性
     * @param elements 要检查的元素列表
     * @return 兼容性报告
     */
    @Nonnull
    public static CompatibilityReport checkCompatibility(@Nonnull List<ZhenType> elements) {
        return new CompatibilityReport(elements);
    }
    
    /**
     * 兼容性报告类
     */
    public static class CompatibilityReport {
        private final List<ZhenType> elements;
        private final boolean compatible;
        private final List<String> issues;
        private final List<String> suggestions;
        
        public CompatibilityReport(List<ZhenType> elements) {
            this.elements = new ArrayList<>(elements);
            this.issues = new ArrayList<>();
            this.suggestions = new ArrayList<>();
            this.compatible = analyzeCompatibility();
        }
        
        private boolean analyzeCompatibility() {
            if (elements.size() < 2) {
                issues.add("至少需要两个元素才能进行组合");
                return false;
            }
            
            // 检查层级差异
            Set<ElementTier> tiers = elements.stream()
                .map(element -> getElementInfo(element))
                .filter(Objects::nonNull)
                .map(ElementInfo::getTier)
                .collect(Collectors.toSet());
            
            if (tiers.size() > 2) {
                issues.add("元素层级差异过大，建议使用相近层级的元素");
            }
            
            // 检查分类兼容性
            Set<ElementCategory> categories = elements.stream()
                .map(element -> getElementInfo(element))
                .filter(Objects::nonNull)
                .map(ElementInfo::getCategory)
                .collect(Collectors.toSet());
            
            if (categories.contains(ElementCategory.CREATION) && categories.size() > 1) {
                suggestions.add("创造元素与其他元素组合可能产生意想不到的效果");
            }
            
            return issues.isEmpty();
        }
        
        public List<ZhenType> getElements() { return new ArrayList<>(elements); }
        public boolean isCompatible() { return compatible; }
        public List<String> getIssues() { return new ArrayList<>(issues); }
        public List<String> getSuggestions() { return new ArrayList<>(suggestions); }
    }
}