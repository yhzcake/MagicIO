package cn.yhzcake.magicio.block.zhen;

import java.util.function.Supplier;

import cn.yhzcake.magicio.utils.ElementTypes;
import cn.yhzcake.magicio.block.entity.method.PortalMethod;
import net.neoforged.bus.api.IEventBus;

/**
 * 所有阵功能的集中注册点。
 * <p>每个功能定义写一行，自动展开到所有可达等级。</p>
 * <p>需要外部引用的重点功能（如筛子）含有对应的 Supplier 字段。</p>
 */
public class ZhenFunctions {

    // ===== 外部引用的关键 Supplier =====
    /** 不稳定筛阵 — 被 GridParseRule 和 ZhenBusBlock 引用 */
    public static Supplier<ZhenType> UNSTABLE_SIEVE_ZHEN;

    // ===== Level 0 — 不稳定（通过元素缓慢聚集）=====
    public static final ZhenFunction CINDER   = ZhenFunction.simpleItem("cinder",   ElementTypes.FIRE, 0);
    public static final ZhenFunction SIEVE    = ZhenFunction.simpleItem("sieve", ElementTypes.EARTH, 0);
    public static final ZhenFunction DEW      = ZhenFunction.fluidItem("dew", ElementTypes.WATER, 0,1000);
    public static final ZhenFunction ZEPHYR   = ZhenFunction.simpleItem("zephyr",   ElementTypes.WIND, 0);

    // ===== Level 10 — 稳定（借用元素性质）=====
    public static final ZhenFunction FORGE    = ZhenFunction.simpleItem("forge",    ElementTypes.FIRE, 10);
    public static final ZhenFunction COMPACT  = ZhenFunction.simpleItem("compact",  ElementTypes.EARTH, 10);
    public static final ZhenFunction POTION   = ZhenFunction.simpleItem("potion",   ElementTypes.WATER, 10);
    public static final ZhenFunction CARVE    = ZhenFunction.simpleItem("carve",    ElementTypes.WIND, 10);
    public static final ZhenFunction SPROUT   = ZhenFunction.simpleItem("sprout",   ElementTypes.WOOD, 10);
    public static final ZhenFunction GRIND    = ZhenFunction.simpleItem("grind",    ElementTypes.METAL, 10);
    public static final ZhenFunction FROST    = ZhenFunction.simpleItem("frost",    ElementTypes.ICE, 10);
    public static final ZhenFunction VOLTAIC  = ZhenFunction.simpleItem("voltaic",  ElementTypes.LIGHTNING, 10);

    // ===== Level 20 — 稳固（借用元素能力）=====
    public static final ZhenFunction BLAZE    = ZhenFunction.simpleItem("blaze",    ElementTypes.FIRE, 20);
    public static final ZhenFunction GEM      = ZhenFunction.simpleItem("gem",      ElementTypes.EARTH, 20);
    public static final ZhenFunction SPRING   = ZhenFunction.simpleItem("spring",   ElementTypes.WATER, 20);
    public static final ZhenFunction WHIRL    = ZhenFunction.simpleItem("whirl",    ElementTypes.WIND, 20);
    public static final ZhenFunction FERMENT  = ZhenFunction.simpleItem("ferment",  ElementTypes.WOOD, 20);
    public static final ZhenFunction MOLD     = ZhenFunction.simpleItem("mold",     ElementTypes.METAL, 20);
    public static final ZhenFunction SHIFT    = ZhenFunction.simpleItem("shift",    ElementTypes.ICE, 20);
    public static final ZhenFunction THUNDER  = ZhenFunction.simpleItem("thunder",  ElementTypes.LIGHTNING, 20);

    // ===== Level 30 — 充盈（高阶元素）=====
    public static final ZhenFunction ENGRAVE    = ZhenFunction.simpleItem("engrave",    ElementTypes.FIRE, 30);
    public static final ZhenFunction QUAKE      = ZhenFunction.simpleItem("quake",      ElementTypes.EARTH, 30);
    public static final ZhenFunction SYMBIOSIS  = ZhenFunction.simpleItem("symbiosis",  ElementTypes.WOOD, 30);
    public static final ZhenFunction CONFLUX    = ZhenFunction.simpleItem("conflux",    ElementTypes.METAL, 30);
    public static final ZhenFunction SYNTHESIS  = ZhenFunction.simpleItem("synthesis",  ElementTypes.ORDER, 30);
    public static final ZhenFunction DISTILL    = ZhenFunction.simpleItem("distill",    ElementTypes.CHAOS, 30);
    public static final ZhenFunction GATE       = ZhenFunction.simpleItem("gate",       ElementTypes.SPACE, 30);
    public static final ZhenFunction DIVINE     = ZhenFunction.simpleItem("divine",     ElementTypes.DESCRIPTION, 30);

    // ===== Level 40 — 古朴（超级元素）=====
    public static final ZhenFunction WEAVE      = ZhenFunction.simpleItem("weave",      ElementTypes.ORDER, 40);
    public static final ZhenFunction FATE       = ZhenFunction.simpleItem("fate",       ElementTypes.CHAOS, 40);
    public static final ZhenFunction VOID       = ZhenFunction.simpleItem("void",       ElementTypes.SPACE, 40);
    public static final ZhenFunction HASTE      = ZhenFunction.simpleItem("haste",      ElementTypes.TIME, 40);
    public static final ZhenFunction TRANSMUTE  = ZhenFunction.simpleItem("transmute",  ElementTypes.ENERGY, 40);
    public static final ZhenFunction FORESIGHT  = ZhenFunction.simpleItem("foresight",  ElementTypes.DESCRIPTION, 40);
    public static final ZhenFunction SUMMON     = ZhenFunction.simpleItem("summon",     ElementTypes.CONSCIOUSNESS, 40);
    public static final ZhenFunction PORTAL     = ZhenFunction.tickOnly("portal",     ElementTypes.SPACE, 40,
            (l, p, s, be) -> () -> new PortalMethod(l, p, s, be).portal_tick());

    // ===== Level 50 — 远古（终极）=====
    public static final ZhenFunction CREATIVE   = ZhenFunction.simpleItem("creative",   ElementTypes.CREATIVE, 50);

    /** 注册所有功能 */
    public static void register(IEventBus eventBus) {
        var reg = ZhenTypes.ZHEN_TYPES;
        CINDER.register(reg);
        UNSTABLE_SIEVE_ZHEN = SIEVE.register(reg);
        DEW.register(reg);
        ZEPHYR.register(reg);
        FORGE.register(reg);
        COMPACT.register(reg);
        POTION.register(reg);
        CARVE.register(reg);
        SPROUT.register(reg);
        GRIND.register(reg);
        FROST.register(reg);
        VOLTAIC.register(reg);
        BLAZE.register(reg);
        GEM.register(reg);
        SPRING.register(reg);
        WHIRL.register(reg);
        FERMENT.register(reg);
        MOLD.register(reg);
        SHIFT.register(reg);
        THUNDER.register(reg);
        ENGRAVE.register(reg);
        QUAKE.register(reg);
        SYMBIOSIS.register(reg);
        CONFLUX.register(reg);
        SYNTHESIS.register(reg);
        DISTILL.register(reg);
        GATE.register(reg);
        DIVINE.register(reg);
        WEAVE.register(reg);
        FATE.register(reg);
        VOID.register(reg);
        HASTE.register(reg);
        TRANSMUTE.register(reg);
        FORESIGHT.register(reg);
        SUMMON.register(reg);
        PORTAL.register(reg);
        CREATIVE.register(reg);
    }
}
