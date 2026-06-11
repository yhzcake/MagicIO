package cn.yhzcake.magicio.common.data.gen;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class MIOEnglishProvider extends LanguageProvider {
    public MIOEnglishProvider(PackOutput output) {
        super(output, MagicIO.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
    }
}
