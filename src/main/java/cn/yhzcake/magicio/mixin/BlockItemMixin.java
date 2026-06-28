package cn.yhzcake.magicio.mixin;

import cn.yhzcake.magicio.block.zhen.ZhenBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让 BlockItem 的 getName() 委托给 getBlock().getName()，这样 ZhenBlock 的 getName() 覆写对物品也生效。
 * 目标选 Item.class，因为 getName(ItemStack) 在 Item 中声明，BlockItem 未覆写。
 */
@Mixin(Item.class)
public abstract class BlockItemMixin {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void magicio$delegateToBlockName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        if (!(((Object) this) instanceof BlockItem blockItem)) return;
        Block block = blockItem.getBlock();
        if (block instanceof ZhenBlock) {
            cir.setReturnValue(block.getName());
        }
    }
}
