package cn.yhzcake.magicio;

import cn.yhzcake.magicio.common.registry.MIOCommonBlocks;
import cn.yhzcake.magicio.common.registry.MIOCommonItems;
import cn.yhzcake.magicio.common.registry.MIORegistries;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MagicIO.MOD_ID)
public class MagicIO {
    public static final String MOD_ID = "magic_io";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MagicIO(IEventBus bus, ModContainer container) {
        MIORegistries.register(bus);
        MIOCommonBlocks.BLOCKS.register(bus);
        MIOCommonItems.ITEMS.register(bus);
    }
}
