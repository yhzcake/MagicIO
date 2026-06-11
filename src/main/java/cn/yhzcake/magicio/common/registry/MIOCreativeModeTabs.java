package cn.yhzcake.magicio.common.registry;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * @author MakerTechno
 * @since origin
 */
public class MIOCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicIO.MOD_ID);
}
