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
import javax.annotation.Nullable;

/**
 * One creative tab for the whole addon, laid out school by school (its spellbook first, then a scroll of each of its
 * spells at every level, in the order they were added). The school buttons beside the tab (client/CreativeTabFilter)
 * show one school at a time.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcanesOublies.MODID);

    /** A school's part of the tab: its spellbooks, then its scrolls. */
    public record Section(Supplier<SchoolType> school, Supplier<ItemStack> icon, List<Supplier<? extends Item>> books) {}

    public static final List<Section> SECTIONS = List.of(
            new Section(ModSchools.GOLD, () -> new ItemStack(ModItems.GOLDEN_GRIMOIRE.get()), List.of(ModItems.GOLDEN_GRIMOIRE)),
            new Section(ModSchools.EXPLOSION, () -> new ItemStack(ModItems.DETONATION_CODEX.get()), List.of(ModItems.DETONATION_CODEX)),
            new Section(ModSchools.BOOK, () -> new ItemStack(ModItems.LIVING_GRIMOIRE.get()), List.of(ModItems.LIVING_GRIMOIRE)),
            new Section(ModSchools.ERRETHYS, () -> scroll(ModSpells.MANA_DROMEDARY.get(), 1), List.of()),
            new Section(ModSchools.PHYSICAL, () -> scroll(ModSpells.PUNCH.get(), 1), List.of()),
            new Section(ModSchools.BOSS, () -> new ItemStack(ModItems.TYRANTS_CODEX.get()), List.of(ModItems.TYRANTS_CODEX)),
            new Section(ModSchools.RUNE, () -> new ItemStack(ModItems.ELDER_RUNE_CODEX.get()), List.of(ModItems.ELDER_RUNE_CODEX)),
            new Section(ModSchools.GLYPH, () -> new ItemStack(ModItems.SIX_INKS_CODEX.get()), List.of(ModItems.SIX_INKS_CODEX)),
            new Section(ModSchools.TOKEN, () -> new ItemStack(ModItems.SOUL_LEDGER.get()), List.of(ModItems.SOUL_LEDGER, ModItems.SOUL_TOKEN)),
            new Section(ModSchools.VERDIGRIS, () -> new ItemStack(ModItems.RUSTED_CODEX.get()), List.of(ModItems.RUSTED_CODEX)),
            new Section(ModSchools.WALL, () -> new ItemStack(ModItems.RAMPART_CODEX.get()), List.of(ModItems.RAMPART_CODEX)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + ArcanesOublies.MODID))
            .icon(() -> new ItemStack(ModItems.GOLDEN_GRIMOIRE.get()))
            .displayItems((parameters, output) -> {
                for (Section section : SECTIONS) {
                    section.books().forEach(book -> output.accept(book.get()));
                    for (var entry : ModSpells.SPELLS.getEntries()) {
                        AbstractSpell spell = entry.get();
                        if (!spell.isEnabled() || spell.getSchoolType() != section.school().get()) {
                            continue;
                        }
                        for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); level++) {
                            output.accept(scroll(spell, level));
                        }
                    }
                }
            })
            .build());

    private ModCreativeTabs() {}

    /** The section a stack of the tab belongs to: a scroll by its spell's school, a spellbook by the section listing it. */
    @Nullable
    public static Section sectionOf(ItemStack stack) {
        SchoolType school = stack.is(ItemRegistry.SCROLL.get()) && ISpellContainer.isSpellContainer(stack)
                ? ISpellContainer.get(stack).getSpellAtIndex(0).getSpell().getSchoolType() : null;
        for (Section section : SECTIONS) {
            if (section.school().get() == school || section.books().stream().anyMatch(book -> stack.is(book.get()))) {
                return section;
            }
        }
        return null;
    }

    private static ItemStack scroll(AbstractSpell spell, int level) {
        ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, level, scroll);
        return scroll;
    }
}
