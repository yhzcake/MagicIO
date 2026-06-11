package cn.yhzcake.magicio.common.data.gen;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.common.registry.MIOCommonBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;

/**
 * 目标端: 仅客户端
 * 数据生成: 方块模型Model、方块状态BlockState、物品模型Model
 */
public class MIOModelProvider extends ModelProvider {
    public MIOModelProvider(PackOutput output) {
        super(output, MagicIO.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockModels.createTrivialCube(MIOCommonBlocks.MATRIX_BLOCK.get());
    }
}
