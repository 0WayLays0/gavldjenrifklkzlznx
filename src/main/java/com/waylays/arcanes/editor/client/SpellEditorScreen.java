package com.waylays.arcanes.editor.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.waylays.arcanes.editor.SpellEditorNet;
import com.waylays.arcanes.editor.SpellTuning;
import com.waylays.arcanes.editor.SpellTuning.Stat;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The spell editor ("Atelier des sorts"), for game masters: every spell of every mod, Iron's own and its addons'.
 * On the left, the spells (searched by name, sorted, filtered by mod, by school, or to those changed); on the right, the
 * chosen spell and all its stats in tabs: General (on or off, crafting, school, rarity, levels, cooldown), Power and
 * mana (Iron's multipliers, its base power and mana and how they grow, its cast time, extra levels), Combat (damage,
 * fire, freezing, life stolen), Add-ons (every parameter other mods declare to Iron's config) and a Preview (each
 * level's mana, power, cooldown and cast time, as they will be). Numbers are typed, dragged sideways, scrolled or
 * stepped; a changed stat shows its default and a button to go back to it. Changes wait until saved; they can be
 * applied at once to every spell in the list.
 */
public class SpellEditorScreen extends Screen {
    private static final int GOLD = 0xFFDEB25C, GOLD_LIGHT = 0xFFFFE6A0, GOLD_DARK = 0xFF7A5A22, TEXT = 0xFFF2ECFF, DIM = 0xFFA79CC6,
            FAINT = 0xFF6E6492, CHANGED = 0xFFFFC857, PENDING = 0xFF7FE0FF, RED = 0xFFFF7070, GREEN = 0xFF86E89A;
    private static final int ROW = 18, LIST_W = 168, LINE = 20;
    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "none");

    private enum Tab {GENERAL, POWER, COMBAT, ADDONS, PREVIEW}

    private enum Kind {NUMBER, BOOL, CHOICE, TEXT}

    private enum Sort {NAME, SCHOOL, MOD}

    /** A stat the editor shows: one of Iron's config parameters or one of the editor's own. */
    private static final class Entry {
        final String id;
        final Component label;
        @Nullable
        final Component tip;
        final Kind kind;
        double min = -1e9, max = 1e9, step = 1;
        boolean integer;
        String unit = "";
        List<Object> choices = List.of();
        Function<Object, Component> choiceName = o -> Component.literal(String.valueOf(o));
        @Nullable
        SpellConfigParameter<?> param;
        @Nullable
        Stat stat;

        Entry(String id, Component label, @Nullable Component tip, Kind kind) {
            this.id = id;
            this.label = label;
            this.tip = tip;
            this.kind = kind;
        }
    }

    /** A place on the screen that does something when clicked, and may say what it does. */
    private record Hot(int x0, int y0, int x1, int y1, @Nullable Runnable click, @Nullable List<Component> tip) {
        boolean in(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    /** A text being typed: the search, or a value. */
    private static final class Input {
        final String target;
        String text;

        Input(String target, String text) {
            this.target = target;
            this.text = text;
        }
    }

    // What is shown
    private final List<AbstractSpell> spells = new ArrayList<>();
    private final List<AbstractSpell> shown = new ArrayList<>();
    private final List<String> mods = new ArrayList<>();
    private final List<SchoolType> schools = new ArrayList<>();
    private final Map<Tab, List<Entry>> entries = new LinkedHashMap<>();
    @Nullable
    private AbstractSpell selected;
    private Tab tab = Tab.GENERAL;

    // What is being changed: per spell, the entries' new values
    private final Map<ResourceLocation, Map<String, Object>> pending = new LinkedHashMap<>();

    // Filters and scrolling
    private static String lastSearch = "";
    private static int lastMod = -1, lastSchool = -1;
    private static boolean lastOnlyChanged;
    private static Sort lastSort = Sort.NAME;
    @Nullable
    private static ResourceLocation lastSelected;
    private String search = lastSearch;
    private int modFilter = lastMod, schoolFilter = lastSchool;
    private boolean onlyChanged = lastOnlyChanged;
    private Sort sort = lastSort;
    private double listScroll, contentScroll;
    private int previewLevel = 1;

    // Interaction
    private final List<Hot> hots = new ArrayList<>();
    @Nullable
    private Input input;
    @Nullable
    private Entry armed;
    private double armX;
    private double armValue;
    private boolean dragged;
    @Nullable
    private String confirming;
    private long confirmAt;
    private Component status = Component.empty();
    private int statusColor = DIM;
    private long statusAt;
    private boolean escapeWarned;

    // Window
    private int left, top, w, h;

    public SpellEditorScreen() {
        super(Component.translatable("ui.arcanes_oublies.editor.title"));
    }

    // ---------------------------------------------------------------- set-up

    @Override
    protected void init() {
        w = Math.min(500, width - 8);
        h = Math.min(290, height - 8);
        left = (width - w) / 2;
        top = (height - h) / 2;
        refresh();
    }

    /** Reads the spells and their stats again (after a save, or anyone's). */
    void refresh() {
        spells.clear();
        for (AbstractSpell spell : SpellRegistry.REGISTRY) {
            if (spell != null && !NONE.equals(spell.getSpellResource())) {
                spells.add(spell);
            }
        }
        mods.clear();
        spells.stream().map(s -> s.getSpellResource().getNamespace()).distinct().sorted().forEach(mods::add);
        schools.clear();
        for (SchoolType school : SchoolRegistry.REGISTRY) {
            schools.add(school);
        }
        schools.sort(Comparator.comparing(s -> s.getId().toString()));
        buildEntries();
        if (modFilter >= mods.size()) {
            modFilter = -1;
        }
        if (schoolFilter >= schools.size()) {
            schoolFilter = -1;
        }
        filter();
        if (selected == null && lastSelected != null) {
            selected = spells.stream().filter(s -> s.getSpellResource().equals(lastSelected)).findFirst().orElse(null);
        }
        if (selected == null && !shown.isEmpty()) {
            selected = shown.get(0);
        }
    }

    private void filter() {
        String q = search.toLowerCase(Locale.ROOT).trim();
        shown.clear();
        for (AbstractSpell spell : spells) {
            ResourceLocation id = spell.getSpellResource();
            if (modFilter >= 0 && !id.getNamespace().equals(mods.get(modFilter))) {
                continue;
            }
            if (schoolFilter >= 0 && spell.getSchoolType() != schools.get(schoolFilter)) {
                continue;
            }
            if (onlyChanged && !changed(spell) && !pending.containsKey(id)) {
                continue;
            }
            if (!q.isEmpty() && !name(spell).getString().toLowerCase(Locale.ROOT).contains(q) && !id.toString().contains(q)) {
                continue;
            }
            shown.add(spell);
        }
        Comparator<AbstractSpell> byName = Comparator.comparing(s -> name(s).getString().toLowerCase(Locale.ROOT));
        shown.sort(switch (sort) {
            case NAME -> byName;
            case SCHOOL -> Comparator.<AbstractSpell, String>comparing(s -> s.getSchoolType().getId().toString()).thenComparing(byName);
            case MOD -> Comparator.<AbstractSpell, String>comparing(s -> s.getSpellResource().getNamespace()).thenComparing(byName);
        });
        listScroll = Mth.clamp(listScroll, 0, maxListScroll());
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("ui.arcanes_oublies.editor." + key, args);
    }

    private static Component name(AbstractSpell spell) {
        return Component.translatable(spell.getComponentId());
    }

    private static int schoolColor(SchoolType school) {
        TextColor c = school.getDisplayName().getStyle().getColor();
        return c == null ? 0xFFB0A8D0 : 0xFF000000 | c.getValue();
    }

    private static String modName(String namespace) {
        return ModList.get().getModContainerById(namespace).map(c -> c.getModInfo().getDisplayName()).orElse(namespace);
    }

    // ---------------------------------------------------------------- the stats

    private void buildEntries() {
        entries.clear();
        for (Tab t : Tab.values()) {
            entries.put(t, new ArrayList<>());
        }
        Map<String, SpellConfigParameter<?>> iron = new LinkedHashMap<>();
        for (SpellConfigParameter<?> p : SpellConfigManager.ALL_TYPES) {
            iron.put(p.key().toString(), p);
        }
        List<Entry> general = entries.get(Tab.GENERAL), power = entries.get(Tab.POWER), combat = entries.get(Tab.COMBAT);
        addIron(general, iron.remove("irons_spellbooks:enabled"), "enabled", 0, 0, 0);
        addIron(general, iron.remove("irons_spellbooks:allow_crafting"), "allow_crafting", 0, 0, 0);
        addIron(general, iron.remove("irons_spellbooks:school"), "school", 0, 0, 0);
        addIron(general, iron.remove("irons_spellbooks:min_rarity"), "min_rarity", 0, 0, 0);
        addIron(general, iron.remove("irons_spellbooks:max_level"), "max_level", 1, 100, 1);
        addIron(general, iron.remove("irons_spellbooks:cooldown_in_seconds"), "cooldown", 0, 3600, .5);
        addIron(power, iron.remove("irons_spellbooks:power_multiplier"), "power_multiplier", 0, 100, .05);
        addIron(power, iron.remove("irons_spellbooks:mana_cost_multiplier"), "mana_multiplier", 0, 100, .05);
        for (Stat stat : List.of(Stat.BASE_POWER, Stat.POWER_PER_LEVEL, Stat.BASE_MANA, Stat.MANA_PER_LEVEL, Stat.CAST_TIME, Stat.BONUS_LEVELS)) {
            addStat(power, stat);
        }
        for (Stat stat : List.of(Stat.DAMAGE_MULT, Stat.FIRE_SECONDS, Stat.FREEZE_SECONDS, Stat.LIFESTEAL)) {
            addStat(combat, stat);
        }
        // Whatever else the installed mods declare
        iron.values().stream().sorted(Comparator.comparing(p -> p.key().toString()))
                .forEach(p -> addIron(entries.get(Tab.ADDONS), p, null, -1e9, 1e9, 1));
    }

    private void addStat(List<Entry> list, Stat stat) {
        if (!SpellTuning.supports(stat)) {
            return;
        }
        Entry e = new Entry("tune:" + stat.key, tr("stat." + stat.key), tr("stat." + stat.key + ".tip"), Kind.NUMBER);
        e.stat = stat;
        e.min = stat.min;
        e.max = stat.max;
        e.step = stat.step;
        e.integer = stat.integer;
        e.unit = switch (stat) {
            case CAST_TIME -> " t";
            case FIRE_SECONDS, FREEZE_SECONDS -> " s";
            case LIFESTEAL -> " %";
            case DAMAGE_MULT -> "×";
            default -> "";
        };
        list.add(e);
    }

    private void addIron(List<Entry> list, @Nullable SpellConfigParameter<?> param, @Nullable String key, double min, double max, double step) {
        if (param == null) {
            return;
        }
        Object def = param.defaultValue().get();
        Component label;
        Component tip;
        if (key != null) {
            label = tr("stat." + key);
            tip = tr("stat." + key + ".tip");
        } else {
            label = Component.literal(pretty(param.key().getPath()));
            tip = Component.literal(param.key().toString()).withStyle(ChatFormatting.GRAY);
        }
        Entry e;
        if (def instanceof Boolean) {
            e = new Entry("iron:" + param.key(), label, tip, Kind.BOOL);
        } else if (def instanceof Number n) {
            e = new Entry("iron:" + param.key(), label, tip, Kind.NUMBER);
            e.integer = n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte;
            e.min = min;
            e.max = max;
            e.step = e.integer ? Math.max(1, Math.round(step)) : (key == null ? .1 : step);
            if ("cooldown".equals(key)) {
                e.unit = " s";
            } else if (key != null && key.endsWith("multiplier")) {
                e.unit = "×";
            }
        } else if (def instanceof SchoolType) {
            e = new Entry("iron:" + param.key(), label, tip, Kind.CHOICE);
            e.choices = new ArrayList<>(schools);
            e.choiceName = o -> ((SchoolType) o).getDisplayName();
        } else if (def instanceof SpellRarity) {
            e = new Entry("iron:" + param.key(), label, tip, Kind.CHOICE);
            e.choices = List.of((Object[]) SpellRarity.values());
            e.choiceName = o -> ((SpellRarity) o).getDisplayName();
        } else if (def instanceof Enum<?> en) {
            e = new Entry("iron:" + param.key(), label, tip, Kind.CHOICE);
            e.choices = List.of((Object[]) en.getDeclaringClass().getEnumConstants());
            e.choiceName = o -> Component.literal(pretty(((Enum<?>) o).name().toLowerCase(Locale.ROOT)));
        } else {
            e = new Entry("iron:" + param.key(), label, tip, Kind.TEXT);
        }
        e.param = param;
        list.add(e);
    }

    private static String pretty(String path) {
        String s = path.replace('_', ' ').replace('/', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** The entry's value as it is now (saved). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object current(AbstractSpell spell, Entry e) {
        if (e.stat != null) {
            return SpellTuning.current(spell, e.stat);
        }
        return SpellConfigManager.getSpellConfigValue(spell, (SpellConfigParameter) e.param);
    }

    /** The entry's value as the spell's mod made it. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object def(AbstractSpell spell, Entry e) {
        if (e.stat != null) {
            return SpellTuning.original(spell, e.stat);
        }
        return SpellConfigManager.getSpellDefaultConfigValue(spell, (SpellConfigParameter) e.param);
    }

    /** The entry's value as shown: changed and not yet saved, or as it is. */
    private Object value(AbstractSpell spell, Entry e) {
        Map<String, Object> p = pending.get(spell.getSpellResource());
        if (p != null && p.containsKey(e.id)) {
            return p.get(e.id);
        }
        return current(spell, e);
    }

    private boolean isPending(AbstractSpell spell, Entry e) {
        Map<String, Object> p = pending.get(spell.getSpellResource());
        return p != null && p.containsKey(e.id);
    }

    private static boolean same(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) {
            return Math.abs(x.doubleValue() - y.doubleValue()) < 1e-6;
        }
        return Objects.equals(a, b);
    }

    /** Whether any of the spell's stats differs from its default (saved values). */
    private boolean changed(AbstractSpell spell) {
        for (List<Entry> list : entries.values()) {
            for (Entry e : list) {
                if (!same(current(spell, e), def(spell, e))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void set(AbstractSpell spell, Entry e, Object v) {
        if (v instanceof Number n && e.kind == Kind.NUMBER) {
            double d = Mth.clamp(n.doubleValue(), e.min, e.max);
            if (e.integer) {
                d = Math.round(d);
            } else {
                d = Math.round(d * 1000) / 1000.0;
            }
            v = numberLike(e, d);
        }
        Map<String, Object> p = pending.computeIfAbsent(spell.getSpellResource(), k -> new LinkedHashMap<>());
        if (same(v, current(spell, e))) {
            p.remove(e.id);
        } else {
            p.put(e.id, v);
        }
        if (p.isEmpty()) {
            pending.remove(spell.getSpellResource());
        }
        escapeWarned = false;
    }

    /** {@code d} in the type the entry's parameter wants. */
    private static Object numberLike(Entry e, double d) {
        if (e.stat != null) {
            return d;
        }
        Object def = e.param.defaultValue().get();
        if (def instanceof Integer) {
            return (int) Math.round(d);
        } else if (def instanceof Long) {
            return Math.round(d);
        } else if (def instanceof Float) {
            return (float) d;
        } else if (def instanceof Short) {
            return (short) Math.round(d);
        } else if (def instanceof Byte) {
            return (byte) Math.round(d);
        }
        return d;
    }

    private static String fmt(Object v, Entry e) {
        if (v instanceof Number n) {
            double d = n.doubleValue();
            String s = e.integer || d == Math.rint(d) && Math.abs(d) < 1e12 ? String.valueOf(Math.round(d))
                    : String.format(Locale.ROOT, "%.3f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
            return s + e.unit;
        }
        if (v instanceof Boolean b) {
            return b ? "✔" : "✘";
        }
        return String.valueOf(v);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String json(Entry e, Object v) {
        Codec codec = e.param.datatype();
        JsonElement el = (JsonElement) codec.encodeStart(JsonOps.INSTANCE, v).getOrThrow();
        return el.toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    private static Object parseJson(Entry e, String text) {
        try {
            Codec codec = e.param.datatype();
            Object decoded = codec.decode(JsonOps.INSTANCE, JsonParser.parseString(text)).getOrThrow();
            return ((com.mojang.datafixers.util.Pair<?, ?>) decoded).getFirst();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    // ---------------------------------------------------------------- saving

    private int pendingCount() {
        return pending.size();
    }

    /** The edit packet's two maps for {@code changes} of {@code spell}. */
    private void encode(AbstractSpell spell, Map<String, Object> changes, Map<String, String> iron, Map<String, Double> tune) {
        for (List<Entry> list : entries.values()) {
            for (Entry e : list) {
                if (!changes.containsKey(e.id)) {
                    continue;
                }
                Object v = changes.get(e.id);
                if (e.stat != null) {
                    tune.put(e.stat.key, same(v, SpellTuning.original(spell, e.stat)) ? Double.NaN : ((Number) v).doubleValue());
                } else {
                    try {
                        iron.put(e.param.key().toString(), same(v, def(spell, e)) ? "" : json(e, v));
                    } catch (RuntimeException ex) {
                        setStatus(tr("bad_value", e.label), RED);
                    }
                }
            }
        }
    }

    private void save() {
        commitInput();
        if (pending.isEmpty()) {
            setStatus(tr("nothing_to_save"), DIM);
            return;
        }
        int n = 0;
        for (Map.Entry<ResourceLocation, Map<String, Object>> en : pending.entrySet()) {
            AbstractSpell spell = SpellRegistry.getSpell(en.getKey());
            if (spell == null) {
                continue;
            }
            Map<String, String> iron = new LinkedHashMap<>();
            Map<String, Double> tune = new LinkedHashMap<>();
            encode(spell, en.getValue(), iron, tune);
            PacketDistributor.sendToServer(new SpellEditorNet.Edit(List.of(en.getKey()), iron, tune, false));
            n++;
        }
        pending.clear();
        setStatus(tr("saving", n), PENDING);
        click();
    }

    /** The chosen spell's changes, made to every spell in the list. */
    private void applyToList() {
        commitInput();
        if (selected == null) {
            return;
        }
        Map<String, Object> changes = pending.get(selected.getSpellResource());
        if (changes == null || changes.isEmpty()) {
            setStatus(tr("apply_nothing"), RED);
            return;
        }
        if (!confirm("apply")) {
            setStatus(tr("apply_confirm", shown.size()), CHANGED);
            return;
        }
        List<ResourceLocation> ids = shown.stream().map(AbstractSpell::getSpellResource).toList();
        Map<String, String> iron = new LinkedHashMap<>();
        Map<String, Double> tune = new LinkedHashMap<>();
        encode(selected, changes, iron, tune);
        // The same values for all (a value equal to one spell's default is still written for the others)
        for (List<Entry> list : entries.values()) {
            for (Entry e : list) {
                if (changes.containsKey(e.id) && e.param != null && "".equals(iron.get(e.param.key().toString()))) {
                    iron.put(e.param.key().toString(), json(e, changes.get(e.id)));
                }
                if (changes.containsKey(e.id) && e.stat != null && Double.isNaN(tune.getOrDefault(e.stat.key, 0.0))) {
                    tune.put(e.stat.key, ((Number) changes.get(e.id)).doubleValue());
                }
            }
        }
        send(ids, iron, tune, false);
        for (ResourceLocation id : ids) {
            pending.remove(id);
        }
        setStatus(tr("saving", ids.size()), PENDING);
        click();
    }

    /** Every spell in the list back to its defaults (Iron's parameters and the editor's stats). */
    private void resetList() {
        if (!confirm("reset_list")) {
            setStatus(tr("reset_list_confirm", shown.size()), CHANGED);
            return;
        }
        List<ResourceLocation> ids = shown.stream().map(AbstractSpell::getSpellResource).toList();
        send(ids, Map.of(), Map.of(), true);
        ids.forEach(pending::remove);
        setStatus(tr("saving", ids.size()), PENDING);
        click();
    }

    private static void send(List<ResourceLocation> ids, Map<String, String> iron, Map<String, Double> tune, boolean reset) {
        for (int i = 0; i < ids.size(); i += SpellEditorNet.Edit.MAX_SPELLS) {
            List<ResourceLocation> part = ids.subList(i, Math.min(ids.size(), i + SpellEditorNet.Edit.MAX_SPELLS));
            PacketDistributor.sendToServer(new SpellEditorNet.Edit(new ArrayList<>(part), iron, tune, reset));
        }
    }

    /** The chosen spell's stats set back to their defaults (to be saved). */
    private void resetSpell() {
        if (selected == null) {
            return;
        }
        for (List<Entry> list : entries.values()) {
            for (Entry e : list) {
                set(selected, e, def(selected, e));
            }
        }
        setStatus(tr("reset_done"), CHANGED);
        click();
    }

    private void discard(boolean all) {
        if (all) {
            pending.clear();
        } else if (selected != null) {
            pending.remove(selected.getSpellResource());
        }
        input = null;
        setStatus(tr("discarded"), DIM);
        click();
    }

    /** True on the second click within a few seconds. */
    private boolean confirm(String what) {
        long now = Util.getMillis();
        if (what.equals(confirming) && now - confirmAt < 4000) {
            confirming = null;
            return true;
        }
        confirming = what;
        confirmAt = now;
        return false;
    }

    void onSaved(int spellsChanged, List<String> errors) {
        if (errors.isEmpty()) {
            setStatus(tr("saved", spellsChanged), GREEN);
        } else {
            setStatus(tr("saved_errors", spellsChanged, String.join(" · ", errors)), RED);
        }
        refresh();
    }

    private void setStatus(Component text, int color) {
        status = text;
        statusColor = color;
        statusAt = Util.getMillis();
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, .5f));
    }

    // ---------------------------------------------------------------- layout

    private int listX0() {
        return left + 8;
    }

    private int listY0() {
        return top + 78;
    }

    private int listY1() {
        return top + h - 10;
    }

    private int listRows() {
        return Math.max(1, (listY1() - listY0()) / ROW);
    }

    private double maxListScroll() {
        return Math.max(0, shown.size() - listRows());
    }

    private int panelX0() {
        return left + LIST_W + 16;
    }

    private int panelX1() {
        return left + w - 8;
    }

    private int contentY0() {
        return top + 86;
    }

    private int contentY1() {
        return top + h - 38;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        hots.clear();
        long now = Util.getMillis();
        drawWindow(g, now);
        drawHeader(g, mx, my);
        drawFilters(g, mx, my);
        drawList(g, mx, my);
        if (selected != null) {
            drawSpell(g, mx, my, now);
        }
        drawButtons(g, mx, my);
        // The tooltip of what the mouse is over
        if (input == null || armed == null) {
            for (int i = hots.size() - 1; i >= 0; i--) {
                Hot hot = hots.get(i);
                if (hot.in(mx, my)) {
                    if (hot.tip != null && !hot.tip.isEmpty()) {
                        List<FormattedCharSequence> lines = new ArrayList<>();
                        for (Component c : hot.tip) {
                            lines.addAll(font.split(c, 220));
                        }
                        g.renderTooltip(font, lines, mx, my);
                    }
                    break;
                }
            }
        }
    }

    private void drawWindow(GuiGraphics g, long now) {
        int x0 = left, y0 = top, x1 = left + w, y1 = top + h;
        // Its shadow, its night, its frame of gold
        g.fill(x0 - 3, y0 - 2, x1 + 4, y1 + 5, 0x60000000);
        g.fillGradient(x0, y0, x1, y1, 0xF6221642, 0xF60B0920);
        // Faint stars, twinkling
        for (int i = 0; i < 70; i++) {
            int sx = x0 + 4 + (int) ((i * 7919L) % (w - 8));
            int sy = y0 + 4 + (int) ((i * 104729L) % (h - 8));
            float tw = .5f + .5f * Mth.sin(now / 700f + i * 1.7f);
            int a = (int) (20 + 70 * tw);
            g.fill(sx, sy, sx + 1, sy + 1, a << 24 | 0xDCD4FF);
        }
        frame(g, x0, y0, x1, y1, GOLD_DARK, GOLD);
        // The header's band
        g.fillGradient(x0 + 2, y0 + 2, x1 - 2, y0 + 24, 0x803A2470, 0x10221642);
        g.fill(x0 + 6, y0 + 24, x1 - 6, y0 + 25, 0x80DEB25C);
        // Corner gems
        for (int[] c : new int[][]{{x0, y0}, {x1 - 1, y0}, {x0, y1 - 1}, {x1 - 1, y1 - 1}}) {
            diamond(g, c[0], c[1], 3, GOLD_LIGHT);
        }
        // The two panels
        inset(g, left + 6, top + 28, left + LIST_W + 10, top + h - 8);
        inset(g, panelX0() - 2, top + 28, panelX1() + 2, top + h - 8);
    }

    private static void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int outer, int inner) {
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, outer);
        g.renderOutline(x0 + 1, y0 + 1, x1 - x0 - 2, y1 - y0 - 2, inner);
    }

    private static void inset(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, 0x700A0618);
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, 0xFF3A2E64);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0x30FFFFFF);
    }

    private static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int i = -r; i <= r; i++) {
            int half = r - Math.abs(i);
            g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
        }
    }

    private void text(GuiGraphics g, Component c, int x, int y, int color, float scale) {
        if (scale == 1) {
            g.drawString(font, c, x, y, color, false);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, c, 0, 0, color, false);
        g.pose().popPose();
    }

    /** {@code c} cut to {@code width} pixels with an ellipsis. */
    private Component fit(Component c, int width) {
        if (font.width(c) <= width) {
            return c;
        }
        String s = c.getString();
        while (!s.isEmpty() && font.width(s + "…") > width) {
            s = s.substring(0, s.length() - 1);
        }
        return Component.literal(s + "…").withStyle(c.getStyle());
    }

    private void drawHeader(GuiGraphics g, int mx, int my) {
        diamond(g, left + 12, top + 12, 3, GOLD);
        text(g, title, left + 20, top + 7, GOLD_LIGHT, 1.25f);
        long changed = spells.stream().filter(this::changed).count();
        Component counts = tr("counts", spells.size(), changed, pendingCount());
        int cw = font.width(counts);
        text(g, counts, left + w - 26 - cw, top + 9, DIM, 1);
        // Close
        int cx = left + w - 18, cy = top + 6;
        boolean hover = mx >= cx && mx < cx + 12 && my >= cy && my < cy + 12;
        g.fill(cx, cy, cx + 12, cy + 12, hover ? 0x60FF7070 : 0x30FFFFFF);
        g.drawString(font, "✕", cx + 3, cy + 2, hover ? 0xFFFFFFFF : DIM, false);
        hots.add(new Hot(cx, cy, cx + 12, cy + 12, this::onClose, List.of(tr("close"))));
    }

    private void drawFilters(GuiGraphics g, int mx, int my) {
        int x0 = left + 10, x1 = left + LIST_W + 6;
        // The search
        int sy = top + 32;
        boolean typing = input != null && input.target.equals("search");
        g.fill(x0, sy, x1, sy + 14, typing ? 0xFF1C1440 : 0xFF140E30);
        g.renderOutline(x0, sy, x1 - x0, 14, typing ? GOLD : 0xFF4A3C7C);
        magnifier(g, x0 + 4, sy + 3, typing ? GOLD_LIGHT : DIM);
        String q = typing ? input.text : search;
        if (q.isEmpty() && !typing) {
            text(g, tr("search"), x0 + 15, sy + 3, FAINT, 1);
        } else {
            String shownText = q;
            while (font.width(shownText) > x1 - x0 - 20) {
                shownText = shownText.substring(1);
            }
            g.drawString(font, shownText + (typing && (Util.getMillis() / 400) % 2 == 0 ? "_" : ""), x0 + 15, sy + 3, TEXT, false);
        }
        hots.add(new Hot(x0, sy, x1, sy + 14, () -> input = new Input("search", search), null));
        // The filters
        int fy = top + 49, half = (x1 - x0 - 2) / 2;
        pill(g, mx, my, x0, fy, x0 + half, tr("mod", modFilter < 0 ? tr("all").getString() : modName(mods.get(modFilter))), modFilter >= 0, () -> {
            modFilter = cycle(modFilter, mods.size());
            filter();
        }, List.of(tr("mod.tip")));
        pill(g, mx, my, x0 + half + 2, fy, x1, schoolFilter < 0 ? tr("school", tr("all_f").getString())
                        : tr("school", schools.get(schoolFilter).getDisplayName().getString()), schoolFilter >= 0, () -> {
                    schoolFilter = cycle(schoolFilter, schools.size());
                    filter();
                }, List.of(tr("school.tip")));
        int fy2 = top + 63;
        pill(g, mx, my, x0, fy2, x0 + half, tr("only_changed"), onlyChanged, () -> {
            onlyChanged = !onlyChanged;
            filter();
        }, List.of(tr("only_changed.tip")));
        pill(g, mx, my, x0 + half + 2, fy2, x1, tr("sort", tr("sort." + sort.name().toLowerCase(Locale.ROOT)).getString()), false, () -> {
            sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
            filter();
        }, null);
    }

    /** -1 (all), 0 .. n-1, then -1 again; shift goes backwards. */
    private static int cycle(int i, int n) {
        if (Screen.hasShiftDown()) {
            return i < 0 ? n - 1 : i - 1;
        }
        return i + 1 >= n ? -1 : i + 1;
    }

    private void magnifier(GuiGraphics g, int x, int y, int color) {
        g.renderOutline(x, y, 6, 6, color);
        g.fill(x + 5, y + 5, x + 6, y + 6, color);
        g.fill(x + 6, y + 6, x + 8, y + 8, color);
    }

    private void pill(GuiGraphics g, int mx, int my, int x0, int y0, int x1, Component label, boolean on, Runnable action, @Nullable List<Component> tip) {
        boolean hover = mx >= x0 && mx < x1 && my >= y0 && my < y0 + 12;
        g.fill(x0, y0, x1, y0 + 12, on ? 0xC05A3E14 : hover ? 0xC0302460 : 0xC01A1238);
        g.renderOutline(x0, y0, x1 - x0, 12, on ? GOLD : hover ? 0xFF8070C0 : 0xFF3E3270);
        Component t = fit(label, x1 - x0 - 6);
        g.drawString(font, t, (x0 + x1 - font.width(t)) / 2, y0 + 2, on ? GOLD_LIGHT : TEXT, false);
        hots.add(new Hot(x0, y0, x1, y0 + 12, () -> {
            action.run();
            click();
        }, tip));
    }

    private void drawList(GuiGraphics g, int mx, int my) {
        int x0 = listX0() + 2, x1 = left + LIST_W + 2, y0 = listY0(), y1 = listY1();
        g.enableScissor(x0, y0, x1 + 6, y1);
        int first = (int) listScroll;
        float frac = (float) (listScroll - first);
        for (int i = 0; i <= listRows() + 1; i++) {
            int index = first + i;
            if (index >= shown.size()) {
                break;
            }
            AbstractSpell spell = shown.get(index);
            int y = y0 + (int) ((i - frac) * ROW);
            boolean sel = spell == selected;
            boolean hover = mx >= x0 && mx < x1 && my >= Math.max(y, y0) && my < Math.min(y + ROW, y1);
            if (sel) {
                g.fillGradient(x0, y, x1, y + ROW - 1, 0xC0604014, 0xC0301A0A);
                g.renderOutline(x0, y, x1 - x0, ROW - 1, GOLD);
            } else if (hover) {
                g.fill(x0, y, x1, y + ROW - 1, 0x40FFFFFF);
            }
            int sc = schoolColor(spell.getSchoolType());
            g.fill(x0, y + 1, x0 + 2, y + ROW - 2, sc);
            g.blit(spell.getSpellIconResource(), x0 + 4, y + 1, 0, 0, 16, 16, 16, 16);
            boolean disabled = !spell.isEnabled();
            Component n = fit(name(spell), x1 - x0 - 36);
            g.drawString(font, n, x0 + 23, y + 5, disabled ? FAINT : sel ? GOLD_LIGHT : TEXT, false);
            if (disabled) {
                g.fill(x0 + 23, y + 9, x0 + 23 + font.width(n), y + 10, 0xA0FF7070);
            }
            if (pending.containsKey(spell.getSpellResource())) {
                diamond(g, x1 - 6, y + 9, 2, PENDING);
            } else if (changed(spell)) {
                diamond(g, x1 - 6, y + 9, 2, CHANGED);
            }
            if (y + ROW > y0 && y < y1) {
                hots.add(new Hot(x0, Math.max(y, y0), x1, Math.min(y + ROW, y1), () -> select(spell),
                        List.of(name(spell).copy().withStyle(s -> s.withColor(sc)), Component.literal(spell.getSpellResource().toString()).withStyle(ChatFormatting.DARK_GRAY))));
            }
        }
        g.disableScissor();
        if (shown.isEmpty()) {
            text(g, tr("no_spell"), x0 + 4, y0 + 6, FAINT, 1);
        }
        // Its scroll bar
        if (maxListScroll() > 0) {
            int bx = x1 + 2, track = y1 - y0;
            int bar = Math.max(12, track * listRows() / shown.size());
            int by = y0 + (int) ((track - bar) * listScroll / maxListScroll());
            g.fill(bx, y0, bx + 2, y1, 0x40FFFFFF);
            g.fill(bx, by, bx + 2, by + bar, GOLD);
        }
    }

    private void select(AbstractSpell spell) {
        commitInput();
        selected = spell;
        lastSelected = spell.getSpellResource();
        contentScroll = 0;
        previewLevel = 1;
        confirming = null;
        click();
    }

    private void drawSpell(GuiGraphics g, int mx, int my, long now) {
        AbstractSpell spell = selected;
        int x0 = panelX0() + 4, x1 = panelX1() - 2, y0 = top + 32;
        int sc = schoolColor(spell.getSchoolType());
        // Its icon, haloed in its school's colour
        float pulse = .5f + .5f * Mth.sin(now / 500f);
        g.fill(x0 - 1, y0 - 1, x0 + 35, y0 + 35, ((int) (60 + 60 * pulse) << 24) | (sc & 0xFFFFFF));
        g.renderOutline(x0 - 2, y0 - 2, 38, 38, GOLD);
        g.pose().pushPose();
        g.pose().translate(x0 + 1, y0 + 1, 0);
        g.pose().scale(2, 2, 1);
        g.blit(spell.getSpellIconResource(), 0, 0, 0, 0, 16, 16, 16, 16);
        g.pose().popPose();
        int tx = x0 + 42;
        text(g, fit(name(spell), (int) ((x1 - tx) / 1.3f)), tx, y0, sc, 1.3f);
        Component school = spell.getSchoolType().getDisplayName();
        MutableComponent line = Component.empty().append(school.copy().withStyle(s -> s.withColor(sc)))
                .append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(modName(spell.getSpellResource().getNamespace())).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY))
                .append(tr("levels", spell.getMinLevel(), spell.getMaxLevel()).copy().withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY))
                .append(tr("cast_" + spell.getCastType().name().toLowerCase(Locale.ROOT)).copy().withStyle(ChatFormatting.GRAY));
        g.drawString(font, fit(line, x1 - tx), tx, y0 + 14, DIM, false);
        Component badge = pending.containsKey(spell.getSpellResource()) ? tr("badge_pending")
                : !spell.isEnabled() ? tr("badge_disabled") : changed(spell) ? tr("badge_changed") : tr("badge_default");
        int bc = pending.containsKey(spell.getSpellResource()) ? PENDING : !spell.isEnabled() ? RED : changed(spell) ? CHANGED : FAINT;
        int bw = font.width(badge) + 8;
        g.fill(tx, y0 + 25, tx + bw, y0 + 36, (bc & 0xFFFFFF) | 0x30000000);
        g.renderOutline(tx, y0 + 25, bw, 11, bc);
        g.drawString(font, badge, tx + 4, y0 + 27, bc, false);
        g.drawString(font, fit(Component.literal(spell.getSpellResource().toString()), x1 - tx - bw - 8), tx + bw + 6, y0 + 27, FAINT, false);
        // The tabs
        int ty = top + 72, tw = (x1 - x0 + 2) / Tab.values().length;
        for (Tab t : Tab.values()) {
            int a = x0 - 2 + t.ordinal() * tw, b = a + tw - 2;
            boolean on = t == tab;
            boolean hover = mx >= a && mx < b && my >= ty && my < ty + 12;
            g.fillGradient(a, ty, b, ty + 12, on ? 0xF0705020 : hover ? 0xC0342868 : 0xC01E1640, on ? 0xF0402810 : 0xC0140E30);
            g.renderOutline(a, ty, b - a, 12, on ? GOLD : 0xFF3E3270);
            int count = t == Tab.ADDONS ? entries.get(Tab.ADDONS).size() : -1;
            Component label = count >= 0 ? tr("tab." + t.name().toLowerCase(Locale.ROOT)).copy().append(" (" + count + ")")
                    : tr("tab." + t.name().toLowerCase(Locale.ROOT));
            Component fl = fit(label, b - a - 4);
            g.drawString(font, fl, (a + b - font.width(fl)) / 2, ty + 2, on ? GOLD_LIGHT : TEXT, false);
            if (tabHasChanges(spell, t)) {
                diamond(g, b - 4, ty + 3, 1, pendingInTab(spell, t) ? PENDING : CHANGED);
            }
            hots.add(new Hot(a, ty, b, ty + 12, () -> {
                commitInput();
                tab = t;
                contentScroll = 0;
                click();
            }, null));
        }
        // The content
        int cy0 = contentY0(), cy1 = contentY1();
        g.enableScissor(x0 - 2, cy0, x1 + 2, cy1);
        if (tab == Tab.PREVIEW) {
            drawPreview(g, mx, my, spell, x0, x1, cy0 - (int) contentScroll);
        } else {
            List<Entry> list = entries.get(tab);
            if (list.isEmpty()) {
                text(g, tr("no_addons"), x0 + 4, cy0 + 6, FAINT, 1);
            }
            int y = cy0 + 2 - (int) contentScroll;
            for (Entry e : list) {
                drawEntry(g, mx, my, spell, e, x0, x1, y, cy0, cy1);
                y += LINE;
            }
        }
        g.disableScissor();
        // Its scroll bar
        double max = maxContentScroll();
        if (max > 0) {
            int bx = x1 + 1, track = cy1 - cy0;
            int bar = Math.max(12, (int) (track * track / (track + max)));
            int by = cy0 + (int) ((track - bar) * contentScroll / max);
            g.fill(bx, cy0, bx + 2, cy1, 0x40FFFFFF);
            g.fill(bx, by, bx + 2, by + bar, GOLD);
        }
        // The status line
        if (Util.getMillis() - statusAt < 8000 && !status.getString().isEmpty()) {
            g.drawString(font, fit(status, x1 - x0), x0, cy1 + 3, statusColor, false);
        }
    }

    private boolean tabHasChanges(AbstractSpell spell, Tab t) {
        for (Entry e : entries.get(t)) {
            if (!same(value(spell, e), def(spell, e)) || isPending(spell, e)) {
                return true;
            }
        }
        return false;
    }

    private boolean pendingInTab(AbstractSpell spell, Tab t) {
        for (Entry e : entries.get(t)) {
            if (isPending(spell, e)) {
                return true;
            }
        }
        return false;
    }

    private double maxContentScroll() {
        int rows = tab == Tab.PREVIEW ? previewHeight() : entries.get(tab).size() * LINE + 4;
        return Math.max(0, rows - (contentY1() - contentY0()));
    }

    private void drawEntry(GuiGraphics g, int mx, int my, AbstractSpell spell, Entry e, int x0, int x1, int y, int cy0, int cy1) {
        Object v = value(spell, e);
        Object d = def(spell, e);
        boolean pend = isPending(spell, e);
        boolean diff = !same(v, d);
        boolean visible = y + LINE > cy0 && y < cy1;
        boolean hover = visible && mx >= x0 - 2 && mx < x1 && my >= Math.max(y, cy0) && my < Math.min(y + LINE - 2, cy1);
        if (hover) {
            g.fill(x0 - 2, y, x1, y + LINE - 2, 0x20FFFFFF);
        }
        if (diff || pend) {
            g.fill(x0 - 2, y + 1, x0, y + LINE - 3, pend ? PENDING : CHANGED);
        }
        // The control's width decides how much room the label gets
        int cx1 = x1 - 16, cx0 = cx1 - 150;
        g.drawString(font, fit(e.label, cx0 - x0 - 4), x0 + 3, y + 5, pend ? PENDING : diff ? CHANGED : TEXT, false);
        List<Component> tip = new ArrayList<>();
        tip.add(e.label.copy().withStyle(ChatFormatting.GOLD));
        if (e.tip != null) {
            tip.add(e.tip);
        }
        tip.add(tr("default_is", fmtAny(d, e)).copy().withStyle(ChatFormatting.DARK_GRAY));
        if (visible) {
            hots.add(new Hot(x0, Math.max(y, cy0), cx0 - 2, Math.min(y + LINE - 2, cy1), null, tip));
        }
        switch (e.kind) {
            case NUMBER -> drawNumber(g, mx, my, spell, e, v, cx0, cx1, y, visible, tip);
            case BOOL -> {
                boolean on = Boolean.TRUE.equals(v);
                int sx = cx0 + 4;
                g.fill(sx, y + 4, sx + 22, y + 14, on ? 0xFF3C8A4C : 0xFF4A2A3A);
                g.renderOutline(sx, y + 4, 22, 10, on ? GREEN : 0xFF8A5A6A);
                int knob = on ? sx + 12 : sx + 1;
                g.fill(knob, y + 5, knob + 9, y + 13, on ? 0xFFE8FFE8 : 0xFFD8C8D0);
                g.drawString(font, on ? tr("yes") : tr("no"), sx + 28, y + 5, on ? GREEN : RED, false);
                if (visible) {
                    hots.add(new Hot(cx0, Math.max(y, cy0), cx1, Math.min(y + LINE - 2, cy1), () -> {
                        set(spell, e, !on);
                        click();
                    }, tip));
                }
            }
            case CHOICE -> {
                int idx = Math.max(0, indexOf(e.choices, v));
                Component name = e.choiceName.apply(v);
                g.fill(cx0, y + 3, cx1, y + 15, 0xC0140E30);
                g.renderOutline(cx0, y + 3, cx1 - cx0, 12, 0xFF4A3C7C);
                g.drawString(font, "◀", cx0 + 3, y + 5, GOLD, false);
                g.drawString(font, "▶", cx1 - 9, y + 5, GOLD, false);
                Component fn = fit(name, cx1 - cx0 - 26);
                int color = v instanceof SchoolType st ? schoolColor(st) : TEXT;
                g.drawString(font, fn, (cx0 + cx1 - font.width(fn)) / 2, y + 5, color, false);
                if (visible && !e.choices.isEmpty()) {
                    int n = e.choices.size();
                    hots.add(new Hot(cx0, Math.max(y, cy0), (cx0 + cx1) / 2, Math.min(y + LINE - 2, cy1), () -> {
                        set(spell, e, e.choices.get((idx - 1 + n) % n));
                        click();
                    }, tip));
                    hots.add(new Hot((cx0 + cx1) / 2, Math.max(y, cy0), cx1, Math.min(y + LINE - 2, cy1), () -> {
                        set(spell, e, e.choices.get((idx + 1) % n));
                        click();
                    }, tip));
                }
            }
            case TEXT -> {
                String id = "value:" + e.id;
                boolean typing = input != null && input.target.equals(id);
                String shownText = typing ? input.text : json(e, v);
                g.fill(cx0, y + 3, cx1, y + 15, typing ? 0xFF1C1440 : 0xC0140E30);
                g.renderOutline(cx0, y + 3, cx1 - cx0, 12, typing ? GOLD : 0xFF4A3C7C);
                while (font.width(shownText) > cx1 - cx0 - 8 && !shownText.isEmpty()) {
                    shownText = typing ? shownText.substring(1) : shownText.substring(0, shownText.length() - 1);
                }
                g.drawString(font, shownText + (typing && (Util.getMillis() / 400) % 2 == 0 ? "_" : ""), cx0 + 4, y + 5, TEXT, false);
                if (visible) {
                    hots.add(new Hot(cx0, Math.max(y, cy0), cx1, Math.min(y + LINE - 2, cy1), () -> input = new Input(id, json(e, value(spell, e))), tip));
                }
            }
        }
        // Back to the default
        if (diff && visible) {
            int rx = x1 - 13;
            boolean rh = mx >= rx && mx < rx + 12 && my >= y + 3 && my < y + 15;
            g.fill(rx, y + 3, rx + 12, y + 15, rh ? 0x80DEB25C : 0x30FFFFFF);
            g.drawString(font, "↺", rx + 3, y + 5, rh ? 0xFF000000 : GOLD_LIGHT, false);
            hots.add(new Hot(rx, y + 3, rx + 12, y + 15, () -> {
                set(spell, e, d);
                click();
            }, List.of(tr("back_to_default", fmtAny(d, e)))));
        }
    }

    private String fmtAny(Object v, Entry e) {
        if (e.kind == Kind.CHOICE) {
            return e.choiceName.apply(v).getString();
        }
        if (e.kind == Kind.BOOL) {
            return (Boolean.TRUE.equals(v) ? tr("yes") : tr("no")).getString();
        }
        if (e.kind == Kind.TEXT) {
            try {
                return json(e, v);
            } catch (RuntimeException ex) {
                return String.valueOf(v);
            }
        }
        return fmt(v, e);
    }

    private static int indexOf(List<Object> list, Object v) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i), v)) {
                return i;
            }
        }
        return -1;
    }

    private void drawNumber(GuiGraphics g, int mx, int my, AbstractSpell spell, Entry e, Object v, int cx0, int cx1, int y, boolean visible,
                            List<Component> tip) {
        int cy0 = contentY0(), cy1 = contentY1();
        String id = "value:" + e.id;
        boolean typing = input != null && input.target.equals(id);
        // [-] [ value ] [+]
        int bx0 = cx0, bx1 = cx0 + 12, vx0 = cx0 + 14, vx1 = cx1 - 14, px0 = cx1 - 12;
        button(g, mx, my, bx0, y + 3, bx1, y + 15, "−");
        button(g, mx, my, px0, y + 3, cx1, y + 15, "+");
        boolean drag = armed == e && dragged;
        g.fill(vx0, y + 3, vx1, y + 15, typing ? 0xFF1C1440 : drag ? 0xFF2A2050 : 0xC0140E30);
        g.renderOutline(vx0, y + 3, vx1 - vx0, 12, typing || drag ? GOLD : 0xFF4A3C7C);
        // A gauge of where the value sits in a sensible range
        double val = ((Number) v).doubleValue();
        double span = gaugeMax(spell, e);
        if (span > 0 && !typing) {
            int fillTo = vx0 + 1 + (int) ((vx1 - vx0 - 2) * Mth.clamp(val / span, 0, 1));
            g.fill(vx0 + 1, y + 13, fillTo, y + 14, 0x90DEB25C);
        }
        String shownText = typing ? input.text + ((Util.getMillis() / 400) % 2 == 0 ? "_" : "") : fmt(v, e);
        if (e.stat == Stat.CAST_TIME && !typing) {
            shownText += String.format(Locale.ROOT, " (%.2f s)", val / 20);
        }
        g.drawString(font, shownText, (vx0 + vx1 - font.width(shownText)) / 2, y + 5, typing ? GOLD_LIGHT : TEXT, false);
        if (visible) {
            int hy0 = Math.max(y + 3, cy0), hy1 = Math.min(y + 15, cy1);
            hots.add(new Hot(bx0, hy0, bx1, hy1, () -> step(spell, e, -1), tip));
            hots.add(new Hot(px0, hy0, cx1, hy1, () -> step(spell, e, 1), tip));
            List<Component> vtip = new ArrayList<>(tip);
            vtip.add(tr("number_help").copy().withStyle(ChatFormatting.DARK_AQUA));
            hots.add(new Hot(vx0, hy0, vx1, hy1, () -> {
                armed = e;
                armValue = ((Number) value(spell, e)).doubleValue();
                dragged = false;
            }, vtip));
        }
    }

    /** The top of the little gauge under a number. */
    private double gaugeMax(AbstractSpell spell, Entry e) {
        double d = ((Number) def(spell, e)).doubleValue();
        if (e.stat == Stat.LIFESTEAL) {
            return 100;
        }
        if (e.unit.equals("×")) {
            return 3;
        }
        return d > 0 ? d * 3 : Math.min(e.max, 20);
    }

    private void button(GuiGraphics g, int mx, int my, int x0, int y0, int x1, int y1, String label) {
        boolean hover = mx >= x0 && mx < x1 && my >= y0 && my < y1;
        g.fill(x0, y0, x1, y1, hover ? 0xC0604014 : 0xC0201848);
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, hover ? GOLD : 0xFF4A3C7C);
        g.drawString(font, label, (x0 + x1 - font.width(label)) / 2 + 1, y0 + 2, hover ? GOLD_LIGHT : TEXT, false);
    }

    /** One step up or down (shift: ten; control: a tenth, for decimals). */
    private void step(AbstractSpell spell, Entry e, int dir) {
        double s = e.step * (Screen.hasShiftDown() ? 10 : 1) * (Screen.hasControlDown() && !e.integer ? .1 : 1);
        set(spell, e, ((Number) value(spell, e)).doubleValue() + dir * s);
        click();
    }

    // ---------------------------------------------------------------- the preview

    private int previewHeight() {
        return 22 + Math.min(selected == null ? 1 : selected.getMaxLevel(), 10) * 12 + 30 + 12 * 8;
    }

    /** The spell level by level, with the changes not yet saved; then its own lines (saved values) at a chosen level. */
    private void drawPreview(GuiGraphics g, int mx, int my, AbstractSpell spell, int x0, int x1, int y) {
        double powerMult = num(spell, "iron:irons_spellbooks:power_multiplier", 1);
        double manaMult = num(spell, "iron:irons_spellbooks:mana_cost_multiplier", 1);
        double cooldown = num(spell, "iron:irons_spellbooks:cooldown_in_seconds", spell.getSpellCooldown() / 20.0);
        int maxLevel = (int) num(spell, "iron:irons_spellbooks:max_level", spell.getMaxLevel());
        double bm = num(spell, "tune:" + Stat.BASE_MANA.key, 0), mpl = num(spell, "tune:" + Stat.MANA_PER_LEVEL.key, 0);
        double bp = num(spell, "tune:" + Stat.BASE_POWER.key, 0), ppl = num(spell, "tune:" + Stat.POWER_PER_LEVEL.key, 0);
        double cast = num(spell, "tune:" + Stat.CAST_TIME.key, spell.getCastTime(1));
        double dmg = num(spell, "tune:" + Stat.DAMAGE_MULT.key, 1);
        int[] cols = {x0 + 4, x0 + 40, x0 + 100, x0 + 170, x1 - 60};
        String[] heads = {"level", "mana", "power", "cooldown", "cast"};
        g.fill(x0, y, x1, y + 13, 0x60402870);
        for (int i = 0; i < cols.length; i++) {
            g.drawString(font, tr("col." + heads[i]), cols[i], y + 3, GOLD_LIGHT, false);
        }
        y += 15;
        int shownLevels = Math.min(maxLevel, 10);
        for (int level = 1; level <= shownLevels; level++) {
            boolean sel = level == previewLevel;
            if (sel) {
                g.fill(x0, y - 1, x1, y + 10, 0x40DEB25C);
            } else if (level % 2 == 0) {
                g.fill(x0, y - 1, x1, y + 10, 0x18FFFFFF);
            }
            int mana = (int) ((bm + mpl * (level - 1)) * manaMult);
            double power = (bp + ppl * (level - 1)) * powerMult;
            g.drawString(font, String.valueOf(level), cols[0] + 4, y, sel ? GOLD_LIGHT : TEXT, false);
            g.drawString(font, String.valueOf(mana), cols[1], y, 0xFF7FB8FF, false);
            String p = String.format(Locale.ROOT, "%.1f", power) + (Math.abs(dmg - 1) > 1e-6 ? String.format(Locale.ROOT, "  (×%.2f)", dmg) : "");
            g.drawString(font, p, cols[2], y, 0xFFFF9F6B, false);
            g.drawString(font, String.format(Locale.ROOT, "%.1f s", cooldown), cols[3], y, TEXT, false);
            Map<String, Object> waiting = pending.get(spell.getSpellResource());
            boolean castSet = waiting != null && waiting.containsKey("tune:" + Stat.CAST_TIME.key);
            double shownCast = spell.getCastType() == CastType.INSTANT && !castSet ? com.waylays.arcanes.spells.CastTimeFloor.instantCharge(spell) : cast;
            g.drawString(font, shownCast <= 0 ? "—" : String.format(Locale.ROOT, "%.2f s", shownCast / 20), cols[4], y, TEXT, false);
            final int lv = level;
            hots.add(new Hot(x0, y - 1, x1, y + 10, () -> {
                previewLevel = lv;
                click();
            }, List.of(tr("preview_pick"))));
            y += 12;
        }
        if (maxLevel > shownLevels) {
            g.drawString(font, tr("more_levels", maxLevel - shownLevels), cols[0], y, FAINT, false);
            y += 12;
        }
        y += 6;
        g.fill(x0, y, x1, y + 1, 0x60DEB25C);
        y += 5;
        g.drawString(font, tr("info_at", previewLevel), x0 + 2, y, GOLD_LIGHT, false);
        y += 13;
        try {
            var player = Minecraft.getInstance().player;
            for (MutableComponent c : spell.getUniqueInfo(previewLevel, player)) {
                g.drawString(font, fit(c, x1 - x0 - 8), x0 + 6, y, TEXT, false);
                y += 11;
            }
        } catch (RuntimeException ex) {
            g.drawString(font, tr("info_unavailable"), x0 + 6, y, FAINT, false);
        }
    }

    private double num(AbstractSpell spell, String id, double fallback) {
        for (List<Entry> list : entries.values()) {
            for (Entry e : list) {
                if (e.id.equals(id)) {
                    Object v = value(spell, e);
                    return v instanceof Number n ? n.doubleValue() : fallback;
                }
            }
        }
        return fallback;
    }

    // ---------------------------------------------------------------- the bottom buttons

    private void drawButtons(GuiGraphics g, int mx, int my) {
        int y = top + h - 32, x1 = panelX1() - 2;
        int x = panelX0() + 2;
        int n = pendingCount();
        x = bigButton(g, mx, my, x, y, tr("save", n), n > 0, GOLD, this::save, List.of(tr("save.tip")));
        x = bigButton(g, mx, my, x + 4, y, tr("discard"), n > 0, 0xFF9A8FB8, () -> discard(Screen.hasShiftDown()), List.of(tr("discard.tip")));
        x = bigButton(g, mx, my, x + 4, y, tr("defaults"), true, 0xFFFF9F6B, () -> {
            if (Screen.hasShiftDown()) {
                resetList();
            } else {
                resetSpell();
            }
        }, List.of(tr("defaults.tip", shown.size())));
        boolean can = selected != null && pending.containsKey(selected.getSpellResource());
        bigButton(g, mx, my, x + 4, y, "apply".equals(confirming) ? tr("apply_sure", shown.size()) : tr("apply", shown.size()), can, PENDING,
                this::applyToList, List.of(tr("apply.tip")));
    }

    private int bigButton(GuiGraphics g, int mx, int my, int x, int y, Component label, boolean active, int color, Runnable action,
                          List<Component> tip) {
        int bw = font.width(label) + 14;
        bw = Math.min(bw, panelX1() - x - 2);
        boolean hover = active && mx >= x && mx < x + bw && my >= y && my < y + 18;
        int base = color & 0xFFFFFF;
        g.fillGradient(x, y, x + bw, y + 18, (hover ? 0xD0000000 : active ? 0x90000000 : 0x40000000) | base,
                (active ? 0x60000000 : 0x20000000) | (base >> 2 & 0x3F3F3F));
        g.renderOutline(x, y, bw, 18, active ? color : 0xFF3E3270);
        Component t = fit(label, bw - 6);
        g.drawString(font, t, x + (bw - font.width(t)) / 2, y + 5, active ? (hover ? 0xFFFFFFFF : 0xFFF8F0E0) : FAINT, false);
        hots.add(new Hot(x, y, x + bw, y + 18, active ? action : null, tip));
        return x + bw;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = hots.size() - 1; i >= 0; i--) {
                Hot hot = hots.get(i);
                if (hot.in(mx, my) && hot.click != null) {
                    // What was being typed is taken first
                    commitInput();
                    armX = mx;
                    hot.click.run();
                    return true;
                }
            }
            commitInput();
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (armed != null && selected != null) {
            if (Math.abs(mx - armX) > 2 || dragged) {
                if (!dragged) {
                    dragged = true;
                    armX = mx;
                }
                double steps = Math.round((mx - armX) / 3);
                double s = armed.step * (Screen.hasShiftDown() ? 10 : 1) * (Screen.hasControlDown() && !armed.integer ? .1 : 1);
                set(selected, armed, armValue + steps * s);
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (armed != null) {
            Entry e = armed;
            boolean wasDrag = dragged;
            armed = null;
            dragged = false;
            if (!wasDrag && selected != null) {
                input = new Input("value:" + e.id, fmt(value(selected, e), e).replace(e.unit, "").trim());
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (mx >= listX0() && mx < left + LIST_W + 10 && my >= listY0() && my < listY1()) {
            listScroll = Mth.clamp(listScroll - scrollY * 2, 0, maxListScroll());
            return true;
        }
        if (selected != null && mx >= panelX0() && mx < panelX1() && my >= contentY0() && my < contentY1()) {
            // Over a number: change it; elsewhere: scroll
            if (tab != Tab.PREVIEW && Screen.hasAltDown()) {
                int row = (int) ((my - contentY0() - 2 + contentScroll) / LINE);
                List<Entry> list = entries.get(tab);
                if (row >= 0 && row < list.size() && list.get(row).kind == Kind.NUMBER) {
                    step(selected, list.get(row), scrollY > 0 ? 1 : -1);
                    return true;
                }
            }
            contentScroll = Mth.clamp(contentScroll - scrollY * 12, 0, maxContentScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (input != null) {
            if (input.target.equals("search")) {
                if (c >= ' ' && input.text.length() < 60) {
                    input.text += c;
                    search = input.text;
                    filter();
                }
            } else if (input.text.length() < 200 && (isTextEntry() ? c >= ' ' : "0123456789.,-+eE".indexOf(c) >= 0)) {
                input.text += c;
            }
            return true;
        }
        return super.charTyped(c, mods);
    }

    private boolean isTextEntry() {
        Entry e = inputEntry();
        return e != null && e.kind == Kind.TEXT;
    }

    @Nullable
    private Entry inputEntry() {
        if (input == null || !input.target.startsWith("value:")) {
            return null;
        }
        String id = input.target.substring("value:".length());
        for (List<Entry> list : entries.values()) {
            for (Entry e : list) {
                if (e.id.equals(id)) {
                    return e;
                }
            }
        }
        return null;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_S && Screen.hasControlDown()) {
            save();
            return true;
        }
        if (input != null) {
            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (!input.text.isEmpty()) {
                        input.text = Screen.hasControlDown() ? "" : input.text.substring(0, input.text.length() - 1);
                        if (input.target.equals("search")) {
                            search = input.text;
                            filter();
                        }
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_TAB -> {
                    commitInput();
                    return true;
                }
                case GLFW.GLFW_KEY_ESCAPE -> {
                    if (input.target.equals("search")) {
                        search = "";
                        filter();
                    }
                    input = null;
                    return true;
                }
                default -> {
                    if (key == GLFW.GLFW_KEY_V && Screen.hasControlDown()) {
                        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                        for (char c : clip.toCharArray()) {
                            charTyped(c, 0);
                        }
                        return true;
                    }
                    return true;
                }
            }
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && !pending.isEmpty() && !escapeWarned) {
            escapeWarned = true;
            setStatus(tr("unsaved_warning", pending.size()), CHANGED);
            return true;
        }
        if (key == GLFW.GLFW_KEY_F && Screen.hasControlDown()) {
            input = new Input("search", search);
            return true;
        }
        if ((key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) && !shown.isEmpty()) {
            int i = selected == null ? -1 : shown.indexOf(selected);
            i = Mth.clamp(i + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, shown.size() - 1);
            select(shown.get(i));
            if (i < listScroll) {
                listScroll = i;
            } else if (i >= listScroll + listRows()) {
                listScroll = i - listRows() + 1;
            }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    /** Takes what was typed. */
    private void commitInput() {
        Input in = input;
        input = null;
        if (in == null) {
            return;
        }
        if (in.target.equals("search")) {
            search = in.text;
            filter();
            return;
        }
        Entry e = entryById(in.target.substring("value:".length()));
        if (e == null || selected == null) {
            return;
        }
        if (e.kind == Kind.TEXT) {
            Object v = parseJson(e, in.text);
            if (v == null) {
                setStatus(tr("bad_value", e.label), RED);
            } else {
                set(selected, e, v);
            }
            return;
        }
        try {
            String t = in.text.replace(',', '.').trim();
            if (!t.isEmpty()) {
                set(selected, e, Double.parseDouble(t));
            }
        } catch (NumberFormatException ex) {
            setStatus(tr("bad_value", e.label), RED);
        }
    }

    @Nullable
    private Entry entryById(String id) {
        for (List<Entry> list : entries.values()) {
            for (Entry e : list) {
                if (e.id.equals(id)) {
                    return e;
                }
            }
        }
        return null;
    }

    @Override
    public void removed() {
        lastSearch = search;
        lastMod = modFilter;
        lastSchool = schoolFilter;
        lastOnlyChanged = onlyChanged;
        lastSort = sort;
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** For the client test: the spells it lists, and how many have changes waiting. */
    public int spellCount() {
        return spells.size();
    }

    public Map<ResourceLocation, Map<String, Object>> pendingForTest() {
        return new HashMap<>(pending);
    }
}
