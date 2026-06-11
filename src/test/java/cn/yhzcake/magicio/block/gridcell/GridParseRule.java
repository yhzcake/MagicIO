package cn.yhzcake.magicio.block.gridcell;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * 网格解析规则 — 自定义注册表。
 * 每条规则包含一个匹配条件（matcher）和一个执行逻辑（handler）。
 * 当 4×4 网格全部填满后，按注册顺序遍历，执行第一个匹配的规则。
 */
public class GridParseRule {

    public static final ResourceKey<Registry<GridParseRule>> GRID_PARSE_RULE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "grid_parse_rule"));
    public static Registry<GridParseRule> GRID_PARSE_RULES;

    private final Supplier<ZhenType> zhenTypeSupplier;
    private final Predicate<CellAction[][]> matcher;
    private final Consumer<GridParseContext> handler;

    public GridParseRule(Supplier<ZhenType> zhenTypeSupplier,
                         Predicate<CellAction[][]> matcher,
                         Consumer<GridParseContext> handler) {
        this.zhenTypeSupplier = Objects.requireNonNull(zhenTypeSupplier, "zhenTypeSupplier is null");
        this.matcher = Objects.requireNonNull(matcher, "matcher is null");
        this.handler = Objects.requireNonNull(handler, "handler is null");
    }

    public ZhenType getZhenType() {
        return zhenTypeSupplier.get();
    }

    public Predicate<CellAction[][]> getMatcher() {
        return matcher;
    }

    public Consumer<GridParseContext> getHandler() {
        return handler;
    }

    /**
     * 测试此规则是否匹配当前网格。
     */
    public boolean matches(CellAction[][] grid) {
        return matcher.test(grid);
    }

    /**
     * 执行此规则的处理逻辑。
     */
    public void execute(GridParseContext context) {
        handler.accept(context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridParseRule that = (GridParseRule) o;
        return Objects.equals(zhenTypeSupplier.get(), that.zhenTypeSupplier.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(zhenTypeSupplier.get());
    }

    @Override
    public String toString() {
        return zhenTypeSupplier.get().toString();
    }

    @SubscribeEvent
    public static void register(NewRegistryEvent event) {
        GRID_PARSE_RULES = new RegistryBuilder<>(GRID_PARSE_RULE_REGISTRY_KEY)
                .defaultKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "empty"))
                .create();
        event.register(GRID_PARSE_RULES);
    }
}
