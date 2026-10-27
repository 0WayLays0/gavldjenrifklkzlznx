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

    /** The Bone Blade grown out of the caster's elbow by Elbow Blade: a weapon for a while (see BoneBladeItem). */
    public static final DeferredHolder<Item, Item> BONE_BLADE = ITEMS.register("bone_blade", com.waylays.arcanes.item.BoneBladeItem::new);

    /** Ritual Chalk: chooses a group ritual and draws its circle on the ground (32 circles). */
    public static final DeferredHolder<Item, Item> RITUAL_CHALK = ITEMS.register("ritual_chalk",
            () -> new com.waylays.arcanes.ritual.RitualChalkItem(new Item.Properties().stacksTo(1).durability(32).rarity(Rarity.UNCOMMON)));

    private ModItems() {}

    /** A legendary school spellbook, like Iron's own: +10% power in its school and +200 max mana. */
    private static Item spellbook(int slots, Holder<Attribute> schoolPower) {
        return new SpellBook(slots, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()).withSpellbookAttributes(
                new AttributeContainer(schoolPower, .10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new AttributeContainer(AttributeRegistry.MAX_MANA, 200, AttributeModifier.Operation.ADD_VALUE));
    }
}
