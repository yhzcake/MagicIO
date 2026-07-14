package cn.yhzcake.magicio.item.crafting;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.io.ModIOTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/**
 * 运行时桥接：将原版熔炉配方动态转换为 {@link ZhenRecipe} 并注入 {@link ZhenRecipeManager}。
 */
public class ForgeRecipeBridge {

    private static final String FORGE_TYPE = "magic_io:stable_forge_zhen";

    public static void injectFurnaceRecipes(MinecraftServer server) {
        if (server == null) {
            MagicIO.LOGGER.warn("[ForgeRecipeBridge] server is null, skipping furnace recipe injection");
            return;
        }

        // 1.21.5: 使用 getRecipes() 获取全部配方，按类型过滤
        var allRecipes = server.getRecipeManager().getRecipes();
        int count = 0;

        for (RecipeHolder<?> holder : allRecipes) {
            if (!(holder.value() instanceof SmeltingRecipe furnaceRecipe)) continue;
            ZhenRecipe zhenRecipe = convert(holder.id().identifier(), furnaceRecipe);
            if (zhenRecipe != null) {
                ZhenRecipeManager.getInstance().addRecipe(zhenRecipe);
                count++;
            }
        }

        MagicIO.LOGGER.info("[ForgeRecipeBridge] 已注入 {} 个熔炉配方到 {}", count, FORGE_TYPE);
    }

    private static ZhenRecipe convert(Identifier sourceId, SmeltingRecipe recipe) {
        // 原料 — SingleItemRecipe.input()
        var ingredient = recipe.input();
        if (ingredient.isEmpty()) return null;

        // 产出 — assemble() 返回 ItemStack（1.21.5: result() 是 protected）
        ItemStack result = recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
        if (result.isEmpty()) return null;

        // 组装输入
        NonNullList<ItemRequirement> inputList = NonNullList.create();
        inputList.add(new ItemRequirement(ingredient, 1));
        var recipeInput = new RecipeInput<>(ModIOTypes.ITEM.get(),
                SlotZone.ITEM_INPUT_ALL.getName(), inputList);

        // 组装输出
        NonNullList<OutputEntry> outputList = NonNullList.create();
        outputList.add(OutputEntry.item(result.copy()));
        var recipeOutput = new RecipeOutput<>(ModIOTypes.ITEM.get(),
                SlotZone.ITEM_OUTPUT_ALL.getName(), outputList);

        int processingTime = recipe.cookingTime();
        if (processingTime <= 0) processingTime = 200;

        return new ZhenRecipe(
                Identifier.fromNamespaceAndPath(MagicIO.MOD_ID,
                        "bridge/smelting/" + sourceId.getNamespace() + "/" + sourceId.getPath()),
                Identifier.parse(FORGE_TYPE),
                java.util.List.of(recipeInput),
                java.util.List.of(recipeOutput),
                processingTime
        );
    }
}
