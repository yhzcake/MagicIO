package cn.yhzcake.magicio.mixin;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cn.yhzcake.magicio.MagicIO;

@Mixin(TitleScreen.class)
public class ExampleMixin {
    @Inject(at = @At("HEAD"), method = "init")
    private void init(CallbackInfo info) {
        MagicIO.LOGGER.info("MagicIO mixin successfully.");
    }
}
