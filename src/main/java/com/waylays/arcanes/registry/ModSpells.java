package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.spells.gold.AurumBladeSpell;
import com.waylays.arcanes.spells.gold.GildedArmorSpell;
import com.waylays.arcanes.spells.gold.GildedChainsSpell;
import com.waylays.arcanes.spells.gold.GoldBoltSpell;
import com.waylays.arcanes.spells.gold.GoldenSentinelsSpell;
import com.waylays.arcanes.spells.gold.GoldenSpikesSpell;
import com.waylays.arcanes.spells.gold.GoldfallSpell;
import com.waylays.arcanes.spells.gold.MidasCurseSpell;
import com.waylays.arcanes.spells.gold.MoltenTorrentSpell;
import com.waylays.arcanes.spells.gold.SunOfEldoradoSpell;
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
    public static final RegistryObject<AbstractSpell> GOLDFALL = register(new GoldfallSpell());
    public static final RegistryObject<AbstractSpell> GOLDEN_SPIKES = register(new GoldenSpikesSpell());
    public static final RegistryObject<AbstractSpell> MIDAS_CURSE = register(new MidasCurseSpell());
    public static final RegistryObject<AbstractSpell> AURUM_BLADE = register(new AurumBladeSpell());
    public static final RegistryObject<AbstractSpell> MOLTEN_TORRENT = register(new MoltenTorrentSpell());
    public static final RegistryObject<AbstractSpell> GILDED_ARMOR = register(new GildedArmorSpell());
    public static final RegistryObject<AbstractSpell> GOLDEN_SENTINELS = register(new GoldenSentinelsSpell());
    public static final RegistryObject<AbstractSpell> SUN_OF_ELDORADO = register(new SunOfEldoradoSpell());

    private ModSpells() {}

    private static RegistryObject<AbstractSpell> register(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }
}
