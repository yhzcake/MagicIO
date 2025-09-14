package asia.yhzcake.magicio.block.blocks;

import asia.yhzcake.magicio.MagicIO;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Objects;

/**
 * 阵法类型注册器
 * 管理所有已注册的阵法类型，支持动态扩展
 */
public class ZhenTypeRegistry {
    private static final Map<String, ZhenType> REGISTERED_TYPES = new HashMap<>();
    private static ZhenType defaultType = null;
    
    // 元素组合关系映射
    private static final Map<String, ZhenType> COMBINATIONS = new HashMap<>();
    
    /**
     * 注册一个新的阵法类型
     * @param zhenType 要注册的阵法类型
     * @return 注册的阵法类型实例
     * @throws IllegalArgumentException 如果ID已存在
     */
    public static ZhenType register(ZhenType zhenType) {
        Objects.requireNonNull(zhenType, "ZhenType cannot be null");
        
        String id = zhenType.getId();
        if (REGISTERED_TYPES.containsKey(id)) {
            MagicIO.LOGGER.warn("ZhenType with id '{}' is already registered, skipping", id);
            return REGISTERED_TYPES.get(id);
        }
        
        REGISTERED_TYPES.put(id, zhenType);
        MagicIO.LOGGER.info("Registered ZhenType: {} ({})", id, zhenType.getDisplayName());
        return zhenType;
    }

    /**
     * 根据ID获取阵法类型
     * @param id 阵法类型ID
     * @return 对应的ZhenType，如果未找到则返回默认类型
     */
    public static ZhenType getById(String id) {
        if (id == null || id.isEmpty()) {
            return getDefaultType();
        }
        return REGISTERED_TYPES.getOrDefault(id, getDefaultType());
    }

    /**
     * 检查指定ID的阵法类型是否已注册
     * @param id 阵法类型ID
     * @return 如果已注册返回true，否则返回false
     */
    public static boolean isRegistered(String id) {
        return REGISTERED_TYPES.containsKey(id);
    }

    /**
     * 获取所有已注册的阵法类型
     * @return 所有已注册的阵法类型集合
     */
    public static Collection<ZhenType> getAllTypes() {
        return REGISTERED_TYPES.values();
    }

    /**
     * 获取默认阵法类型
     * @return 默认阵法类型
     */
    public static ZhenType getDefaultType() {
        return defaultType;
    }

    /**
     * 设置默认阵法类型
     * @param type 要设置为默认的阵法类型
     */
    public static void setDefaultType(ZhenType type) {
        if (type != null && REGISTERED_TYPES.containsValue(type)) {
            defaultType = type;
            MagicIO.LOGGER.info("Default ZhenType set to: {}", type.getId());
        }
    }

    /**
     * 获取已注册阵法类型的数量
     * @return 已注册的阵法类型数量
     */
    public static int getRegisteredCount() {
        return REGISTERED_TYPES.size();
    }
    
    /**
     * 添加二元素组合关系
     * @param element1 第一个元素
     * @param element2 第二个元素
     * @param result 组合结果
     */
    public static void addCombination(ZhenType element1, ZhenType element2, ZhenType result) {
        String key1 = generateCombinationKey(element1.getId(), element2.getId());
        String key2 = generateCombinationKey(element2.getId(), element1.getId());
        COMBINATIONS.put(key1, result);
        COMBINATIONS.put(key2, result); // 组合关系是可交换的
    }
    
    /**
     * 添加三元素组合关系
     * @param element1 第一个元素
     * @param element2 第二个元素
     * @param element3 第三个元素
     * @param result 组合结果
     */
    public static void addTripleCombination(ZhenType element1, ZhenType element2, ZhenType element3, ZhenType result) {
        String key = generateTripleCombinationKey(element1.getId(), element2.getId(), element3.getId());
        COMBINATIONS.put(key, result);
    }
    
    /**
     * 生成二元素组合键
     */
    private static String generateCombinationKey(String id1, String id2) {
        return id1.compareTo(id2) < 0 ? id1 + "+" + id2 : id2 + "+" + id1;
    }
    
    /**
     * 生成三元素组合键
     */
    private static String generateTripleCombinationKey(String id1, String id2, String id3) {
        String[] ids = {id1, id2, id3};
        java.util.Arrays.sort(ids);
        return String.join("+", ids);
    }
    
    /**
     * 尝试组合两个元素
     * @param element1 第一个元素
     * @param element2 第二个元素
     * @return 组合结果，如果没有匹配的组合则返回null
     */
    public static ZhenType combineElements(ZhenType element1, ZhenType element2) {
        if (element1 == null || element2 == null) {
            return null;
        }
        
        String key = generateCombinationKey(element1.getId(), element2.getId());
        ZhenType result = COMBINATIONS.get(key);
        
        if (result != null) {
            MagicIO.LOGGER.info("元素组合成功: {} + {} = {}", 
                element1.getDisplayName(), element2.getDisplayName(), result.getDisplayName());
        }
        
        return result;
    }
    
    /**
     * 尝试组合三个元素
     * @param element1 第一个元素
     * @param element2 第二个元素
     * @param element3 第三个元素
     * @return 组合结果，如果没有匹配的组合则返回null
     */
    public static ZhenType combineElements(ZhenType element1, ZhenType element2, ZhenType element3) {
        if (element1 == null || element2 == null || element3 == null) {
            return null;
        }
        
        String key = generateTripleCombinationKey(element1.getId(), element2.getId(), element3.getId());
        ZhenType result = COMBINATIONS.get(key);
        
        if (result != null) {
            MagicIO.LOGGER.info("三元素组合成功: {} + {} + {} = {}", 
                element1.getDisplayName(), element2.getDisplayName(), element3.getDisplayName(), result.getDisplayName());
        }
        
        return result;
    }
    
    /**
     * 检查两个元素是否可以组合
     * @param element1 第一个元素
     * @param element2 第二个元素
     * @return 如果可以组合返回true，否则返回false
     */
    public static boolean canCombine(ZhenType element1, ZhenType element2) {
        if (element1 == null || element2 == null) {
            return false;
        }
        
        String key = generateCombinationKey(element1.getId(), element2.getId());
        return COMBINATIONS.containsKey(key);
    }
    
    /**
     * 检查三个元素是否可以组合
     * @param element1 第一个元素
     * @param element2 第二个元素
     * @param element3 第三个元素
     * @return 如果可以组合返回true，否则返回false
     */
    public static boolean canCombine(ZhenType element1, ZhenType element2, ZhenType element3) {
        if (element1 == null || element2 == null || element3 == null) {
            return false;
        }
        
        String key = generateTripleCombinationKey(element1.getId(), element2.getId(), element3.getId());
        return COMBINATIONS.containsKey(key);
    }
    
    /**
     * 获取所有可能的组合关系
     * @return 组合关系映射
     */
    public static Map<String, ZhenType> getAllCombinations() {
        return new HashMap<>(COMBINATIONS);
    }
    
    /**
     * 清空所有注册的阵法类型（主要用于测试）
     */
    public static void clear() {
        REGISTERED_TYPES.clear();
        COMBINATIONS.clear();
        defaultType = null;
        MagicIO.LOGGER.warn("All ZhenTypes have been cleared from registry");
    }
}