package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.registries.Registries;
import com.waylays.arcanes.spells.fire.HeatColors;
import com.waylays.arcanes.spells.fire.StarfireSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * One creative tab for the whole addon, laid out school by school (its spellbook first, then a scroll of each of its
 * spells at every level, in the order they were added). Stellar Fire is laid out by colour: a part for each of its
 * seven colours (red to violet, then green), each with every spell of that colour at every level. The bar of schools
 * beside the tab (client/CreativeTabFilter) shows one school (or one heat) at a time.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcanesOublies.MODID);

    /**
     * A part of the tab: its spellbooks, then the scrolls of the spells it takes ({@code takes}); a part of another
     * ({@code parent}, shown with it) has its own {@code title} and {@code color}, else the school's.
     */
    public record Section(Supplier<SchoolType> school, Supplier<ItemStack> icon, List<Supplier<? extends Item>> books, Predicate<AbstractSpell> takes,
                          @Nullable Section parent, @Nullable Supplier<Component> title, int color, @Nullable ResourceLocation picture) {
        /** A school's part: every spell of the school. */
        public Section(Supplier<SchoolType> school, Supplier<ItemStack> icon, List<Supplier<? extends Item>> books) {
            this(school, icon, books, spell -> spell.getSchoolType() == school.get(), null, null, -1, null);
        }

        public boolean sub() {
            return parent != null;
        }
    }

    /** Stellar Fire's own part: its codex (its spells are in its heats' parts, shown with it). */
    public static final Section STARFIRE = new Section(ModSchools.STARFIRE, () -> new ItemStack(ModItems.STARFIRE_CODEX.get()),
            List.of(ModItems.STARFIRE_CODEX), spell -> false, null, null, -1, null);

    /** A part for heat {@code tier} of Stellar Fire: every spell of that heat. */
    private static Section heat(int tier) {
        return new Section(ModSchools.STARFIRE, () -> new ItemStack(ModItems.STARFIRE_CODEX.get()), List.of(),
                spell -> spell instanceof StarfireSpell fire && fire.tier() == tier, STARFIRE,
                () -> Component.translatable("gui." + ArcanesOublies.MODID + ".creative.heat", Component.translatable("heat." + ArcanesOublies.MODID + "." + tier),
                        HeatColors.CLASS[tier]), HeatColors.BODY[tier],
                ArcanesOublies.id("textures/gui/spell_icons/ember_bolt_" + HeatColors.ID[tier] + ".png"));
    }

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
            new Section(ModSchools.WALL, () -> new ItemStack(ModItems.RAMPART_CODEX.get()), List.of(ModItems.RAMPART_CODEX)),
            new Section(ModSchools.GEM, () -> new ItemStack(ModItems.JEWELLERS_CODEX.get()), List.of(ModItems.JEWELLERS_CODEX)),
            new Section(ModSchools.COSMOS, () -> new ItemStack(ModItems.CODEX_OUT_OF_TIME.get()), List.of(ModItems.CODEX_OUT_OF_TIME)),
            new Section(ModSchools.BONE, () -> new ItemStack(ModItems.OSSUARY_CODEX.get()), List.of(ModItems.OSSUARY_CODEX)),
            new Section(ModSchools.WOOD, () -> new ItemStack(ModItems.SYLVAN_CODEX.get()), List.of(ModItems.SYLVAN_CODEX)),
            new Section(ModSchools.WATER, () -> new ItemStack(ModItems.TIDE_CODEX.get()), List.of(ModItems.TIDE_CODEX)),
            STARFIRE, heat(0), heat(1), heat(2), heat(3), heat(4), heat(5), heat(6),
            new Section(ModSchools.WIND, () -> new ItemStack(ModItems.WINDS_CODEX.get()), List.of(ModItems.WINDS_CODEX)),
            new Section(ModSchools.ECHO, () -> new ItemStack(ModItems.ECHOES_CODEX.get()), List.of(ModItems.ECHOES_CODEX)),
            new Section(ModSchools.SAND, () -> new ItemStack(ModItems.DUNES_CODEX.get()), List.of(ModItems.DUNES_CODEX)),
            new Section(ModSchools.LOTUS, () -> new ItemStack(ModItems.LOTUS_CODEX.get()), List.of(ModItems.LOTUS_CODEX)),
            new Section(ModSchools.AURORA, () -> new ItemStack(ModItems.BOREAL_CODEX.get()), List.of(ModItems.BOREAL_CODEX)),
            new Section(ModSchools.CLOCKWORK, () -> new ItemStack(ModItems.CLOCKMAKERS_CODEX.get()), List.of(ModItems.CLOCKMAKERS_CODEX)),
            new Section(ModSchools.MELODY, () -> new ItemStack(ModItems.SYMPHONY_CODEX.get()), List.of(ModItems.SYMPHONY_CODEX)),
            new Section(ModSchools.MEND, () -> new ItemStack(ModItems.CODEX_OF_MENDING.get()), List.of(ModItems.CODEX_OF_MENDING)),
            new Section(ModSchools.TRAP, () -> new ItemStack(ModItems.TRAPPERS_CODEX.get()), List.of(ModItems.TRAPPERS_CODEX)),
            new Section(ModSchools.SNOW, () -> new ItemStack(ModItems.FIRST_FROST_CODEX.get()), List.of(ModItems.FIRST_FROST_CODEX)),
            new Section(ModSchools.LAVA, () -> new ItemStack(ModItems.MAGMA_CODEX.get()), List.of(ModItems.MAGMA_CODEX)),
            new Section(ModSchools.PLANT, () -> new ItemStack(ModItems.HERBARIUM_CODEX.get()), List.of(ModItems.HERBARIUM_CODEX)),
            new Section(ModSchools.ACID, () -> new ItemStack(ModItems.CORROSIVE_CODEX.get()), List.of(ModItems.CORROSIVE_CODEX)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + ArcanesOublies.MODID))
            .icon(() -> new ItemStack(ModItems.GOLDEN_GRIMOIRE.get()))
            // A night-blue panel framed in gold, a gem for the scroll bar, gold titles, a gold glow on the slot under the mouse
            .backgroundTexture(ArcanesOublies.id("textures/gui/creative/tab.png"))
            .withScrollBarSpriteLocation(ArcanesOublies.id("creative/scroller"))
            .withLabelColor(0xFFD77A)
            .withSlotColor(0x60FFD77A)
            .displayItems((parameters, output) -> {
                // The rituals are not a school: their chalk comes first
                output.accept(ModItems.RITUAL_CHALK.get());
                output.accept(ModItems.ALTAR_OF_RITES.get());
                output.accept(ModItems.ALEMBIC.get());
                output.accept(ModItems.MEDICAL_TABLE.get());
                for (Section section : SECTIONS) {
                    section.books().forEach(book -> output.accept(book.get()));
                    for (var entry : ModSpells.SPELLS.getEntries()) {
                        AbstractSpell spell = entry.get();
                        if (!spell.isEnabled() || !section.takes().test(spell)) {
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

    /** The section a stack of the tab belongs to: a scroll by the section taking its spell, a spellbook by the section listing it. */
    @Nullable
    public static Section sectionOf(ItemStack stack) {
        AbstractSpell spell = stack.is(ItemRegistry.SCROLL.get()) && ISpellContainer.isSpellContainer(stack)
                ? ISpellContainer.get(stack).getSpellAtIndex(0).getSpell() : null;
        for (Section section : SECTIONS) {
            if (spell != null ? section.takes().test(spell) : section.books().stream().anyMatch(book -> stack.is(book.get()))) {
                return section;
            }
        }
        return null;
    }

    /** Whether choosing {@code section} shows {@code stack}: it is the section's own, or one of its parts'. */
    public static boolean shows(Section section, ItemStack stack) {
        Section of = sectionOf(stack);
        return of == section || of != null && of.parent() == section;
    }

    /** Whether {@code section} (or one of its parts) takes {@code spell}. */
    public static boolean covers(Section section, AbstractSpell spell) {
        for (Section s : SECTIONS) {
            if ((s == section || s.parent() == section) && s.takes().test(spell)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack scroll(AbstractSpell spell, int level) {
        ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, level, scroll);
        return scroll;
    }
}
