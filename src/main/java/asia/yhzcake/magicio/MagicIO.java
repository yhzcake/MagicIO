package asia.yhzcake.magicio;

import asia.yhzcake.magicio.block.Blocks;
import asia.yhzcake.magicio.block.blocks.ZhenBlock;
import asia.yhzcake.magicio.block.blocks.ZhenTypes;
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
    
    // 初始化阵法类型
    ZhenTypes.init();
    
    // 注册阵法类型
    ZhenBlock.registriesZhenType();

    Blocks.BLOCKS.register(modBus);
    Items.ITEMS.register(modBus);
    Items.CREATIVE_MODE_TABS.register(modBus);
    Blocks.ZHEN_ENTITY.register(modBus);
  }
}
