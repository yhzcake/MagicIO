package cn.yhzcake.magicio.common.data.gen;

import cn.yhzcake.magicio.common.registry.MIOCommonBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MIOLootTableProvider extends LootTableProvider {
    public MIOLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(
                new SubProviderEntry(MIOBlockLootSubProvider::new, LootContextParamSets.BLOCK)
        ), registries);
    }

    public static class MIOBlockLootSubProvider extends BlockLootSubProvider {
        List<Block> blockList = MIOCommonBlocks.BLOCKS.getEntries()
                .stream()
                .map(e -> (Block) e.value())
                .toList();
        protected MIOBlockLootSubProvider(HolderLookup.Provider provider) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
        }
        @Override
        protected void generate() {
            for (Block block : blockList) {
                dropSelf(block);
            }
        }

        @Override
        protected @NotNull Iterable<Block> getKnownBlocks() {
            return MIOCommonBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::get).collect(Collectors.toUnmodifiableList());
        }
    }
}
