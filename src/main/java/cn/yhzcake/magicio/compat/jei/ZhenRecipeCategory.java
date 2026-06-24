package cn.yhzcake.magicio.compat.jei;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.ModBlocks;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.item.crafting.OutputEntry;
import cn.yhzcake.magicio.item.crafting.RecipeInput;
import cn.yhzcake.magicio.item.crafting.RecipeOutput;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.*;

public class ZhenRecipeCategory implements IRecipeCategory<ZhenRecipe> {

    private static final int COLS = 9;
    private static final int SLOT = 18;
    private static final int VISIBLE_ROWS = 3;

    private static final int WIDTH = COLS * SLOT + 2;   // 164
    private static final int INPUT_Y = 0;
    private static final int GRID_Y = INPUT_Y + SLOT;    // 18

    private final String baseName;
    private final IRecipeType<ZhenRecipe> recipeType;
    private final IDrawable icon;

    public ZhenRecipeCategory(IGuiHelper helper, String baseName, IRecipeType<ZhenRecipe> recipeType) {
        this.baseName = baseName;
        this.recipeType = recipeType;

        ItemStack iconStack = ItemStack.EMPTY;
        for (var entry : ModBlocks.ZHEN_BLOCKS.entrySet()) {
            if (MagicIOJeiPlugin.blockBaseName(entry.getKey()).equals(baseName)) {
                iconStack = new ItemStack(entry.getValue().get());
                break;
            }
        }
        this.icon = iconStack.isEmpty()
                ? helper.createDrawable(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "textures/item/unknown.png"), 0, 0, 16, 16)
                : helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public IRecipeType<ZhenRecipe> getRecipeType() { return recipeType; }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.magic_io.category." + baseName);
    }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public int getWidth() { return WIDTH; }

    @Override
    public int getHeight() { return GRID_Y + VISIBLE_ROWS * SLOT; } // 72

    @Override
    public Identifier getIdentifier(ZhenRecipe recipe) {
        return Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, recipe.getZhenTypeId().getPath());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ZhenRecipe recipe, IFocusGroup focuses) {
        // ===== 第一行：物品输入 → 流体输入（从左向右）=====
        int cx = 1;
        NonNullList<Ingredient> itemIngredients = recipe.getIngredients();
        int nIn = Math.min(itemIngredients.size(), 3);
        for (int i = 0; i < nIn; i++) {
            builder.addInputSlot(cx, INPUT_Y).add(itemIngredients.get(i)).setStandardSlotBackground();
            cx += SLOT;
        }
        if (nIn > 0) cx += 2;

        for (RecipeInput<?> input : recipe.getInputs()) {
            if (input.type() == ModIOTypes.FLUID.get()) {
                NonNullList<ZhenRecipe.FluidIngredient> fluids =
                        (NonNullList<ZhenRecipe.FluidIngredient>) input.requirement();
                for (var fi : fluids) {
                    int amount = fi.amount();
                    for (var holder : fi.fluids()) {
                        int a = amount;
                        builder.addSlot(RecipeIngredientRole.INPUT, cx, INPUT_Y)
                                .add(holder.value(), amount).setStandardSlotBackground()
                                .addRichTooltipCallback((v, t) ->
                                        t.add(Component.literal(a + " mB").withStyle(ChatFormatting.AQUA)));
                        cx += SLOT;
                    }
                }
            }
        }

        // ===== 第一行右侧：物品输出 → 流体输出（从右向左）=====
        int rx = WIDTH - 2 - SLOT;
        for (RecipeOutput<?> output : recipe.getOutputs()) {
            if (output.type() == ModIOTypes.FLUID.get()) {
                NonNullList<FluidStack> fluids = (NonNullList<FluidStack>) output.specification();
                for (int i = fluids.size() - 1; i >= 0; i--) {
                    FluidStack fs = fluids.get(i);
                    int amount = fs.getAmount();
                    builder.addSlot(RecipeIngredientRole.OUTPUT, rx, INPUT_Y)
                            .add(fs.getFluid(), amount).setStandardSlotBackground()
                            .addRichTooltipCallback((v, t) ->
                                    t.add(Component.literal(amount + " mB").withStyle(ChatFormatting.AQUA)));
                    rx -= SLOT;
                }
            }
        }
        NonNullList<ItemStack> itemOutputs = recipe.getFixedOutputs();
        if (!itemOutputs.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, rx, INPUT_Y)
                    .addItemStacks(itemOutputs).setStandardSlotBackground();
        }

        // ===== 战利品表槽位（全部放置，由 ScrollGrid 接管滚动）=====
        var lootItems = collectLootItems(recipe);
        for (int i = 0; i < lootItems.size(); i++) {
            var li = lootItems.get(i);

            var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 0, GRID_Y)
                    .addItemStacks(List.of(li.stack())).setStandardSlotBackground()
                    .setSlotName("loot_" + i);

            slot.addRichTooltipCallback((v, t) -> {
                for (var line : li.tooltipLines()) t.add(line);
            });
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ZhenRecipe recipe, IFocusGroup focuses) {
        // 用 findSlotByName 逐个收集 loot_n 槽位
        var slotView = builder.getRecipeSlots();
        List<mezz.jei.api.gui.ingredient.IRecipeSlotDrawable> lootDrawables = new ArrayList<>();
        int idx = 0;
        while (true) {
            var opt = slotView.findSlotByName("loot_" + idx);
            if (opt.isEmpty()) break;
            lootDrawables.add(opt.get());
            idx++;
        }

        if (!lootDrawables.isEmpty()) {
            var widget = builder.addScrollGridWidget(lootDrawables, COLS, VISIBLE_ROWS);
            widget.setPosition(1, GRID_Y);
        }
    }

    @Override
    public void draw(ZhenRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        guiGraphics.text(font, "\u279C", WIDTH / 2 - 4, INPUT_Y, 0xFF555555, false);
        int sec = Math.max(1, recipe.getProcessingTime() / 20);
        String timeStr = sec + "s";
        guiGraphics.text(font, timeStr, WIDTH / 2 - font.width(timeStr) / 2, INPUT_Y + 8, 0xFF888888, false);
    }

    // ====== 辅助方法 ======

    @SuppressWarnings("unchecked")
    private List<LootTableParser.LootDisplayItem> collectLootItems(ZhenRecipe recipe) {
        List<LootTableParser.LootDisplayItem> result = new ArrayList<>();
        for (RecipeOutput<?> output : recipe.getOutputs()) {
            if (output.type() == ModIOTypes.ITEM.get()) {
                for (OutputEntry entry : (NonNullList<OutputEntry>) output.specification()) {
                    if (entry.isLootTable() && entry.lootTableId() != null) {
                        result.addAll(LootTableParser.parse(entry.lootTableId()));
                    }
                }
            }
        }
        List<LootTableParser.LootDisplayItem> merged = new ArrayList<>();
        for (var li : result) {
            boolean found = false;
            for (int i = 0; i < merged.size(); i++) {
                var m = merged.get(i);
                if (m.stack().getItem() == li.stack().getItem()) {
                    merged.set(i, new LootTableParser.LootDisplayItem(m.stack(), m.expectedCount() + li.expectedCount(), m.tooltipLines()));
                    found = true; break;
                }
            }
            if (!found) merged.add(li);
        }
        return merged;
    }
}
