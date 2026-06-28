package cn.yhzcake.magicio.block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.gridcell.GridCellPanelBlock;
import cn.yhzcake.magicio.block.zhen.ZhenBlock;
import cn.yhzcake.magicio.block.zhen.ZhenTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final Map<String, DeferredBlock<Block>> ZHEN_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> ZHEN_BLOCK_ITEMS = new LinkedHashMap<>();

    // ===== Grid Cell Panel =====

    public static final DeferredBlock<Block> GRID_CELL_PANEL =
            MagicIO.BLOCKS.registerBlock("grid_cell_panel",
                    p -> new GridCellPanelBlock(p.noOcclusion()));

    // ===== 阵方块注册 =====

    public static void registerZhenBlocks(DeferredRegister.Blocks blockRegister) {
        for (var entry : ZhenTypes.ZHEN_TYPES.getEntries()) {
            String name = entry.getId().getPath();
            ZHEN_BLOCKS.put(name, blockRegister.registerBlock(name, ZhenBlock::new));
        }
    }

    public static void registerZhenBlockItems(DeferredRegister.Items itemRegister) {
        for (var blockEntry : ZHEN_BLOCKS.entrySet()) {
            String name = blockEntry.getKey();
            var block = blockEntry.getValue();
            ZHEN_BLOCK_ITEMS.put(name, itemRegister.registerSimpleBlockItem(name, block));
        }
    }

    public static List<Block> getZhenBlockList() {
        return ZHEN_BLOCKS.values().stream().map(DeferredBlock::get).toList();
    }
}
