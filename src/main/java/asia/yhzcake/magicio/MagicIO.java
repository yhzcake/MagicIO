package asia.yhzcake.magicio;

import asia.yhzcake.magicio.block.Blocks;
import asia.yhzcake.magicio.block.blocks.method.ZhenHash;
import asia.yhzcake.magicio.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MagicIO.MOD_ID)
public class MagicIO {
  public static final String MOD_ID = "magicio";
  public static final Logger LOGGER;

  static {
    LOGGER = LoggerFactory.getLogger(MOD_ID);
  }

  public MagicIO(IEventBus modBus) {
    LOGGER.info("MagicIO has been initialized");
    ZhenHash.defaultMethod();
    Blocks.BLOCKS.register(modBus);
    Items.ITEMS.register(modBus);
    Items.CREATIVE_MODE_TABS.register(modBus);
    Blocks.ZHEN_ENTITY.register(modBus);
  }
}
