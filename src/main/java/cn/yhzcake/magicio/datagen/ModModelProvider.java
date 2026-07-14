package cn.yhzcake.magicio.datagen;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.ModBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unused")
public class ModModelProvider extends ModelProvider {

    private static final Identifier MAGIC_CIRCLE_MODEL = Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "item/magic_circle");

    // 统一纹理材质
    private static final Material CIRCLE_MATERIAL = new Material(
            Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "block/magic_circle"));

    public ModModelProvider(PackOutput output) {
        super(output, MagicIO.MOD_ID);
    }

    @Override
    protected java.util.stream.Stream<? extends net.minecraft.core.Holder<Block>> getKnownBlocks() {
        return java.util.stream.Stream.of();
    }

    @Override
    protected java.util.stream.Stream<? extends net.minecraft.core.Holder<Item>> getKnownItems() {
        return java.util.stream.Stream.of();
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // 不调用 blockModels.run()——zhen block 使用 RenderShape.INVISIBLE + BER

        // 用统一法阵模型注册所有物品
        registerItem(itemModels, MagicIO.COAL_COKE.get());
        registerItem(itemModels, MagicIO.EXAMPLE_ITEM.get());
        registerItem(itemModels, MagicIO.ZHEN_BUS_ITEM.get());
        registerItem(itemModels, MagicIO.GRID_CELL_PANEL_ITEM.get());

        for (var entry : ModBlocks.ZHEN_BLOCK_ITEMS.entrySet()) {
            registerItem(itemModels, entry.getValue().get());
        }
    }

    /** 注册物品为统一法阵图标 */
    private static void registerItem(ItemModelGenerators itemModels, Item item) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        // 创建物品模型 JSON（models/item/xxx.json）指向统一纹理
        var modelId = ModelTemplates.FLAT_ITEM.create(
                Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "item/" + itemId.getPath()),
                TextureMapping.layer0(CIRCLE_MATERIAL),
                itemModels.modelOutput);
        // 注册 items JSON 指向该模型
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(modelId));
    }
}
