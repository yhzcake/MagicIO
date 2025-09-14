package asia.yhzcake.magicio.block.blocks;

import asia.yhzcake.magicio.MagicIO;

/**
 * 阵法类型定义和初始化类
 * 统一管理所有内置阵法类型的注册
 */
public class ZhenTypes {
    
    // ========== 基础元素（四大基本元素） ==========
    public static final ZhenType EARTH = new ZhenType("earth", "土阵", "土元素操控小分子固体（无机物）类似:筛选机,离心机");
    public static final ZhenType WATER = new ZhenType("water", "水阵", "水元素操控流体（液体、气体）类似:储罐");
    public static final ZhenType FIRE = new ZhenType("fire", "火阵", "火元素操控内能（守恒，只能转移）类似:熔炉,高炉");
    public static final ZhenType WIND = new ZhenType("wind", "风阵", "风元素操控机械能（守恒，只能机械能内转换）类似:粉碎机");
    
    // ========== 二级组合元素 ==========
    public static final ZhenType WOOD = new ZhenType("wood", "木阵", "木元素操控大分子固体（有机物）类似:发酵室");
    public static final ZhenType FREEZE = new ZhenType("freeze", "冻阵", "冻元素操控变化（膨胀收缩、物态变化）");
    public static final ZhenType METAL = new ZhenType("metal", "金阵", "金元素操控微观集团（炼金术，无机物）类似:反应釜");
    public static final ZhenType LIGHTNING = new ZhenType("lightning", "雷阵", "雷元素操控电磁力（包括宏观、微观）类似:两级磁化机，发电机");
    
    // ========== 三级组合元素 ==========
    public static final ZhenType ORDER = new ZhenType("order", "秩序阵", "秩序元素操控运行（顺序，过滤，轮询）");
    public static final ZhenType SPACE = new ZhenType("space", "空间阵", "空间元素操控引力（压缩空间，并行并发）");
    public static final ZhenType TIME = new ZhenType("time", "时间阵", "时间元素操控进程（加速）");
    public static final ZhenType CHAOS = new ZhenType("chaos", "混沌阵", "混沌元素操控随机（概率）无视概率的机器升级之类的");
    
    // ========== 四级组合元素 ==========
    public static final ZhenType ENERGY = new ZhenType("energy", "能量阵", "能量元素操控四大基本力（维持阵法稳定等）");
    public static final ZhenType DESCRIPTION = new ZhenType("description", "描述阵", "描述元素操控信息");
    
    // ========== 五级终极元素 ==========
    public static final ZhenType CREATION = new ZhenType("creation", "创造阵", "创造元素操控一切");
    public static final ZhenType CONSCIOUSNESS = new ZhenType("consciousness", "意识阵", "意识元素操控灵魂（含生物信息的能量团，刷怪笼）");
    
    /**
     * 初始化所有阵法类型
     * 这个方法应该在模组加载时调用
     */
    public static void init() {
        MagicIO.LOGGER.info("开始初始化阵法类型...");
        
        // 注册基础元素
        ZhenTypeRegistry.register(EARTH);
        ZhenTypeRegistry.register(WATER);
        ZhenTypeRegistry.register(FIRE);
        ZhenTypeRegistry.register(WIND);
        
        // 注册二级组合元素
        ZhenTypeRegistry.register(WOOD);
        ZhenTypeRegistry.register(FREEZE);
        ZhenTypeRegistry.register(METAL);
        ZhenTypeRegistry.register(LIGHTNING);
        
        // 注册三级组合元素
        ZhenTypeRegistry.register(ORDER);
        ZhenTypeRegistry.register(SPACE);
        ZhenTypeRegistry.register(TIME);
        ZhenTypeRegistry.register(CHAOS);
        
        // 注册四级组合元素
        ZhenTypeRegistry.register(ENERGY);
        ZhenTypeRegistry.register(DESCRIPTION);
        
        // 注册五级终极元素
        ZhenTypeRegistry.register(CREATION);
        ZhenTypeRegistry.register(CONSCIOUSNESS);
        
        // 设置默认类型为风阵
        ZhenTypeRegistry.setDefaultType(WIND);
        
        // 初始化元素组合关系
        initializeElementCombinations();
        
        MagicIO.LOGGER.info("阵法类型初始化完成。总计类型数: {}", 
            ZhenTypeRegistry.getRegisteredCount());
    }
    
    /**
     * 初始化元素组合关系
     * 根据设计的元素进化树建立组合关系
     */
    private static void initializeElementCombinations() {
        // 二级组合：基础元素 + 基础元素 = 二级元素
        ZhenTypeRegistry.addCombination(EARTH, WATER, WOOD);     // 土 + 水 = 木
        ZhenTypeRegistry.addCombination(WATER, WIND, FREEZE);    // 水 + 风 = 冻
        ZhenTypeRegistry.addCombination(FIRE, EARTH, METAL);     // 火 + 土 = 金
        ZhenTypeRegistry.addCombination(WIND, FIRE, LIGHTNING);  // 风 + 火 = 雷
        
        // 三级组合：二级元素 + 基础/二级元素 = 三级元素
        ZhenTypeRegistry.addCombination(WOOD, METAL, ORDER);     // 木 + 金 = 秩序
        ZhenTypeRegistry.addCombination(FREEZE, WOOD, SPACE);    // 冻 + 木 = 空间
        ZhenTypeRegistry.addCombination(METAL, LIGHTNING, TIME); // 金 + 雷 = 时间
        ZhenTypeRegistry.addCombination(LIGHTNING, FREEZE, CHAOS); // 雷 + 冻 = 混沌
        
        // 四级组合：三级元素 + 三级元素 = 四级元素
        ZhenTypeRegistry.addCombination(TIME, SPACE, ENERGY);    // 时间 + 空间 = 能量
        ZhenTypeRegistry.addCombination(CHAOS, ORDER, DESCRIPTION); // 混沌 + 秩序 = 描述
        
        // 五级组合：四级元素 + 四级元素 = 五级元素
        ZhenTypeRegistry.addCombination(DESCRIPTION, ENERGY, CREATION); // 描述 + 能量 = 创造
        
        // 特殊组合：三元素组合
        ZhenTypeRegistry.addTripleCombination(LIGHTNING, EARTH, ENERGY, CONSCIOUSNESS); // 雷 + 土 + 能量 = 意识
        
        MagicIO.LOGGER.info("元素组合关系初始化完成，共有 {} 个组合", ZhenTypeRegistry.getAllCombinations().size());
    }
}