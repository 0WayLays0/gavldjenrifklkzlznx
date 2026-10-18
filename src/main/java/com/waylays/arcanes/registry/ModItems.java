package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.spell_books.SimpleAttributeSpellBook;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ArcanesOublies.MODID);

    /** Gold school spellbook: 10 slots, +10% Gold Spell Power, +200 max mana. */
    public static final RegistryObject<Item> GOLDEN_GRIMOIRE = ITEMS.register("golden_grimoire",
            () -> new SimpleAttributeSpellBook(10, SpellRarity.LEGENDARY, ModAttributes.GOLD_SPELL_POWER.get(), .10, 200));

    /** Explosion school spellbook: 10 slots, +10% Explosion Spell Power, +200 max mana. */
    public static final RegistryObject<Item> DETONATION_CODEX = ITEMS.register("detonation_codex",
            () -> new SimpleAttributeSpellBook(10, SpellRarity.LEGENDARY, ModAttributes.EXPLOSION_SPELL_POWER.get(), .10, 200));

    private ModItems() {}
}
