package cn.yhzcake.magicio.common.data.gen;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class MIOBlockTagsProvider extends BlockTagsProvider {
    public MIOBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, MagicIO.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {

    }
}
