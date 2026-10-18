package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.spells.gold.GildedChainsSpell;
import com.waylays.arcanes.spells.gold.GoldBoltSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Spells added to Iron's spell registry. Every spell class also needs {@code @AutoSpellConfig} and a no-arg
 * constructor, a spell icon at textures/gui/spell_icons/{@code <spell>}.png and its lang keys.
 */
public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ArcanesOublies.MODID);

    // Gold school
    public static final RegistryObject<AbstractSpell> GOLD_BOLT = register(new GoldBoltSpell());
    public static final RegistryObject<AbstractSpell> GILDED_CHAINS = register(new GildedChainsSpell());

    private ModSpells() {}

    private static RegistryObject<AbstractSpell> register(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }
}
