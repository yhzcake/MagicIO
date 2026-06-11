package cn.yhzcake.magicio.common.data.gen.sub;

import cn.yhzcake.magicio.common.data.gen.MIOModelProvider;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.data.PackOutput;

/**
 * 为繁杂方块单独分类的子生成器
 */
public class MIOBlockModelSubProvider extends MIOModelProvider {
    public MIOBlockModelSubProvider(PackOutput output) {
        super(output);
    }

    protected void registerModels(BlockModelGenerators blockModels) {

    }

    @Override
    public String getName() {
        return "Block " + super.getName();
    }
}
