package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * One creative tab per school, side by side, so a spell is found without scrolling through all of them: the school's
 * spellbook first, then a scroll of each of its spells at every level, in the order they were added (Physical Prowess
 * goes style by style: bare hands, sword, dagger, scythe and so on).
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcanesOublies.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GOLD = schoolTab("gold", null, ModSchools.GOLD,
            () -> new ItemStack(ModItems.GOLDEN_GRIMOIRE.get()), List.of(ModItems.GOLDEN_GRIMOIRE));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXPLOSION = schoolTab("explosion", GOLD, ModSchools.EXPLOSION,
            () -> new ItemStack(ModItems.DETONATION_CODEX.get()), List.of(ModItems.DETONATION_CODEX));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BOOK = schoolTab("book", EXPLOSION, ModSchools.BOOK,
            () -> new ItemStack(ModItems.LIVING_GRIMOIRE.get()), List.of(ModItems.LIVING_GRIMOIRE));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ERRETHYS = schoolTab("errethys", BOOK, ModSchools.ERRETHYS,
            () -> scroll(ModSpells.MANA_DROMEDARY.get(), 1), List.of());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PHYSICAL = schoolTab("physical", ERRETHYS, ModSchools.PHYSICAL,
            () -> scroll(ModSpells.PUNCH.get(), 1), List.of());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BOSS = schoolTab("boss", PHYSICAL, ModSchools.BOSS,
            () -> new ItemStack(ModItems.TYRANTS_CODEX.get()), List.of(ModItems.TYRANTS_CODEX));

    private ModCreativeTabs() {}

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> schoolTab(String name, DeferredHolder<CreativeModeTab, CreativeModeTab> after,
                                                                              Supplier<SchoolType> school, Supplier<ItemStack> icon,
                                                                              List<Supplier<? extends Item>> books) {
        return TABS.register(name, () -> {
            CreativeModeTab.Builder builder = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ArcanesOublies.MODID + "." + name))
                    .icon(icon)
                    .displayItems((parameters, output) -> {
                        books.forEach(book -> output.accept(book.get()));
                        for (var entry : ModSpells.SPELLS.getEntries()) {
                            AbstractSpell spell = entry.get();
                            if (!spell.isEnabled() || spell.getSchoolType() != school.get()) {
                                continue;
                            }
                            for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); level++) {
                                output.accept(scroll(spell, level));
                            }
                        }
                    });
            if (after != null) {
                builder.withTabsBefore(after.getKey());
            }
            return builder.build();
        });
    }

    private static ItemStack scroll(AbstractSpell spell, int level) {
        ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, level, scroll);
        return scroll;
    }
}
