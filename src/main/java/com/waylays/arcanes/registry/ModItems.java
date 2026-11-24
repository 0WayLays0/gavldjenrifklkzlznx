package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ArcanesOublies.MODID);

    /** Gold school spellbook: 10 slots, +10% Gold Spell Power, +200 max mana. */
    public static final DeferredHolder<Item, Item> GOLDEN_GRIMOIRE = ITEMS.register("golden_grimoire",
            () -> spellbook(10, ModAttributes.GOLD_SPELL_POWER));

    /** Explosion school spellbook: 10 slots, +10% Explosion Spell Power, +200 max mana. */
    public static final DeferredHolder<Item, Item> DETONATION_CODEX = ITEMS.register("detonation_codex",
            () -> spellbook(10, ModAttributes.EXPLOSION_SPELL_POWER));

    /** Book school spellbook: 12 slots, +10% Book Spell Power, +200 max mana. */
    public static final DeferredHolder<Item, Item> LIVING_GRIMOIRE = ITEMS.register("living_grimoire",
            () -> spellbook(12, ModAttributes.BOOK_SPELL_POWER));

    /** Boss Magic spellbook: 10 slots, +10% Boss Spell Power, +200 max mana. */
    public static final DeferredHolder<Item, Item> TYRANTS_CODEX = ITEMS.register("tyrants_codex",
            () -> spellbook(10, ModAttributes.BOSS_SPELL_POWER));

    /** Rune Magic spellbook (bound between two carved stone plates): 10 slots, +10% Rune Spell Power, +200 max mana. */
    public static final DeferredHolder<Item, Item> ELDER_RUNE_CODEX = ITEMS.register("elder_rune_codex",
            () -> spellbook(10, ModAttributes.RUNE_SPELL_POWER));

    /** Glyph Magic spellbook (six inks set in its cover): 10 slots, +10% Glyph Spell Power, +200 max mana. */
    public static final DeferredHolder<Item, Item> SIX_INKS_CODEX = ITEMS.register("six_inks_codex",
            () -> spellbook(10, ModAttributes.GLYPH_SPELL_POWER));

    /** Token Magic spellbook (a ledger set with soul tokens): 10 slots, +10% Token Spell Power, +200 max mana. */
    public static final DeferredHolder<Item, Item> SOUL_LEDGER = ITEMS.register("soul_ledger",
            () -> spellbook(10, ModAttributes.TOKEN_SPELL_POWER));

    /** A Soul Token (the server team's own model): the Token Magic focus, crafted from prismarine, gold and soul sand. */
    public static final DeferredHolder<Item, Item> SOUL_TOKEN = ITEMS.register("soul_token",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** Moss, Copper and Rust spellbook (bound in verdigrised copper, overgrown): 10 slots, +10% power, +200 max mana. */
    public static final DeferredHolder<Item, Item> RUSTED_CODEX = ITEMS.register("rusted_codex",
            () -> spellbook(10, ModAttributes.VERDIGRIS_SPELL_POWER));

    /** Wall Magic spellbook (bound between two plates of stone brick): 10 slots, +10% Wall Spell Power, +200 max mana. */
    public static final DeferredHolder<Item, Item> RAMPART_CODEX = ITEMS.register("rampart_codex",
            () -> spellbook(10, ModAttributes.WALL_SPELL_POWER));

    /** Gem Magic spellbook (the Jeweller's Codex, five cut gems set in its cover): 10 slots, +10% power, +200 max mana. */
    public static final DeferredHolder<Item, Item> JEWELLERS_CODEX = ITEMS.register("jewellers_codex",
            () -> spellbook(10, ModAttributes.GEM_SPELL_POWER));

    /** Cosmos spellbook (the Codex Out of Time: deep space on its cover, the TVA's clock set in it): 12 slots, +10% power, +200 max mana. */
    public static final DeferredHolder<Item, Item> CODEX_OUT_OF_TIME = ITEMS.register("codex_out_of_time",
            () -> spellbook(12, ModAttributes.COSMOS_SPELL_POWER));

    /** Bone Magic spellbook (the Ossuary Codex: bound in bone, a skull set in its cover): 10 slots, +10% power, +200 max mana. */
    public static final DeferredHolder<Item, Item> OSSUARY_CODEX = ITEMS.register("ossuary_codex",
            () -> spellbook(10, ModAttributes.BONE_SPELL_POWER));

    /** Wood Magic spellbook (the Sylvan Codex: bound in bark between two boards, leaves growing out of it): 10 slots, +10% power, +200 max mana. */
    public static final DeferredHolder<Item, Item> SYLVAN_CODEX = ITEMS.register("sylvan_codex",
            () -> spellbook(10, ModAttributes.WOOD_SPELL_POWER));

    /** Water Magic spellbook (the Codex of the Tides: bound in sea-worn leather, a pearl in a shell on its cover): 10 slots, +10% power, +200 max mana. */
    public static final DeferredHolder<Item, Item> TIDE_CODEX = ITEMS.register("tide_codex",
            () -> spellbook(10, ModAttributes.WATER_SPELL_POWER));

    /** Stellar Fire spellbook (the Starfire Codex: bound in blackened leather, six flames burning round a star on its cover): 10 slots, +10% power, +200 max mana. */
    public static final DeferredHolder<Item, Item> STARFIRE_CODEX = ITEMS.register("starfire_codex",
            () -> spellbook(10, ModAttributes.STARFIRE_SPELL_POWER));
    public static final DeferredHolder<Item, Item> WINDS_CODEX = ITEMS.register("winds_codex",
            () -> spellbook(10, ModAttributes.WIND_SPELL_POWER));
    public static final DeferredHolder<Item, Item> ECHOES_CODEX = ITEMS.register("echoes_codex",
            () -> spellbook(10, ModAttributes.ECHO_SPELL_POWER));
    public static final DeferredHolder<Item, Item> DUNES_CODEX = ITEMS.register("dunes_codex",
            () -> spellbook(10, ModAttributes.SAND_SPELL_POWER));
    public static final DeferredHolder<Item, Item> LOTUS_CODEX = ITEMS.register("lotus_codex",
            () -> spellbook(10, ModAttributes.LOTUS_SPELL_POWER));
    public static final DeferredHolder<Item, Item> BOREAL_CODEX = ITEMS.register("boreal_codex",
            () -> spellbook(10, ModAttributes.AURORA_SPELL_POWER));
    public static final DeferredHolder<Item, Item> CLOCKMAKERS_CODEX = ITEMS.register("clockmakers_codex",
            () -> spellbook(10, ModAttributes.CLOCKWORK_SPELL_POWER));
    public static final DeferredHolder<Item, Item> SYMPHONY_CODEX = ITEMS.register("symphony_codex",
            () -> spellbook(10, ModAttributes.MELODY_SPELL_POWER));
    public static final DeferredHolder<Item, Item> CODEX_OF_MENDING = ITEMS.register("codex_of_mending",
            () -> spellbook(10, ModAttributes.MEND_SPELL_POWER));
    public static final DeferredHolder<Item, Item> TRAPPERS_CODEX = ITEMS.register("trappers_codex",
            () -> spellbook(10, ModAttributes.TRAP_SPELL_POWER));
    public static final DeferredHolder<Item, Item> FIRST_FROST_CODEX = ITEMS.register("first_frost_codex",
            () -> spellbook(10, ModAttributes.SNOW_SPELL_POWER));
    public static final DeferredHolder<Item, Item> MAGMA_CODEX = ITEMS.register("magma_codex",
            () -> spellbook(10, ModAttributes.LAVA_SPELL_POWER));
    public static final DeferredHolder<Item, Item> HERBARIUM_CODEX = ITEMS.register("herbarium_codex",
            () -> spellbook(10, ModAttributes.PLANT_SPELL_POWER));
    public static final DeferredHolder<Item, Item> CORROSIVE_CODEX = ITEMS.register("corrosive_codex",
            () -> spellbook(10, ModAttributes.ACID_SPELL_POWER));
    public static final DeferredHolder<Item, Item> VIOLET_GALE_CODEX = ITEMS.register("violet_gale_codex",
            () -> spellbook(10, ModAttributes.GALE_SPELL_POWER));

    /** The Bone Blade grown out of the caster's elbow by Elbow Blade: a weapon for a while (see BoneBladeItem). */
    public static final DeferredHolder<Item, Item> BONE_BLADE = ITEMS.register("bone_blade", com.waylays.arcanes.item.BoneBladeItem::new);

    /** Ritual Chalk: chooses a group ritual and draws its circle on the ground (32 circles). */
    public static final DeferredHolder<Item, Item> RITUAL_CHALK = ITEMS.register("ritual_chalk",
            () -> new com.waylays.arcanes.ritual.RitualChalkItem(new Item.Properties().stacksTo(1).durability(32).rarity(Rarity.UNCOMMON)));

    /** The Altar of Rites (see AltarOfRitesBlock). */
    public static final DeferredHolder<Item, Item> ALTAR_OF_RITES = ITEMS.register("altar_of_rites",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ALTAR_OF_RITES.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, Item> ALEMBIC = ITEMS.register("alembic",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ALEMBIC.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> MEDICAL_TABLE = ITEMS.register("medical_table",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MEDICAL_TABLE.get(), new Item.Properties()));

    private ModItems() {}

    /** A legendary school spellbook, like Iron's own: +10% power in its school and +200 max mana. */
    private static Item spellbook(int slots, Holder<Attribute> schoolPower) {
        return new SpellBook(slots, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()).withSpellbookAttributes(
                new AttributeContainer(schoolPower, .10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new AttributeContainer(AttributeRegistry.MAX_MANA, 200, AttributeModifier.Operation.ADD_VALUE));
    }
}
