package asia.yhzcake.magicio.item;

import asia.yhzcake.magicio.MagicIO;
import asia.yhzcake.magicio.block.Blocks;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Items {
  public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicIO.MOD_ID);
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagicIO.MOD_ID);

  public static final Supplier<Item> EXAMPLE_ITEM =
      ITEMS.registerItem(
          "example_item",
          Item::new, // The factory that the properties will be passed into.
          new Item.Properties() // The properties to use.
          );
  //  public static final Supplier<Item> EXAMPLE_ITEM1 = ITEMS.registerItem("example_item1",
  // Item::new);
  // CREATIVE_MODE_TABS is a DeferredRegister<CreativeModeTab>
  public static final Supplier<CreativeModeTab> EXAMPLE_TAB =
      CREATIVE_MODE_TABS.register(
          "example",
          () ->
              CreativeModeTab.builder()
                  // Set the title of the tab. Don't forget to add a translation!
                  .title(Component.translatable("itemGroup." + MagicIO.MOD_ID + ".example"))
                  // Set the icon of the tab.
                  .icon(() -> new ItemStack(EXAMPLE_ITEM.get()))
                  // Add your items to the tab.
                  .displayItems(
                      (params, output) -> {
                        output.accept(EXAMPLE_ITEM.get());
                        // Accepts an ItemLike. This assumes that MY_BLOCK has a corresponding item.
                        output.accept(Blocks.ZHEN_BLOCK.get());
                      })
                  .build());
  // Variant that also omits the Item::new parameter
  //  public static final Supplier<Item> EXAMPLE_ITEM2 = ITEMS.registerSimpleItem("example_item2");
  public static final Supplier<BlockItem> EXAMPLE_BLOCK_ITEM =
      ITEMS.registerSimpleBlockItem("wind_zhen_block", Blocks.ZHEN_BLOCK, new Item.Properties());
  // Variant that omits the properties parameter:
  //  public static final Supplier<BlockItem> EXAMPLE_BLOCK_ITEM1 =
  //      ITEMS.registerSimpleBlockItem("example_block", Blocks.MY_BETTER_BLOCK);
  // Variant that omits the name parameter, instead using the block's registry name:
  //  public static final Supplier<BlockItem> EXAMPLE_BLOCK_ITEM2 =
  //      ITEMS.registerSimpleBlockItem(Blocks.MY_BETTER_BLOCK, new Item.Properties());
  // Variant that omits both the name and the properties:
  //  public static final Supplier<BlockItem> EXAMPLE_BLOCK_ITEM3 =
  //      ITEMS.registerSimpleBlockItem(Blocks.MY_BETTER_BLOCK);
}
