package cn.yhzcake.magicio.block.gridcell;

import java.util.function.Supplier;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.zhen.ZhenFunctions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 注册内置的 GridParseRule 条目。
 * 使用 {@link GridPattern} 构建匹配器。
 */
public class ModGridParseRules {

    public static final DeferredRegister<GridParseRule> GRID_PARSE_RULES =
            DeferredRegister.create(GridParseRule.GRID_PARSE_RULE_REGISTRY_KEY, MagicIO.MOD_ID);

    /**
     * 示例规则：使用 GridPattern 匹配。
     * 配置语义：dir=1（左上角起始），sharp 模式，4×4 全为 FIRE。
     */
    public static Supplier<GridParseRule> UNSTABLE_SIFT_ZHEN;

    public static void register(IEventBus eventBus) {
        UNSTABLE_SIFT_ZHEN = GRID_PARSE_RULES.register("unstable_sift_zhen", () -> new GridParseRule(
                () -> ZhenFunctions.UNSTABLE_SIEVE_ZHEN.get(),
                // 使用 GridPattern 构建匹配器
                GridPattern.builder(5, GridPattern.MatchType.SHARP, new String[][]{
                        {"a", "a"},
                        {"a", "a"}
                }).tag("a", ModCellActions.EARTH).buildMatcher(),
                // handler：占位，后续实现具体逻辑
                ctx -> {
                    MagicIO.LOGGER.info("GridParseRule UNSTABLE_SIFT_ZHEN matched at {}", ctx.pos());
                }
        ));

        GRID_PARSE_RULES.register(eventBus);
    }
}
