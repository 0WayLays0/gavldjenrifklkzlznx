package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.attribute.MagicRangedAttribute;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Per-school power and resistance attributes, built like Iron's own (default 1.0 = 100%).
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, ArcanesOublies.MODID);

    public static final DeferredHolder<Attribute, Attribute> GOLD_SPELL_POWER = powerAttribute("gold");
    public static final DeferredHolder<Attribute, Attribute> GOLD_MAGIC_RESIST = resistanceAttribute("gold");
    public static final DeferredHolder<Attribute, Attribute> EXPLOSION_SPELL_POWER = powerAttribute("explosion");
    public static final DeferredHolder<Attribute, Attribute> EXPLOSION_MAGIC_RESIST = resistanceAttribute("explosion");
    public static final DeferredHolder<Attribute, Attribute> BOOK_SPELL_POWER = powerAttribute("book");
    public static final DeferredHolder<Attribute, Attribute> BOOK_MAGIC_RESIST = resistanceAttribute("book");
    public static final DeferredHolder<Attribute, Attribute> ERRETHYS_SPELL_POWER = powerAttribute("errethys");
    public static final DeferredHolder<Attribute, Attribute> ERRETHYS_MAGIC_RESIST = resistanceAttribute("errethys");
    /** Physical Prowess: how hard the fighter's moves hit, and how well they shrug off others' moves. */
    public static final DeferredHolder<Attribute, Attribute> PHYSICAL_SPELL_POWER = powerAttribute("physical");
    public static final DeferredHolder<Attribute, Attribute> PHYSICAL_MAGIC_RESIST = resistanceAttribute("physical");
    public static final DeferredHolder<Attribute, Attribute> BOSS_SPELL_POWER = powerAttribute("boss");
    public static final DeferredHolder<Attribute, Attribute> BOSS_MAGIC_RESIST = resistanceAttribute("boss");
    public static final DeferredHolder<Attribute, Attribute> RUNE_SPELL_POWER = powerAttribute("rune");
    public static final DeferredHolder<Attribute, Attribute> RUNE_MAGIC_RESIST = resistanceAttribute("rune");
    public static final DeferredHolder<Attribute, Attribute> GLYPH_SPELL_POWER = powerAttribute("glyph");
    public static final DeferredHolder<Attribute, Attribute> GLYPH_MAGIC_RESIST = resistanceAttribute("glyph");
    public static final DeferredHolder<Attribute, Attribute> TOKEN_SPELL_POWER = powerAttribute("token");
    public static final DeferredHolder<Attribute, Attribute> TOKEN_MAGIC_RESIST = resistanceAttribute("token");
    public static final DeferredHolder<Attribute, Attribute> VERDIGRIS_SPELL_POWER = powerAttribute("verdigris");
    public static final DeferredHolder<Attribute, Attribute> VERDIGRIS_MAGIC_RESIST = resistanceAttribute("verdigris");
    public static final DeferredHolder<Attribute, Attribute> WALL_SPELL_POWER = powerAttribute("wall");
    public static final DeferredHolder<Attribute, Attribute> WALL_MAGIC_RESIST = resistanceAttribute("wall");
    public static final DeferredHolder<Attribute, Attribute> GEM_SPELL_POWER = powerAttribute("gem");
    public static final DeferredHolder<Attribute, Attribute> GEM_MAGIC_RESIST = resistanceAttribute("gem");
    public static final DeferredHolder<Attribute, Attribute> COSMOS_SPELL_POWER = powerAttribute("cosmos");
    public static final DeferredHolder<Attribute, Attribute> COSMOS_MAGIC_RESIST = resistanceAttribute("cosmos");
    public static final DeferredHolder<Attribute, Attribute> BONE_SPELL_POWER = powerAttribute("bone");
    public static final DeferredHolder<Attribute, Attribute> BONE_MAGIC_RESIST = resistanceAttribute("bone");
    public static final DeferredHolder<Attribute, Attribute> WOOD_SPELL_POWER = powerAttribute("wood");
    public static final DeferredHolder<Attribute, Attribute> WOOD_MAGIC_RESIST = resistanceAttribute("wood");
    public static final DeferredHolder<Attribute, Attribute> WATER_SPELL_POWER = powerAttribute("water");
    public static final DeferredHolder<Attribute, Attribute> WATER_MAGIC_RESIST = resistanceAttribute("water");

    private ModAttributes() {}

    @SubscribeEvent
    public static void addToEntities(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> ATTRIBUTES.getEntries().forEach(attribute -> event.add(type, attribute)));
    }

    private static DeferredHolder<Attribute, Attribute> powerAttribute(String school) {
        String name = school + "_spell_power";
        return ATTRIBUTES.register(name, () -> new MagicRangedAttribute("attribute." + ArcanesOublies.MODID + "." + name, 1.0D, -100, 100).setSyncable(true));
    }

    private static DeferredHolder<Attribute, Attribute> resistanceAttribute(String school) {
        String name = school + "_magic_resist";
        return ATTRIBUTES.register(name, () -> new MagicRangedAttribute("attribute." + ArcanesOublies.MODID + "." + name, 1.0D, -100, 100).setSyncable(true));
    }
}
