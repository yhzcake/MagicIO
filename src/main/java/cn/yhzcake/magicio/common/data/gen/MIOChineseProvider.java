package cn.yhzcake.magicio.common.data.gen;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.common.registry.MIOCommonBlocks;
import cn.yhzcake.magicio.common.registry.MIOElementTypes;
import cn.yhzcake.magicio.common.sys.matrix.ElementType;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class MIOChineseProvider extends LanguageProvider {
    public MIOChineseProvider(PackOutput output) {
        super(output, MagicIO.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add(MIOCommonBlocks.MATRIX_BLOCK.get(), "法阵方块");

        addElementType(MIOElementTypes.EMPTY, "空");

        addElementType(MIOElementTypes.EARTH, "地");
        addElementType(MIOElementTypes.WATER, "水");
        addElementType(MIOElementTypes.WIND, "风");
        addElementType(MIOElementTypes.FIRE, "火");

        addElementType(MIOElementTypes.WOOD, "木");
        addElementType(MIOElementTypes.ICE, "冰");
        addElementType(MIOElementTypes.METAL, "金");
        addElementType(MIOElementTypes.LIGHTNING, "雷");
        addElementType(MIOElementTypes.ORDER, "秩序");
        addElementType(MIOElementTypes.SPACE, "空间");
        addElementType(MIOElementTypes.TIME, "时间");
        addElementType(MIOElementTypes.CHAOS, "混沌");

        addElementType(MIOElementTypes.DESCRIPTION, "描述");
        addElementType(MIOElementTypes.ENERGY, "能量");
        addElementType(MIOElementTypes.CONSCIOUSNESS, "意识");

        addElementType(MIOElementTypes.CREATIVE, "创造");
    }
    public void addElementType(Holder<ElementType> typeHolder, String translate) {
       add(typeHolder.value().getDescriptionId(), translate);
    }
}
