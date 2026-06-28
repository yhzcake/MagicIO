package cn.yhzcake.magicio.block.zhen;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import cn.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;
import cn.yhzcake.magicio.block.entity.ModBlockEntities;
import cn.yhzcake.magicio.block.entity.ZhenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ZhenBlock extends BaseEntityBlock {
    public static final MapCodec<ZhenBlock> CODEC = simpleCodec(ZhenBlock::new);

    public ZhenBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MutableComponent getName() {
        Identifier id = BuiltInRegistries.BLOCK.getKey(this);
        String path = id.getPath();
        // 去掉 _zhen 后缀
        if (!path.endsWith("_zhen")) {
            return super.getName();
        }
        String base = path.substring(0, path.length() - "_zhen".length());

        // 从等级前缀中解析
        for (ZhenLevel level : ZhenLevel.ALL) {
            String prefix = level.prefix();
            if (base.startsWith(prefix)) {
                String typeName = base.substring(prefix.length());
                return Component.translatable("magic_io.zhen_name",
                        Component.translatable("magic_io.level." + prefix.substring(0, prefix.length() - 1)),
                        Component.translatable("magic_io.zhen_type." + typeName));
            }
        }

        return super.getName();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZhenBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack destroyedWith) {
        if (!level.isClientSide() && blockEntity instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                Block.popResource(level, pos, container.getItem(i));
            }
            container.clearContent();
        }
        super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.ZHEN_BLOCK.get(), AbstractZhenBlockEntity::serverTick);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
