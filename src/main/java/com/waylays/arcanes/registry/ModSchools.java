package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Schools of magic added to Iron's school registry. Each school needs, besides this entry: a damage type json,
 * a focus item tag, power/resistance attributes, and the item/scroll_{@code <school>} and
 * item/affinity_ring_{@code <school>} models that Iron's looks up by school id. The scroll model shows Iron's own scroll
 * recoloured by the game with the school's palette (tools/scroll_assets.py).
 */
public final class ModSchools {
    public static final DeferredRegister<SchoolType> SCHOOLS = DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, ArcanesOublies.MODID);

    public static final ResourceLocation GOLD_RESOURCE = ArcanesOublies.id("gold");
    public static final TagKey<Item> GOLD_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("gold_focus"));
    public static final ResourceKey<DamageType> GOLD_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("gold_magic"));
    public static final int GOLD_COLOR = 0xFFC93C;

    public static final DeferredHolder<SchoolType, SchoolType> GOLD = SCHOOLS.register(GOLD_RESOURCE.getPath(), () -> new SchoolType(
            GOLD_RESOURCE,
            GOLD_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".gold").withStyle(Style.EMPTY.withColor(GOLD_COLOR)),
            ModAttributes.GOLD_SPELL_POWER,
            ModAttributes.GOLD_MAGIC_RESIST,
            SoundEvents.ARMOR_EQUIP_GOLD,
            GOLD_MAGIC
    ));

    public static final ResourceLocation EXPLOSION_RESOURCE = ArcanesOublies.id("explosion");
    public static final TagKey<Item> EXPLOSION_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("explosion_focus"));
    public static final ResourceKey<DamageType> EXPLOSION_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("explosion_magic"));
    public static final int EXPLOSION_COLOR = 0xFF6A2A;

    public static final DeferredHolder<SchoolType, SchoolType> EXPLOSION = SCHOOLS.register(EXPLOSION_RESOURCE.getPath(), () -> new SchoolType(
            EXPLOSION_RESOURCE,
            EXPLOSION_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".explosion").withStyle(Style.EMPTY.withColor(EXPLOSION_COLOR)),
            ModAttributes.EXPLOSION_SPELL_POWER,
            ModAttributes.EXPLOSION_MAGIC_RESIST,
            sound(SoundEvents.FIRECHARGE_USE),
            EXPLOSION_MAGIC
    ));

    public static final ResourceLocation BOOK_RESOURCE = ArcanesOublies.id("book");
    public static final TagKey<Item> BOOK_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("book_focus"));
    public static final ResourceKey<DamageType> BOOK_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("book_magic"));
    public static final int BOOK_COLOR = 0x9A5CFF;

    public static final DeferredHolder<SchoolType, SchoolType> BOOK = SCHOOLS.register(BOOK_RESOURCE.getPath(), () -> new SchoolType(
            BOOK_RESOURCE,
            BOOK_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".book").withStyle(Style.EMPTY.withColor(BOOK_COLOR)),
            ModAttributes.BOOK_SPELL_POWER,
            ModAttributes.BOOK_MAGIC_RESIST,
            sound(SoundEvents.BOOK_PAGE_TURN),
            BOOK_MAGIC
    ));

    /** Errethys: creatures of sky-blue mana summoned to fight or carry their caster. */
    public static final ResourceLocation ERRETHYS_RESOURCE = ArcanesOublies.id("errethys");
    public static final TagKey<Item> ERRETHYS_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("errethys_focus"));
    public static final ResourceKey<DamageType> ERRETHYS_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("errethys_magic"));
    public static final int ERRETHYS_COLOR = 0x87CEEB;

    public static final DeferredHolder<SchoolType, SchoolType> ERRETHYS = SCHOOLS.register(ERRETHYS_RESOURCE.getPath(), () -> new SchoolType(
            ERRETHYS_RESOURCE,
            ERRETHYS_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".errethys").withStyle(Style.EMPTY.withColor(ERRETHYS_COLOR)),
            ModAttributes.ERRETHYS_SPELL_POWER,
            ModAttributes.ERRETHYS_MAGIC_RESIST,
            sound(SoundEvents.AMETHYST_BLOCK_CHIME),
            ERRETHYS_MAGIC
    ));

    /**
     * Physical Prowess: fighting moves, not magic (punches, slashes, a dash, a leap, weapon techniques). It lives in
     * Iron's school system so the moves sit in spellbooks and on weapons like spells, but its damage is plain physical
     * damage that armor reduces, and the Seal of Silence does not stop it.
     */
    public static final ResourceLocation PHYSICAL_RESOURCE = ArcanesOublies.id("physical");
    public static final TagKey<Item> PHYSICAL_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("physical_focus"));
    public static final ResourceKey<DamageType> PHYSICAL_STRIKE = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("physical_strike"));
    public static final int PHYSICAL_COLOR = 0xC9875A;

    public static final DeferredHolder<SchoolType, SchoolType> PHYSICAL = SCHOOLS.register(PHYSICAL_RESOURCE.getPath(), () -> new SchoolType(
            PHYSICAL_RESOURCE,
            PHYSICAL_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".physical").withStyle(Style.EMPTY.withColor(PHYSICAL_COLOR)),
            ModAttributes.PHYSICAL_SPELL_POWER,
            ModAttributes.PHYSICAL_MAGIC_RESIST,
            sound(SoundEvents.PLAYER_ATTACK_STRONG),
            PHYSICAL_STRIKE
    ));

    /**
     * Boss Magic: attacks worthy of a final boss (meteors, giant blades, beams, waves of runes). Each one is announced
     * by runes on the ground a few seconds before it lands, so those in its way can get out.
     */
    public static final ResourceLocation BOSS_RESOURCE = ArcanesOublies.id("boss");
    public static final TagKey<Item> BOSS_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("boss_focus"));
    public static final ResourceKey<DamageType> BOSS_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("boss_magic"));
    public static final int BOSS_COLOR = 0xE0283C;

    public static final DeferredHolder<SchoolType, SchoolType> BOSS = SCHOOLS.register(BOSS_RESOURCE.getPath(), () -> new SchoolType(
            BOSS_RESOURCE,
            BOSS_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".boss").withStyle(Style.EMPTY.withColor(BOSS_COLOR)),
            ModAttributes.BOSS_SPELL_POWER,
            ModAttributes.BOSS_MAGIC_RESIST,
            sound(SoundEvents.RESPAWN_ANCHOR_CHARGE),
            BOSS_MAGIC
    ));

    /**
     * Rune Magic: runes carved into the ground, into stone and into weapons. Set down ahead of the fight, they keep
     * working for a while: half of them shield and heal, half of them strike.
     */
    public static final ResourceLocation RUNE_RESOURCE = ArcanesOublies.id("rune");
    public static final TagKey<Item> RUNE_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("rune_focus"));
    public static final ResourceKey<DamageType> RUNE_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("rune_magic"));
    public static final int RUNE_COLOR = 0xB57BFF;

    public static final DeferredHolder<SchoolType, SchoolType> RUNE = SCHOOLS.register(RUNE_RESOURCE.getPath(), () -> new SchoolType(
            RUNE_RESOURCE,
            RUNE_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".rune").withStyle(Style.EMPTY.withColor(RUNE_COLOR)),
            ModAttributes.RUNE_SPELL_POWER,
            ModAttributes.RUNE_MAGIC_RESIST,
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE),
            RUNE_MAGIC
    ));

    /**
     * Glyph Magic: elemental glyphs written in light, in the air or on the ground (fire, frost, storm, tide, gale and
     * stone). Each glyph inscribes its element on what it strikes; another element striking the mark sets off their
     * reaction.
     */
    public static final ResourceLocation GLYPH_RESOURCE = ArcanesOublies.id("glyph");
    public static final TagKey<Item> GLYPH_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("glyph_focus"));
    public static final ResourceKey<DamageType> GLYPH_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("glyph_magic"));
    public static final int GLYPH_COLOR = 0xFFD27A;

    public static final DeferredHolder<SchoolType, SchoolType> GLYPH = SCHOOLS.register(GLYPH_RESOURCE.getPath(), () -> new SchoolType(
            GLYPH_RESOURCE,
            GLYPH_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".glyph").withStyle(Style.EMPTY.withColor(GLYPH_COLOR)),
            ModAttributes.GLYPH_SPELL_POWER,
            ModAttributes.GLYPH_MAGIC_RESIST,
            sound(SoundEvents.AMETHYST_BLOCK_CHIME),
            GLYPH_MAGIC
    ));

    /**
     * Token Magic: soul tokens, cyan-blue coins holding a soul's spark. Thrown, scattered, whirled or gathered into a
     * giant hand; one thrown token is a door the caster can step through.
     */
    public static final ResourceLocation TOKEN_RESOURCE = ArcanesOublies.id("token");
    public static final TagKey<Item> TOKEN_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("token_focus"));
    public static final ResourceKey<DamageType> TOKEN_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("token_magic"));
    public static final int TOKEN_COLOR = 0x33D6FF;

    public static final DeferredHolder<SchoolType, SchoolType> TOKEN = SCHOOLS.register(TOKEN_RESOURCE.getPath(), () -> new SchoolType(
            TOKEN_RESOURCE,
            TOKEN_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".token").withStyle(Style.EMPTY.withColor(TOKEN_COLOR)),
            ModAttributes.TOKEN_SPELL_POWER,
            ModAttributes.TOKEN_MAGIC_RESIST,
            sound(SoundEvents.CHAIN_PLACE),
            TOKEN_MAGIC
    ));

    /**
     * Moss, Copper and Rust: the ancient magic of drowned temples. Copper spikes, pillars and walls burst out of the
     * ground, green with verdigris and hung with vines; rust eats armour and moss chokes. Offensive throughout.
     */
    public static final ResourceLocation VERDIGRIS_RESOURCE = ArcanesOublies.id("verdigris");
    public static final TagKey<Item> VERDIGRIS_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("verdigris_focus"));
    public static final ResourceKey<DamageType> VERDIGRIS_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("verdigris_magic"));
    public static final int VERDIGRIS_COLOR = 0x4FBF96;

    public static final DeferredHolder<SchoolType, SchoolType> VERDIGRIS = SCHOOLS.register(VERDIGRIS_RESOURCE.getPath(), () -> new SchoolType(
            VERDIGRIS_RESOURCE,
            VERDIGRIS_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".verdigris").withStyle(Style.EMPTY.withColor(VERDIGRIS_COLOR)),
            ModAttributes.VERDIGRIS_SPELL_POWER,
            ModAttributes.VERDIGRIS_MAGIC_RESIST,
            sound(SoundEvents.COPPER_PLACE),
            VERDIGRIS_MAGIC
    ));

    private ModSchools() {}

    /** A school's default cast sound, as the holder Iron's asks for. */
    private static Holder<SoundEvent> sound(SoundEvent sound) {
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
    }
}
