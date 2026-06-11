package cn.yhzcake.magicio.common.data.gen.sub;

import cn.yhzcake.magicio.common.data.gen.MIOModelProvider;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.data.PackOutput;

/**
 * 为繁杂物品单独分类的子生成器
 */
public class MIOItemModelSubProvider extends MIOModelProvider {
    public MIOItemModelSubProvider(PackOutput output) {
        super(output);
    }

    protected void registerModels(ItemModelGenerators itemModels) {

    }

    @Override
    public String getName() {
        return "Item " + super.getName();
    }
}
