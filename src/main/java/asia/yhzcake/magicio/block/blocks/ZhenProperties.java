package asia.yhzcake.magicio.block.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 扩展的方块属性类，支持阵法类型
 * 使用方式: ZhenProperties.of().zhenType(ZhenType.WIND)
 */
public class ZhenProperties {
    private final BlockBehaviour.Properties properties;
    private ZhenType zhenType = ZhenTypeRegistry.getDefaultType(); // 使用默认阵法类型

    protected ZhenProperties(BlockBehaviour.Properties properties) {
        this.properties = properties;
    }

    /**
     * 创建一个新的 ZhenProperties 实例
     * @return 新的 ZhenProperties 实例
     */
    public static ZhenProperties of() {
        return new ZhenProperties(BlockBehaviour.Properties.of());
    }

    /**
     * 从现有的 BlockBehaviour.Properties 创建 ZhenProperties
     * @param properties 现有的属性
     * @return 新的 ZhenProperties 实例
     */
    public static ZhenProperties from(BlockBehaviour.Properties properties) {
        return new ZhenProperties(properties);
    }

    /**
     * 设置阵法类型
     * @param type 阵法类型
     * @return 当前实例，支持链式调用
     */
    public ZhenProperties zhenType(ZhenType type) {
        this.zhenType = type;
        return this;
    }

    /**
     * 获取阵法类型
     * @return 阵法类型
     */
    public ZhenType getZhenType() {
        return this.zhenType;
    }

    /**
     * 获取内部的 BlockBehaviour.Properties 实例
     * @return BlockBehaviour.Properties 实例
     */
    public BlockBehaviour.Properties getProperties() {
        return this.properties;
    }

    // 委托方法，支持链式调用

    public ZhenProperties destroyTime(float destroyTime) {
        this.properties.destroyTime(destroyTime);
        return this;
    }

    public ZhenProperties explosionResistance(float explosionResistance) {
        this.properties.explosionResistance(explosionResistance);
        return this;
    }

    public ZhenProperties sound(net.minecraft.world.level.block.SoundType sound) {
        this.properties.sound(sound);
        return this;
    }

    public ZhenProperties lightLevel(java.util.function.ToIntFunction<net.minecraft.world.level.block.state.BlockState> lightLevel) {
        this.properties.lightLevel(lightLevel);
        return this;
    }

    public ZhenProperties strength(float strength) {
        this.properties.strength(strength);
        return this;
    }

    public ZhenProperties instabreak() {
        this.properties.instabreak();
        return this;
    }

    public ZhenProperties strength(float destroyTime, float explosionResistance) {
        this.properties.strength(destroyTime, explosionResistance);
        return this;
    }

    public ZhenProperties noCollission() {
        this.properties.noCollission();
        return this;
    }

    public ZhenProperties noOcclusion() {
        this.properties.noOcclusion();
        return this;
    }

    public ZhenProperties friction(float friction) {
        this.properties.friction(friction);
        return this;
    }

    public ZhenProperties speedFactor(float speedFactor) {
        this.properties.speedFactor(speedFactor);
        return this;
    }

    public ZhenProperties jumpFactor(float jumpFactor) {
        this.properties.jumpFactor(jumpFactor);
        return this;
    }

    public ZhenProperties randomTicks() {
        this.properties.randomTicks();
        return this;
    }

    public ZhenProperties dynamicShape() {
        this.properties.dynamicShape();
        return this;
    }

    public ZhenProperties lootFrom(net.minecraft.world.level.block.Block block) {
        this.properties.lootFrom(() -> block);
        return this;
    }

    public ZhenProperties lootFrom(java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block) {
        this.properties.lootFrom(block);
        return this;
    }

    public ZhenProperties air() {
        this.properties.air();
        return this;
    }

    public ZhenProperties ignitedByLava() {
        this.properties.ignitedByLava();
        return this;
    }

    public ZhenProperties liquid() {
        this.properties.liquid();
        return this;
    }

    /**
     * 强制方块为固体状态
     * 注意：forceSolidOn() 在新版本中仍然可用
     * @return 当前实例，支持链式调用
     */
    public ZhenProperties forceSolidOn() {
        this.properties.forceSolidOn();
        return this;
    }

    /**
     * 强制方块为非固体状态 (已废弃)
     * @deprecated 此方法已被废弃，建议通过重写方块的 getCollisionShape() 方法来控制碰撞形状
     * @return 当前实例，支持链式调用
     */
    @Deprecated
    public ZhenProperties forceSolidOff() {
        this.properties.forceSolidOff();
        return this;
    }

    public ZhenProperties pushReaction(net.minecraft.world.level.material.PushReaction pushReaction) {
        this.properties.pushReaction(pushReaction);
        return this;
    }

    public ZhenProperties requiresCorrectToolForDrops() {
        this.properties.requiresCorrectToolForDrops();
        return this;
    }

    public ZhenProperties mapColor(net.minecraft.world.level.material.MapColor color) {
        this.properties.mapColor(color);
        return this;
    }

    public ZhenProperties mapColor(java.util.function.Function<net.minecraft.world.level.block.state.BlockState, net.minecraft.world.level.material.MapColor> mapColor) {
        this.properties.mapColor(mapColor);
        return this;
    }

    public ZhenProperties replaceable() {
        this.properties.replaceable();
        return this;
    }
}