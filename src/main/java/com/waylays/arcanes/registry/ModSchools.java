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

    /**
     * Wall Magic: walls, bricks and fortresses, built of three blocks only (stone, mossy and cracked stone bricks).
     * Walls burst out of the ground to stop shots, cage, crush and charge; bricks are hurled, lobbed and rained.
     */
    public static final ResourceLocation WALL_RESOURCE = ArcanesOublies.id("wall");
    public static final TagKey<Item> WALL_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("wall_focus"));
    public static final ResourceKey<DamageType> WALL_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("wall_magic"));
    public static final int WALL_COLOR = 0x9A9A92;

    public static final DeferredHolder<SchoolType, SchoolType> WALL = SCHOOLS.register(WALL_RESOURCE.getPath(), () -> new SchoolType(
            WALL_RESOURCE,
            WALL_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".wall").withStyle(Style.EMPTY.withColor(WALL_COLOR)),
            ModAttributes.WALL_SPELL_POWER,
            ModAttributes.WALL_MAGIC_RESIST,
            sound(SoundEvents.STONE_PLACE),
            WALL_MAGIC
    ));

    /**
     * Gem Magic: five cut gems, each with its own nature. The ruby burns (focused light), the lapis seals (the night
     * sky and its golden stars), the diamond refracts and endures, the amethyst resonates, the emerald heals; the
     * crown of the five unites them.
     */
    public static final ResourceLocation GEM_RESOURCE = ArcanesOublies.id("gem");
    public static final TagKey<Item> GEM_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("gem_focus"));
    public static final ResourceKey<DamageType> GEM_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("gem_magic"));
    public static final int GEM_COLOR = 0xE0457B;

    public static final DeferredHolder<SchoolType, SchoolType> GEM = SCHOOLS.register(GEM_RESOURCE.getPath(), () -> new SchoolType(
            GEM_RESOURCE,
            GEM_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".gem").withStyle(Style.EMPTY.withColor(GEM_COLOR)),
            ModAttributes.GEM_SPELL_POWER,
            ModAttributes.GEM_MAGIC_RESIST,
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE),
            GEM_MAGIC
    ));

    /**
     * Cosmos: the magic of space and time. Time is the orange of the Time Variance Authority's doors and clocks (a
     * universe in a pocket, doors back to where you were, loops, variants, stopped time); space is deep blue and
     * violet sown with stars (planets, comets, moons and constellations).
     */
    public static final ResourceLocation COSMOS_RESOURCE = ArcanesOublies.id("cosmos");
    public static final TagKey<Item> COSMOS_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("cosmos_focus"));
    public static final ResourceKey<DamageType> COSMOS_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("cosmos_magic"));
    public static final int COSMOS_COLOR = 0x6C7CFF;

    public static final DeferredHolder<SchoolType, SchoolType> COSMOS = SCHOOLS.register(COSMOS_RESOURCE.getPath(), () -> new SchoolType(
            COSMOS_RESOURCE,
            COSMOS_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".cosmos").withStyle(Style.EMPTY.withColor(COSMOS_COLOR)),
            ModAttributes.COSMOS_SPELL_POWER,
            ModAttributes.COSMOS_MAGIC_RESIST,
            sound(SoundEvents.BEACON_POWER_SELECT),
            COSMOS_MAGIC
    ));

    /**
     * Bone Magic: bones called up out of the ground and grown out of the caster, ivory white, the marrow glowing
     * crimson: lances, spikes, a whip of vertebrae, ribs closing like a cage, a skeletal hand, a wyrm of vertebrae and a
     * giant skeleton.
     */
    public static final ResourceLocation BONE_RESOURCE = ArcanesOublies.id("bone");
    public static final TagKey<Item> BONE_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("bone_focus"));
    public static final ResourceKey<DamageType> BONE_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("bone_magic"));
    public static final int BONE_COLOR = 0xE8DCC0;

    public static final DeferredHolder<SchoolType, SchoolType> BONE = SCHOOLS.register(BONE_RESOURCE.getPath(), () -> new SchoolType(
            BONE_RESOURCE,
            BONE_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".bone").withStyle(Style.EMPTY.withColor(BONE_COLOR)),
            ModAttributes.BONE_SPELL_POWER,
            ModAttributes.BONE_MAGIC_RESIST,
            sound(SoundEvents.BONE_BLOCK_PLACE),
            BONE_MAGIC
    ));

    /**
     * Wood Magic: living wood called up out of the ground: a rolling log, roots that seize, brambles and seed pods, a
     * vine that yanks, a saw of tree rings, a cyclone of splinters, amber that sets round its prey, a guardian of
     * braided wood and the World Tree; its life glows spring green.
     */
    public static final ResourceLocation WOOD_RESOURCE = ArcanesOublies.id("wood");
    public static final TagKey<Item> WOOD_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("wood_focus"));
    public static final ResourceKey<DamageType> WOOD_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("wood_magic"));
    public static final int WOOD_COLOR = 0xB07A3E;

    public static final DeferredHolder<SchoolType, SchoolType> WOOD = SCHOOLS.register(WOOD_RESOURCE.getPath(), () -> new SchoolType(
            WOOD_RESOURCE,
            WOOD_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".wood").withStyle(Style.EMPTY.withColor(WOOD_COLOR)),
            ModAttributes.WOOD_SPELL_POWER,
            ModAttributes.WOOD_MAGIC_RESIST,
            sound(SoundEvents.WOOD_PLACE),
            WOOD_MAGIC
    ));

    /**
     * Water Magic: the sea answering a call: lances of spiralling water, a jet that cuts, geysers, bubbles that carry
     * their prey off, a cleansing rain, a maelstrom, glowing jellyfish, a typhoon, a tidal wave, and the Abyssal Dome
     * that drowns the land.
     */
    public static final ResourceLocation WATER_RESOURCE = ArcanesOublies.id("water");
    public static final TagKey<Item> WATER_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("water_focus"));
    public static final ResourceKey<DamageType> WATER_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("water_magic"));
    public static final int WATER_COLOR = 0x2E9FE0;

    public static final DeferredHolder<SchoolType, SchoolType> WATER = SCHOOLS.register(WATER_RESOURCE.getPath(), () -> new SchoolType(
            WATER_RESOURCE,
            WATER_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".water").withStyle(Style.EMPTY.withColor(WATER_COLOR)),
            ModAttributes.WATER_SPELL_POWER,
            ModAttributes.WATER_MAGIC_RESIST,
            sound(SoundEvents.BUCKET_EMPTY),
            WATER_MAGIC
    ));

    /**
     * Stellar Fire: flames that burn in the colours of the stars and of copper, each spell in each of seven colours that
     * all strike alike: red, orange, yellow, white, blue, violet and green (see {@link com.waylays.arcanes.spells.fire.HeatColors}).
     */
    public static final ResourceLocation STARFIRE_RESOURCE = ArcanesOublies.id("starfire");
    public static final TagKey<Item> STARFIRE_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("starfire_focus"));
    public static final ResourceKey<DamageType> STARFIRE_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("starfire_magic"));
    public static final int STARFIRE_COLOR = 0xFF8A1E;

    public static final DeferredHolder<SchoolType, SchoolType> STARFIRE = SCHOOLS.register(STARFIRE_RESOURCE.getPath(), () -> new SchoolType(
            STARFIRE_RESOURCE,
            STARFIRE_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".starfire").withStyle(Style.EMPTY.withColor(STARFIRE_COLOR)),
            ModAttributes.STARFIRE_SPELL_POWER,
            ModAttributes.STARFIRE_MAGIC_RESIST,
            sound(SoundEvents.FIRECHARGE_USE),
            STARFIRE_MAGIC
    ));

    /** Wind Magic: blades and blasts of wind, whirlwinds, walls of wind, the lift of the air, and the hurricane. */
    public static final ResourceLocation WIND_RESOURCE = ArcanesOublies.id("wind");
    public static final TagKey<Item> WIND_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("wind_focus"));
    public static final ResourceKey<DamageType> WIND_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("wind_magic"));
    public static final int WIND_COLOR = 0x8FE3C8;

    public static final DeferredHolder<SchoolType, SchoolType> WIND = SCHOOLS.register(WIND_RESOURCE.getPath(), () -> new SchoolType(
            WIND_RESOURCE,
            WIND_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".wind").withStyle(Style.EMPTY.withColor(WIND_COLOR)),
            ModAttributes.WIND_SPELL_POWER,
            ModAttributes.WIND_MAGIC_RESIST,
            sound(SoundEvents.BREEZE_SHOOT),
            WIND_MAGIC
    ));

    /**
     * Echo Magic: sound and the sculk of the deep dark. Its blows leave Resonance in their victims (see
     * {@link com.waylays.arcanes.effect.ResonanceEffect}), which its other spells make burst.
     */
    public static final ResourceLocation ECHO_RESOURCE = ArcanesOublies.id("echo");
    public static final TagKey<Item> ECHO_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("echo_focus"));
    public static final ResourceKey<DamageType> ECHO_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("echo_magic"));
    public static final int ECHO_COLOR = 0x2CE6F0;

    public static final DeferredHolder<SchoolType, SchoolType> ECHO = SCHOOLS.register(ECHO_RESOURCE.getPath(), () -> new SchoolType(
            ECHO_RESOURCE,
            ECHO_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".echo").withStyle(Style.EMPTY.withColor(ECHO_COLOR)),
            ModAttributes.ECHO_SPELL_POWER,
            ModAttributes.ECHO_MAGIC_RESIST,
            sound(SoundEvents.SCULK_SHRIEKER_SHRIEK),
            ECHO_MAGIC
    ));

    /** Sand Magic: the desert's sand, glass and sandstone; quicksand, sandstorms, the sandworm, the pyramid. */
    public static final ResourceLocation SAND_RESOURCE = ArcanesOublies.id("sand");
    public static final TagKey<Item> SAND_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("sand_focus"));
    public static final ResourceKey<DamageType> SAND_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("sand_magic"));
    public static final int SAND_COLOR = 0xE0B866;

    public static final DeferredHolder<SchoolType, SchoolType> SAND = SCHOOLS.register(SAND_RESOURCE.getPath(), () -> new SchoolType(
            SAND_RESOURCE,
            SAND_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".sand").withStyle(Style.EMPTY.withColor(SAND_COLOR)),
            ModAttributes.SAND_SPELL_POWER,
            ModAttributes.SAND_MAGIC_RESIST,
            sound(SoundEvents.SAND_BREAK),
            SAND_MAGIC
    ));

    /** Lotus Magic: the lotus rising pure out of the mud; keen petals, the sacred pond, roots and seeds, the light of its heart. */
    public static final ResourceLocation LOTUS_RESOURCE = ArcanesOublies.id("lotus");
    public static final TagKey<Item> LOTUS_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("lotus_focus"));
    public static final ResourceKey<DamageType> LOTUS_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("lotus_magic"));
    public static final int LOTUS_COLOR = 0xF58AC0;

    public static final DeferredHolder<SchoolType, SchoolType> LOTUS = SCHOOLS.register(LOTUS_RESOURCE.getPath(), () -> new SchoolType(
            LOTUS_RESOURCE,
            LOTUS_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".lotus").withStyle(Style.EMPTY.withColor(LOTUS_COLOR)),
            ModAttributes.LOTUS_SPELL_POWER,
            ModAttributes.LOTUS_MAGIC_RESIST,
            sound(SoundEvents.AZALEA_LEAVES_BREAK),
            LOTUS_MAGIC
    ));

    /** Aurora Magic: the northern lights; curtains and ribbons of green, turquoise and violet light, stars and ice. */
    public static final ResourceLocation AURORA_RESOURCE = ArcanesOublies.id("aurora");
    public static final TagKey<Item> AURORA_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("aurora_focus"));
    public static final ResourceKey<DamageType> AURORA_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("aurora_magic"));
    public static final int AURORA_COLOR = 0x5CF5B8;

    public static final DeferredHolder<SchoolType, SchoolType> AURORA = SCHOOLS.register(AURORA_RESOURCE.getPath(), () -> new SchoolType(
            AURORA_RESOURCE,
            AURORA_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".aurora").withStyle(Style.EMPTY.withColor(AURORA_COLOR)),
            ModAttributes.AURORA_SPELL_POWER,
            ModAttributes.AURORA_MAGIC_RESIST,
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE),
            AURORA_MAGIC
    ));

    /** Clockwork Magic: gears of brass, copper and steel, steam, and cores of blue energy; machines that fight for the caster. */
    public static final ResourceLocation CLOCKWORK_RESOURCE = ArcanesOublies.id("clockwork");
    public static final TagKey<Item> CLOCKWORK_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("clockwork_focus"));
    public static final ResourceKey<DamageType> CLOCKWORK_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("clockwork_magic"));
    public static final int CLOCKWORK_COLOR = 0xE6B04C;

    public static final DeferredHolder<SchoolType, SchoolType> CLOCKWORK = SCHOOLS.register(CLOCKWORK_RESOURCE.getPath(), () -> new SchoolType(
            CLOCKWORK_RESOURCE,
            CLOCKWORK_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".clockwork").withStyle(Style.EMPTY.withColor(CLOCKWORK_COLOR)),
            ModAttributes.CLOCKWORK_SPELL_POWER,
            ModAttributes.CLOCKWORK_MAGIC_RESIST,
            sound(SoundEvents.ANVIL_USE),
            CLOCKWORK_MAGIC
    ));

    /** Music Magic: notes of gold, staves of light, and phantom instruments that play for the caster. */
    public static final ResourceLocation MELODY_RESOURCE = ArcanesOublies.id("melody");
    public static final TagKey<Item> MELODY_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("melody_focus"));
    public static final ResourceKey<DamageType> MELODY_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("melody_magic"));
    public static final int MELODY_COLOR = 0xFF8AD8;

    public static final DeferredHolder<SchoolType, SchoolType> MELODY = SCHOOLS.register(MELODY_RESOURCE.getPath(), () -> new SchoolType(
            MELODY_RESOURCE,
            MELODY_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".melody").withStyle(Style.EMPTY.withColor(MELODY_COLOR)),
            ModAttributes.MELODY_SPELL_POWER,
            ModAttributes.MELODY_MAGIC_RESIST,
            SoundEvents.NOTE_BLOCK_CHIME,
            MELODY_MAGIC
    ));

    /** Mending Magic: healing laid on one ally at a time, each spell its own way of keeping them alive. */
    public static final ResourceLocation MEND_RESOURCE = ArcanesOublies.id("mend");
    public static final TagKey<Item> MEND_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("mend_focus"));
    public static final ResourceKey<DamageType> MEND_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("mend_magic"));
    public static final int MEND_COLOR = 0x7CFFB0;

    public static final DeferredHolder<SchoolType, SchoolType> MEND = SCHOOLS.register(MEND_RESOURCE.getPath(), () -> new SchoolType(
            MEND_RESOURCE,
            MEND_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".mend").withStyle(Style.EMPTY.withColor(MEND_COLOR)),
            ModAttributes.MEND_SPELL_POWER,
            ModAttributes.MEND_MAGIC_RESIST,
            sound(SoundEvents.AMETHYST_BLOCK_CHIME),
            MEND_MAGIC
    ));

    /** Trap Magic: traps hidden on the ground that never stun, only weaken: poison, bleeding, rust, curses. */
    public static final ResourceLocation TRAP_RESOURCE = ArcanesOublies.id("trap");
    public static final TagKey<Item> TRAP_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("trap_focus"));
    public static final ResourceKey<DamageType> TRAP_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("trap_magic"));
    public static final int TRAP_COLOR = 0x9CE05A;

    public static final DeferredHolder<SchoolType, SchoolType> TRAP = SCHOOLS.register(TRAP_RESOURCE.getPath(), () -> new SchoolType(
            TRAP_RESOURCE,
            TRAP_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".trap").withStyle(Style.EMPTY.withColor(TRAP_COLOR)),
            ModAttributes.TRAP_SPELL_POWER,
            ModAttributes.TRAP_MAGIC_RESIST,
            sound(SoundEvents.TRIPWIRE_CLICK_ON),
            TRAP_MAGIC
    ));

    private ModSchools() {}

    /** A school's default cast sound, as the holder Iron's asks for. */
    private static Holder<SoundEvent> sound(SoundEvent sound) {
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
    }
}
