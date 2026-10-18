package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * One tab with the addon's spellbooks and a scroll of every addon spell at every level (handy for staff testing).
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcanesOublies.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + ArcanesOublies.MODID + ".main"))
            .icon(() -> new ItemStack(ModItems.GOLDEN_GRIMOIRE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.GOLDEN_GRIMOIRE.get());
                output.accept(ModItems.DETONATION_CODEX.get());
                for (RegistryObject<AbstractSpell> entry : ModSpells.SPELLS.getEntries()) {
                    AbstractSpell spell = entry.get();
                    if (!spell.isEnabled()) {
                        continue;
                    }
                    for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); level++) {
                        ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
                        ISpellContainer.createScrollContainer(spell, level, scroll).save(scroll);
                        output.accept(scroll);
                    }
                }
            })
            .build());

    private ModCreativeTabs() {}
}
