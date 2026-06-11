package cn.yhzcake.magicio.common.data.gen;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.common.registry.MIOCommonBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class MIOChineseProvider extends LanguageProvider {
    public MIOChineseProvider(PackOutput output) {
        super(output, MagicIO.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add(MIOCommonBlocks.MATRIX_BLOCK.get(), "法阵方块");
    }
}
