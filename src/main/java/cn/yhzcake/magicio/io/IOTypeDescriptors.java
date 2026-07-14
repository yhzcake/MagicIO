package cn.yhzcake.magicio.io;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.yhzcake.magicio.MagicIO;

/**
 * IOType 行为描述符注册表。
 * <p>
 * 每种 {@link IOType} 在此注册对应的 {@link IOTypeDescriptor}，
 * 供配方执行、事务验证和虚拟端口等统一调用。
 * 新增 IOType 时只需在此注册描述符，无需修改核心流程类。
 */
public class IOTypeDescriptors {

    private static final Map<IOType, IOTypeDescriptor> DESCRIPTORS = new HashMap<>();

    /** 已注册的 IOType 集合（用于迭代）。 */
    private static volatile Set<IOType> registeredTypes = Set.of();

    private IOTypeDescriptors() {}

    /**
     * 注册一个 IOType 的描述符。
     *
     * @param descriptor 描述符实现
     */
    public static void register(IOTypeDescriptor descriptor) {
        IOType type = descriptor.ioType();
        if (type == null) {
            MagicIO.LOGGER.warn("IOTypeDescriptor registered with null ioType, ignored");
            return;
        }
        if (DESCRIPTORS.put(type, descriptor) != null) {
            MagicIO.LOGGER.warn("Overriding existing IOTypeDescriptor for {}", type);
        }
        registeredTypes = Set.copyOf(DESCRIPTORS.keySet());
        MagicIO.LOGGER.debug("Registered IOTypeDescriptor for {}", type);
    }

    /**
     * 获取指定 IOType 的描述符。
     *
     * @param type IOType
     * @return 描述符，未注册时返回 null
     */
    public static IOTypeDescriptor get(IOType type) {
        return DESCRIPTORS.get(type);
    }

    /**
     * 判断指定 IOType 是否已注册描述符。
     */
    public static boolean hasDescriptor(IOType type) {
        return DESCRIPTORS.containsKey(type);
    }

    /**
     * 获取当前所有已注册的 IOType。
     */
    public static Set<IOType> getRegisteredTypes() {
        return registeredTypes;
    }

    /**
     * 注册内置的 IOType 描述符（ITEM、FLUID 和 ENERGY）。
     * 在模组初始化阶段调用。
     */
    public static void registerBuiltin() {
        if (!hasDescriptor(ModIOTypes.ITEM.get())) {
            register(new ItemDescriptor());
        }
        if (!hasDescriptor(ModIOTypes.FLUID.get())) {
            register(new FluidDescriptor());
        }
        if (!hasDescriptor(ModIOTypes.ENERGY.get())) {
            register(new EnergyDescriptor());
        }
    }
}
