package com.yhzcake.magicio.block;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.zhen.ZhenBlock;
import com.yhzcake.magicio.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MagicIO.MOD_ID);

    public static final DeferredBlock<Block> TEST_BLOCK =
            registerBlocks("test_block", () -> new Block(BlockBehaviour.Properties.of().strength(1.5F, 6.0F)));
    public static final DeferredBlock<ZhenBlock> ZHEN_BLOCK =
            registerBlocks("zhen_block", () -> new ZhenBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_WIRE)));

    private static <T extends Block> void registerBlockItems(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    private static <T extends Block> DeferredBlock<T> registerBlocks(String name, Supplier<T> block) {
        DeferredBlock<T> blocks = BLOCKS.register(name, block);
        registerBlockItems(name, blocks);
        return blocks;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}