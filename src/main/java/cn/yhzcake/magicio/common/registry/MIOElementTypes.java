package cn.yhzcake.magicio.common.registry;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.common.sys.matrix.ElementType;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MIOElementTypes {
    public static final DeferredRegister<ElementType> ELEMENT_TYPES = DeferredRegister.create(MIORegistries.MIOKeys.ELEMENT_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);

    public static final Holder<ElementType> EMPTY = ELEMENT_TYPES.register("empty", () -> new ElementType(ElementType.Properties.create()));

    public static final Holder<ElementType> EARTH = ELEMENT_TYPES.register("earth", () -> new ElementType(ElementType.Properties.create()));
    public static final Holder<ElementType> WATER = ELEMENT_TYPES.register("water", () -> new ElementType(ElementType.Properties.create()));
    public static final Holder<ElementType> WIND = ELEMENT_TYPES.register("wind", () -> new ElementType(ElementType.Properties.create()));
    public static final Holder<ElementType> FIRE = ELEMENT_TYPES.register("fire", () -> new ElementType(ElementType.Properties.create()));



    public static final Holder<ElementType> WOOD = ELEMENT_TYPES.register("wood", () -> new ElementType(ElementType.Properties.create()
        .withSource(EARTH.value())
        .withSource(WATER.value())
        .setRarity(Rarity.UNCOMMON))
    );
    public static final Holder<ElementType> ICE = ELEMENT_TYPES.register("ice", () -> new ElementType(ElementType.Properties.create()
        .withSource(WATER.value())
        .withSource(WIND.value())
        .setRarity(Rarity.UNCOMMON))
    );
    public static final Holder<ElementType> METAL = ELEMENT_TYPES.register("metal", () -> new ElementType(ElementType.Properties.create()
        .withSource(FIRE.value())
        .withSource(EARTH.value())
        .setRarity(Rarity.UNCOMMON))
    );
    public static final Holder<ElementType> LIGHTNING = ELEMENT_TYPES.register("lightning", () -> new ElementType(ElementType.Properties.create()
        .withSource(WIND.value())
        .withSource(FIRE.value())
        .setRarity(Rarity.UNCOMMON))
    );
    public static final Holder<ElementType> ORDER = ELEMENT_TYPES.register("order", () -> new ElementType(ElementType.Properties.create()
        .withSource(WOOD.value())
        .withSource(METAL.value())
        .setRarity(Rarity.UNCOMMON))
    );
    public static final Holder<ElementType> SPACE = ELEMENT_TYPES.register("space", () -> new ElementType(ElementType.Properties.create()
        .withSource(ICE.value())
        .withSource(WOOD.value())
        .setRarity(Rarity.UNCOMMON))
    );
    public static final Holder<ElementType> TIME = ELEMENT_TYPES.register("time", () -> new ElementType(ElementType.Properties.create()
        .withSource(METAL.value())
        .withSource(LIGHTNING.value())
        .setRarity(Rarity.UNCOMMON))
    );
    public static final Holder<ElementType> CHAOS = ELEMENT_TYPES.register("chaos", () -> new ElementType(ElementType.Properties.create()
        .withSource(LIGHTNING.value())
        .withSource(ICE.value())
        .setRarity(Rarity.UNCOMMON))
    );



    public static final Holder<ElementType> DESCRIPTION = ELEMENT_TYPES.register("description", () -> new ElementType(ElementType.Properties.create()
        .withSource(CHAOS.value())
        .withSource(ORDER.value())
        .setRarity(Rarity.RARE))
    );
    public static final Holder<ElementType> ENERGY = ELEMENT_TYPES.register("energy", () -> new ElementType(ElementType.Properties.create()
        .withSource(SPACE.value())
        .withSource(TIME.value())
        .setRarity(Rarity.RARE))
    );
    public static final Holder<ElementType> CONSCIOUSNESS = ELEMENT_TYPES.register("consciousness", () -> new ElementType(ElementType.Properties.create()
        .withSource(LIGHTNING.value())
        .withSource(EARTH.value())
        .withSource(ENERGY.value())
        .setRarity(Rarity.RARE))
    );



    public static final Holder<ElementType> CREATIVE = ELEMENT_TYPES.register("creative", () -> new ElementType(ElementType.Properties.create()
        .withSource(DESCRIPTION.value())
        .withSource(ENERGY.value())
        .withSource(CONSCIOUSNESS.value())
        .setRarity(Rarity.EPIC))
    );

}
