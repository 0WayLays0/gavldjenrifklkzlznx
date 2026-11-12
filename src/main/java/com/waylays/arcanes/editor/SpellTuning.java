package com.waylays.arcanes.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The spell editor's own stats, set per spell on top of Iron's spell config (which keeps the school, rarity, levels,
 * cooldown, power and mana multipliers and whatever other mods add): a spell's base mana and power and how they grow
 * with each level, its cast time (written into the spell itself, on the server and on every client), and what its
 * damage does (a multiplier, fire, freezing, life stolen) and extra levels for whoever casts it (through Iron's
 * events). Kept in {@code config/arcanes_oublies/spell_tuning.json}, for the whole server.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class SpellTuning {
    /** A stat of the editor's own: its key in the file, its range and step, and the spell field it is written to, if any. */
    public enum Stat {
        BASE_MANA("base_mana", "baseManaCost", 0, 10000, 1, true),
        MANA_PER_LEVEL("mana_per_level", "manaCostPerLevel", 0, 2000, 1, true),
        BASE_POWER("base_power", "baseSpellPower", 0, 2000, 1, true),
        POWER_PER_LEVEL("power_per_level", "spellPowerPerLevel", 0, 1000, 1, true),
        CAST_TIME("cast_time", "castTime", 0, 6000, 1, true),
        DAMAGE_MULT("damage_mult", null, 0, 100, .05, false),
        BONUS_LEVELS("bonus_levels", null, -10, 20, 1, true),
        FIRE_SECONDS("fire_seconds", null, 0, 120, .5, false),
        FREEZE_SECONDS("freeze_seconds", null, 0, 60, .5, false),
        LIFESTEAL("lifesteal", null, 0, 200, 1, false);

        public final String key;
        @Nullable
        final String field;
        public final double min, max, step;
        public final boolean integer;

        Stat(String key, @Nullable String field, double min, double max, double step, boolean integer) {
            this.key = key;
            this.field = field;
            this.min = min;
            this.max = max;
            this.step = step;
            this.integer = integer;
        }

        /** Whether the spell itself carries it (as opposed to an effect added when it is cast or does damage). */
        public boolean inSpell() {
            return field != null;
        }

        /** What it is when nothing changes it (for those not in the spell). */
        public double neutral() {
            return this == DAMAGE_MULT ? 1 : 0;
        }

        @Nullable
        public static Stat byKey(String key) {
            for (Stat s : values()) {
                if (s.key.equals(key)) {
                    return s;
                }
            }
            return null;
        }

        public double clamp(double v) {
            v = Math.max(min, Math.min(max, v));
            return integer ? Math.round(v) : v;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, Field> FIELDS = new HashMap<>();

    /** The values set, per spell (only the stats set). */
    private static volatile Map<ResourceLocation, Map<Stat, Double>> tuning = Map.of();
    /** Each spell's own values of the fields written to, taken before they were first changed. */
    private static final Map<ResourceLocation, EnumMap<Stat, Integer>> originals = new HashMap<>();

    private SpellTuning() {}

    // ---------------------------------------------------------------- reading

    /** Everything set, per spell. */
    public static Map<ResourceLocation, Map<Stat, Double>> all() {
        return tuning;
    }

    /** The value set for {@code stat} of {@code spell}, or null if it is left as it is. */
    @Nullable
    public static Double get(AbstractSpell spell, Stat stat) {
        Map<Stat, Double> m = tuning.get(spell.getSpellResource());
        return m == null ? null : m.get(stat);
    }

    /** {@code stat} of {@code spell} as it is now (set or not). */
    public static double current(AbstractSpell spell, Stat stat) {
        Double v = get(spell, stat);
        return v != null ? v : original(spell, stat);
    }

    /** {@code stat} of {@code spell} as its mod made it (for those not in the spell: no change). */
    public static double original(AbstractSpell spell, Stat stat) {
        if (!stat.inSpell()) {
            return stat.neutral();
        }
        EnumMap<Stat, Integer> o = originals.get(spell.getSpellResource());
        if (o != null && o.containsKey(stat)) {
            return o.get(stat);
        }
        Integer v = readField(spell, stat);
        return v == null ? 0 : v;
    }

    /** Whether the spell's own fields can be written (they could not be found, say in a future Iron's version). */
    public static boolean supports(Stat stat) {
        return !stat.inSpell() || field(stat.field) != null;
    }

    // ---------------------------------------------------------------- applying

    /** Takes {@code next} as everything set: the spells' fields are written (or given back their own values). */
    public static synchronized void replace(Map<ResourceLocation, Map<Stat, Double>> next) {
        Set<ResourceLocation> touched = new HashSet<>(tuning.keySet());
        touched.addAll(next.keySet());
        Map<ResourceLocation, Map<Stat, Double>> copy = new HashMap<>();
        next.forEach((id, m) -> {
            if (!m.isEmpty()) {
                copy.put(id, Collections.unmodifiableMap(new EnumMap<>(m)));
            }
        });
        tuning = Collections.unmodifiableMap(copy);
        for (ResourceLocation id : touched) {
            AbstractSpell spell = SpellRegistry.getSpell(id);
            if (spell == null || spell == SpellRegistry.none()) {
                continue;
            }
            for (Stat stat : Stat.values()) {
                if (stat.inSpell()) {
                    Double v = get(spell, stat);
                    writeField(spell, stat, v == null ? (int) original(spell, stat) : (int) Math.round(v));
                }
            }
        }
    }

    @Nullable
    private static Field field(String name) {
        synchronized (FIELDS) {
            if (!FIELDS.containsKey(name)) {
                Field f = null;
                try {
                    f = AbstractSpell.class.getDeclaredField(name);
                    f.setAccessible(true);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    ArcanesOublies.LOGGER.warn("Spell editor: Iron's spells have no field {} ({}), that stat cannot be edited", name, e.toString());
                }
                FIELDS.put(name, f);
            }
            return FIELDS.get(name);
        }
    }

    @Nullable
    private static Integer readField(AbstractSpell spell, Stat stat) {
        Field f = field(stat.field);
        if (f == null) {
            return null;
        }
        try {
            return f.getInt(spell);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static void writeField(AbstractSpell spell, Stat stat, int value) {
        Field f = field(stat.field);
        if (f == null) {
            return;
        }
        try {
            EnumMap<Stat, Integer> o = originals.computeIfAbsent(spell.getSpellResource(), k -> new EnumMap<>(Stat.class));
            if (!o.containsKey(stat)) {
                o.put(stat, f.getInt(spell));
            }
            f.setInt(spell, value);
        } catch (IllegalAccessException e) {
            ArcanesOublies.LOGGER.warn("Spell editor: could not write {} of {}", stat.key, spell.getSpellId());
        }
    }

    // ---------------------------------------------------------------- the file (server)

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(ArcanesOublies.MODID).resolve("spell_tuning.json");
    }

    /** Reads the file and applies it. */
    public static void load() {
        Map<ResourceLocation, Map<Stat, Double>> read = new HashMap<>();
        Path path = file();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    read = fromJson(root);
                }
            } catch (IOException | RuntimeException e) {
                ArcanesOublies.LOGGER.error("Spell editor: could not read {}: {}", path, e.toString());
            }
        }
        replace(read);
        if (!read.isEmpty()) {
            ArcanesOublies.LOGGER.info("Spell editor: {} spells tuned", read.size());
        }
    }

    public static void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(toJson(tuning), writer);
            }
        } catch (IOException e) {
            ArcanesOublies.LOGGER.error("Spell editor: could not write {}: {}", path, e.toString());
        }
    }

    static JsonObject toJson(Map<ResourceLocation, Map<Stat, Double>> data) {
        JsonObject root = new JsonObject();
        root.addProperty("_comment", "Arcanes Oubliés spell editor (/arcanes sorts). Per spell: " + String.join(", ",
                java.util.Arrays.stream(Stat.values()).map(s -> s.key).toList()) + ".");
        data.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            JsonObject o = new JsonObject();
            e.getValue().forEach((stat, v) -> {
                if (stat.integer) {
                    o.addProperty(stat.key, (long) Math.round(v));
                } else {
                    o.addProperty(stat.key, v);
                }
            });
            root.add(e.getKey().toString(), o);
        });
        return root;
    }

    static Map<ResourceLocation, Map<Stat, Double>> fromJson(JsonObject root) {
        Map<ResourceLocation, Map<Stat, Double>> read = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(e.getKey());
            if (id == null || !e.getValue().isJsonObject()) {
                continue;
            }
            EnumMap<Stat, Double> m = new EnumMap<>(Stat.class);
            for (Map.Entry<String, JsonElement> s : e.getValue().getAsJsonObject().entrySet()) {
                Stat stat = Stat.byKey(s.getKey().toLowerCase(Locale.ROOT));
                if (stat != null && s.getValue().isJsonPrimitive() && s.getValue().getAsJsonPrimitive().isNumber()) {
                    m.put(stat, stat.clamp(s.getValue().getAsDouble()));
                }
            }
            if (!m.isEmpty()) {
                read.put(id, m);
            }
        }
        return read;
    }

    // ---------------------------------------------------------------- the effects

    /** Damage, fire, freezing and life stolen, for the spells set so. */
    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        SpellDamageSource source = event.getSpellDamageSource();
        AbstractSpell spell = source.spell();
        if (spell == null) {
            return;
        }
        Map<Stat, Double> m = tuning.get(spell.getSpellResource());
        if (m == null) {
            return;
        }
        Double mult = m.get(Stat.DAMAGE_MULT);
        if (mult != null) {
            event.setAmount((float) (event.getAmount() * mult));
        }
        Double fire = m.get(Stat.FIRE_SECONDS);
        if (fire != null && fire > 0) {
            source.setFireTicks(Math.max(source.getFireTime(), (int) Math.round(fire * 20)));
        }
        Double freeze = m.get(Stat.FREEZE_SECONDS);
        if (freeze != null && freeze > 0) {
            LivingEntity target = event.getEntity();
            // Fully frozen for that long: frozen past the threshold, thawing two ticks a tick (Iron's adds twice what is set)
            int ticks = (target.getTicksRequiredToFreeze() + (int) Math.round(freeze * 40)) / 2;
            source.setFreezeTicks(Math.max(source.getFreezeTicks(), ticks));
        }
        Double steal = m.get(Stat.LIFESTEAL);
        if (steal != null && steal > 0) {
            source.setLifestealPercent(source.getLifestealPercent() + (float) (steal / 100));
        }
    }

    /** Extra (or fewer) levels for whoever casts the spell. */
    @SubscribeEvent
    public static void onSpellLevel(ModifySpellLevelEvent event) {
        Map<Stat, Double> m = tuning.get(event.getSpell().getSpellResource());
        if (m != null) {
            Double bonus = m.get(Stat.BONUS_LEVELS);
            if (bonus != null && bonus != 0) {
                event.setLevel(Math.max(1, event.getLevel() + (int) Math.round(bonus)));
            }
        }
    }
}
