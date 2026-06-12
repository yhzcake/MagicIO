package cn.yhzcake.magicio.common.data.gen;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.common.registry.MIOElementTypes;
import cn.yhzcake.magicio.common.sys.matrix.ElementType;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Objects;

public class MIOEnglishProvider extends LanguageProvider {
    public MIOEnglishProvider(PackOutput output) {
        super(output, MagicIO.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        for (Holder<ElementType> typeHolder : MIOElementTypes.ELEMENT_TYPES.getEntries()) {
            add(typeHolder.value().getDescriptionId(), capitalize(Objects.requireNonNull(typeHolder.getKey()).identifier().getPath()));
        }
    }

    public static String capitalize(String str) {
        if (str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
