package com.waylays.arcanes.metier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.JsonOps;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The trades' definitions (mining, gathering, crafting, forging, distilling): what block starts which minigame, with
 * which tools and how hard each makes it, what each outcome gives, what a worked-out deposit turns into and for how
 * long, and the stations' recipes. Kept in {@code config/arcanes_oublies/metiers.json} and edited in game
 * ({@code /arcanes metiers}). Plain objects so that Gson writes and reads them as they are.
 */
public final class MetierDefs {
    /** The minigame a trade is played with. */
    public enum Game {
        /** Strike the rock's weak points as their rings close. */
        MINING,
        /** Pull a plant out with its roots, the tension kept in the right band. */
        HARVEST,
        /** Fit the plan's pieces, the right one the right way round. */
        ASSEMBLY,
        /** Keep the metal at forging heat with the bellows, strike in rhythm, quench at the right colour. */
        FORGE,
        /** Heat the still, add each ingredient at its colour, keep it steady while the drops condense. */
        DISTILL,
        /** Press a blade on the grindstone, the pressure kept in a wandering band, without blueing it (see {@code Grindstone}). */
        GRIND,
        /** Saw a plank along its pencil line: stroke after stroke, the blade kept on the line. */
        CARPENTRY,
        /** Stitch a wound: each stitch where and when its ring closes, before the bleeding or the pain is too much. */
        MEDICINE
    }

    /** How an attempt ended. */
    public enum Outcome {CRITICAL, PARTIAL, PERFECT}

    public static final class Tool {
        public String item = "minecraft:iron_pickaxe";
        /** 1 = normal; above, harder (1.5: half again as hard); below, easier. */
        public double difficulty = 1;

        public Tool() {}

        public Tool(String item, double difficulty) {
            this.item = item;
            this.difficulty = difficulty;
        }
    }

    /** Items given: {@code min} to {@code max} of {@code item}, with this {@code chance}; {@code components}: its data. */
    public static final class Reward {
        public String item = "minecraft:raw_copper";
        public int min = 1, max = 1;
        public double chance = 1;
        @Nullable
        public JsonObject components;

        public Reward() {}

        public Reward(String item, int min, int max) {
            this.item = item;
            this.min = min;
            this.max = max;
        }
    }

    public static final class Ingredient {
        public String item = "minecraft:iron_ingot";
        public int count = 1;

        public Ingredient() {}

        public Ingredient(String item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    /** A station's recipe: what it takes, how hard it is, what each outcome gives. */
    public static final class Recipe {
        public String name = "Recette";
        public List<Ingredient> inputs = new ArrayList<>();
        public List<Reward> partial = new ArrayList<>();
        public List<Reward> perfect = new ArrayList<>();
        public List<Reward> critical = new ArrayList<>();
        public double difficulty = 1;
        /** Whether a critical failure uses up the ingredients (a partial or perfect result always does). */
        public boolean loseInputsOnFail = true;
        /** Seconds before the same player may make it again (0: none). */
        public int cooldown = 0;
        /** The station's folder it is listed in ("": at the top, outside any folder). */
        public String folder = "";
        /** The minigame it is played with (null: the station's own); so that each recipe has the one that fits it. */
        @Nullable
        public Game game;
    }

    /** A trade. */
    public static final class Def {
        public String id = "metier";
        public String name = "Métier";
        public boolean enabled = true;
        public Game game = Game.MINING;
        /** The blocks that start it (block ids). */
        public List<String> blocks = new ArrayList<>();
        /** The tools it can be done with (none: bare hands or anything). */
        public List<Tool> tools = new ArrayList<>();
        public double difficulty = 1;
        /** Durability a tool loses for an attempt, and for a critical failure. */
        public int durability = 1, durabilityCritical = 5;
        /** The score (0 to 1) from which it is perfect; below {@code criticalBelow} it is a critical failure. */
        public double perfectAt = .8, criticalBelow = .25;
        // Deposits (no recipes)
        public List<Reward> partial = new ArrayList<>();
        public List<Reward> perfect = new ArrayList<>();
        public List<Reward> critical = new ArrayList<>();
        /** What the block turns into once worked out ("" or "minecraft:air": it disappears). */
        public String depletedBlock = "minecraft:cobblestone";
        /** Seconds before it comes back. */
        public int respawnSeconds = 300;
        /** Attempts before it is worked out. */
        public int uses = 1;
        /** Whether a critical failure also works it out. */
        public boolean depleteOnCritical = true;
        /** Whether players in survival may not simply break it (nor the worked-out block). */
        public boolean protectBlocks = true;
        // Stations (with recipes)
        public List<Recipe> recipes = new ArrayList<>();
        /** The station's folders, in the order they are listed (recipes name theirs, see {@link Recipe#folder}). */
        public List<String> folders = new ArrayList<>();
        /** Seconds between two attempts by the same player (0: none). */
        public int playerCooldown = 0;

        public boolean isStation() {
            return !recipes.isEmpty();
        }

        /** A new folder (a name not taken yet, from {@code base}); returns its name. */
        public String newFolder(String base) {
            String name = folderName(base);
            if (name.isEmpty()) {
                name = "Dossier";
            }
            String unique = name;
            for (int i = 2; folders.contains(unique); i++) {
                unique = name + " " + i;
            }
            folders.add(unique);
            return unique;
        }

        /** Renames a folder, its recipes with it; false if the new name is empty or taken. */
        public boolean renameFolder(String from, String to) {
            String name = folderName(to);
            int at = folders.indexOf(from);
            if (at < 0 || name.isEmpty() || folders.contains(name)) {
                return false;
            }
            folders.set(at, name);
            recipes.forEach(r -> {
                if (r.folder.equals(from)) {
                    r.folder = name;
                }
            });
            return true;
        }

        /** Deletes a folder: its recipes are left outside any folder. */
        public void deleteFolder(String name) {
            folders.remove(name);
            recipes.forEach(r -> {
                if (r.folder.equals(name)) {
                    r.folder = "";
                }
            });
        }

        /** Moves a folder up ({@code by} -1) or down (1) the list. */
        public void moveFolder(String name, int by) {
            int at = folders.indexOf(name), to = at + by;
            if (at >= 0 && to >= 0 && to < folders.size()) {
                folders.add(to, folders.remove(at));
            }
        }

        /** Moves a recipe before the previous (or after the next) recipe of its own folder; returns its new index. */
        public int moveRecipe(int index, int by) {
            if (index < 0 || index >= recipes.size()) {
                return index;
            }
            String folder = recipes.get(index).folder;
            int to = index + by;
            while (to >= 0 && to < recipes.size() && !recipes.get(to).folder.equals(folder)) {
                to += by;
            }
            if (to < 0 || to >= recipes.size()) {
                return index;
            }
            Recipe r = recipes.get(index);
            recipes.set(index, recipes.get(to));
            recipes.set(to, r);
            return to;
        }

        @Nullable
        public Tool tool(ItemStack stack) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            for (Tool t : tools) {
                if (id.toString().equals(t.item)) {
                    return t;
                }
            }
            return null;
        }

        public boolean matches(BlockState state) {
            String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            return blocks.contains(id);
        }
    }

    /** The version of the file the mod writes: trades it brought in since are added to an older file (see {@link #load}). */
    public static final int VERSION = 3;

    /** The whole file. */
    public static final class Config {
        public int version = VERSION;
        public List<Def> metiers = new ArrayList<>();
        /** The kinds of item (item ids) kept from the grindstone's repair (see {@code Grindstone}). */
        public List<String> grindstoneBanned = new ArrayList<>();
        /**
         * Single items kept from the grindstone's repair: an item made as it is (its stats, its look), not its whole
         * kind; a dagger of 8 damage is kept from it, other daggers are not (see {@code Grindstone#signature}).
         */
        public List<GrindItem> grindstoneExact = new ArrayList<>();
    }

    /** An item kept from the grindstone as it is made (see {@link Config#grindstoneExact}). */
    public static final class GrindItem {
        /** Its kind (an item id) and how it is made: its components but those wear and use change (as SNBT). */
        public String item = "";
        public String components = "{}";
        /** For the editor: its name, and its attack damage and speed, or its armour and toughness (-1: it has none). */
        public String name = "";
        public double damage = -1, speed = -1, armor = -1, toughness = -1;

        /** Whether {@code other} is the same item made the same way. */
        public boolean same(GrindItem other) {
            return item.equals(other.item) && components.equals(other.components);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile Config config = defaults();

    private MetierDefs() {}

    public static Config get() {
        return config;
    }

    public static List<Def> all() {
        return config.metiers;
    }

    @Nullable
    public static Def byId(String id) {
        for (Def d : config.metiers) {
            if (d.id.equals(id)) {
                return d;
            }
        }
        return null;
    }

    /** The enabled trade started by {@code state}, if any (the first one listed wins). */
    @Nullable
    public static Def forBlock(BlockState state) {
        for (Def d : config.metiers) {
            if (d.enabled && d.matches(state)) {
                return d;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- the file

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(ArcanesOublies.MODID).resolve("metiers.json");
    }

    public static void load() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            config = defaults();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Config read = GSON.fromJson(reader, Config.class);
            config = sanitize(read == null ? new Config() : read);
            if (config.version < VERSION) {
                upgrade(config);
                save();
            }
        } catch (IOException | RuntimeException e) {
            ArcanesOublies.LOGGER.error("Trades: could not read {} ({}); the defaults are used", path, e.toString());
            config = defaults();
        }
    }

    public static void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            ArcanesOublies.LOGGER.error("Trades: could not write {}: {}", path, e.toString());
        }
    }

    public static String toJson(Config c) {
        return GSON.toJson(c);
    }

    public static String toJson(GrindItem g) {
        return GSON.toJson(g);
    }

    @Nullable
    public static GrindItem grindItemFromJson(String json) {
        try {
            GrindItem g = GSON.fromJson(json, GrindItem.class);
            return g == null || g.item == null || g.item.isBlank() ? null : g;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Config fromJson(String json) {
        return sanitize(GSON.fromJson(json, Config.class));
    }

    /** Takes {@code next} as the trades (from the editor). */
    public static void replace(Config next) {
        config = sanitize(next);
        save();
    }

    /** No nulls, sane numbers, unique ids. */
    static Config sanitize(Config c) {
        if (c.metiers == null) {
            c.metiers = new ArrayList<>();
        }
        if (c.grindstoneBanned == null) {
            c.grindstoneBanned = new ArrayList<>();
        }
        List<String> kept = new ArrayList<>();
        for (String id : c.grindstoneBanned) {
            if (id != null && !id.isBlank() && !kept.contains(id.strip())) {
                kept.add(id.strip());
            }
        }
        c.grindstoneBanned = kept;
        if (c.grindstoneExact == null) {
            c.grindstoneExact = new ArrayList<>();
        }
        c.grindstoneExact.removeIf(g -> g == null || g.item == null || g.item.isBlank());
        for (GrindItem g : c.grindstoneExact) {
            if (g.components == null || g.components.isBlank()) g.components = "{}";
            if (g.name == null) g.name = "";
        }
        List<String> seen = new ArrayList<>();
        for (Def d : c.metiers) {
            if (d.id == null || d.id.isBlank()) {
                d.id = "metier";
            }
            d.id = d.id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            String base = d.id;
            for (int i = 2; seen.contains(d.id); i++) {
                d.id = base + "_" + i;
            }
            seen.add(d.id);
            if (d.name == null) d.name = d.id;
            if (d.game == null) d.game = Game.MINING;
            if (d.blocks == null) d.blocks = new ArrayList<>();
            if (d.tools == null) d.tools = new ArrayList<>();
            if (d.partial == null) d.partial = new ArrayList<>();
            if (d.perfect == null) d.perfect = new ArrayList<>();
            if (d.critical == null) d.critical = new ArrayList<>();
            if (d.recipes == null) d.recipes = new ArrayList<>();
            if (d.depletedBlock == null) d.depletedBlock = "";
            d.difficulty = clamp(d.difficulty, .2, 5);
            d.durability = (int) clamp(d.durability, 0, 1000);
            d.durabilityCritical = (int) clamp(d.durabilityCritical, 0, 1000);
            d.perfectAt = clamp(d.perfectAt, .05, 1);
            d.criticalBelow = clamp(d.criticalBelow, 0, d.perfectAt);
            d.respawnSeconds = (int) clamp(d.respawnSeconds, 0, 7 * 24 * 3600);
            d.uses = (int) clamp(d.uses, 1, 10000);
            d.playerCooldown = (int) clamp(d.playerCooldown, 0, 7 * 24 * 3600);
            d.tools.removeIf(t -> t == null || t.item == null);
            d.tools.forEach(t -> t.difficulty = clamp(t.difficulty, .2, 5));
            for (List<Reward> list : List.of(d.partial, d.perfect, d.critical)) {
                sanitizeRewards(list);
            }
            d.recipes.removeIf(r -> r == null);
            if (d.folders == null) d.folders = new ArrayList<>();
            List<String> folders = new ArrayList<>();
            for (String f : d.folders) {
                String name = folderName(f);
                if (!name.isEmpty() && !folders.contains(name)) {
                    folders.add(name);
                }
            }
            d.folders = folders;
            for (Recipe r : d.recipes) {
                r.folder = folderName(r.folder);
                if (!r.folder.isEmpty() && !d.folders.contains(r.folder)) {
                    d.folders.add(r.folder);
                }
                if (r.name == null) r.name = "Recette";
                if (r.inputs == null) r.inputs = new ArrayList<>();
                if (r.partial == null) r.partial = new ArrayList<>();
                if (r.perfect == null) r.perfect = new ArrayList<>();
                if (r.critical == null) r.critical = new ArrayList<>();
                r.inputs.removeIf(i -> i == null || i.item == null);
                r.inputs.forEach(i -> i.count = (int) clamp(i.count, 1, 64 * 36));
                sanitizeRewards(r.partial);
                sanitizeRewards(r.perfect);
                sanitizeRewards(r.critical);
                r.difficulty = clamp(r.difficulty, .2, 5);
                r.cooldown = (int) clamp(r.cooldown, 0, 7 * 24 * 3600);
            }
        }
        return c;
    }

    /** A folder's name as kept: trimmed, at most 40 characters ("" for none). */
    public static String folderName(@Nullable String name) {
        if (name == null) {
            return "";
        }
        String t = name.strip();
        return t.length() > 40 ? t.substring(0, 40).strip() : t;
    }

    private static void sanitizeRewards(List<Reward> list) {
        list.removeIf(r -> r == null || r.item == null);
        for (Reward r : list) {
            r.min = (int) clamp(r.min, 0, 64 * 36);
            r.max = (int) clamp(r.max, r.min, 64 * 36);
            r.chance = clamp(r.chance, 0, 1);
        }
    }

    private static double clamp(double v, double min, double max) {
        return Double.isNaN(v) ? min : Math.max(min, Math.min(max, v));
    }

    // ---------------------------------------------------------------- items and blocks

    public static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
    }

    @Nullable
    public static BlockState block(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.BLOCK.containsKey(rl)) {
            return null;
        }
        Block b = BuiltInRegistries.BLOCK.get(rl);
        return b.defaultBlockState();
    }

    /** One stack of {@code r}'s item, {@code count} of it, with its data. */
    public static ItemStack stack(Reward r, int count, HolderLookup.Provider registries) {
        Item item = item(r.item);
        if (item == Items.AIR || count <= 0) {
            return ItemStack.EMPTY;
        }
        if (r.components == null || r.components.isEmpty()) {
            return new ItemStack(item, count);
        }
        JsonObject json = new JsonObject();
        json.addProperty("id", r.item);
        json.addProperty("count", Math.max(1, Math.min(count, item.getDefaultMaxStackSize())));
        json.add("components", r.components);
        try {
            ItemStack s = ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), json).getOrThrow();
            s.setCount(count);
            return s;
        } catch (RuntimeException e) {
            ArcanesOublies.LOGGER.warn("Trades: the data of {} could not be read ({}), the plain item is given", r.item, e.getMessage());
            return new ItemStack(item, count);
        }
    }

    /** {@code stack}'s data as a reward's components (only what differs from the item's own), or null. */
    @Nullable
    public static JsonObject components(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty() || stack.getComponentsPatch().isEmpty()) {
            return null;
        }
        try {
            JsonElement el = ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), stack.copyWithCount(1)).getOrThrow();
            JsonObject o = el.getAsJsonObject();
            return o.has("components") ? o.getAsJsonObject("components") : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** What {@code rewards} give this time (chance and count drawn). */
    public static List<ItemStack> roll(List<Reward> rewards, Random random, HolderLookup.Provider registries) {
        List<ItemStack> out = new ArrayList<>();
        for (Reward r : rewards) {
            if (r.chance < 1 && random.nextDouble() >= r.chance) {
                continue;
            }
            int n = r.min + (r.max > r.min ? random.nextInt(r.max - r.min + 1) : 0);
            ItemStack s = stack(r, n, registries);
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    public static JsonObject parseObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    /**
     * A file written by an older version: the trades the mod brought in since (carpentry and medicine in version 2) are
     * added, unless a trade with the same id is already there; nothing else changes.
     */
    public static void upgrade(Config c) {
        if (c.version < 2) {
            for (Def d : defaults().metiers) {
                if ((d.id.equals("menuiserie") || d.id.equals("medecine")) && byIdIn(c, d.id) == null) {
                    c.metiers.add(d);
                }
            }
        }
        if (c.version < 3) {
            // The healers' table of 1.8.0 made a golden apple: an antidote now, as a healer would make
            Def medic = byIdIn(c, "medecine");
            if (medic != null) {
                for (int i = 0; i < medic.recipes.size(); i++) {
                    if (medic.recipes.get(i).name.equals("Pomme de vie")) {
                        medic.recipes.set(i, antidote());
                    }
                }
            }
        }
        c.version = VERSION;
        sanitize(c);
    }

    @Nullable
    private static Def byIdIn(Config c, String id) {
        for (Def d : c.metiers) {
            if (d.id.equals(id)) {
                return d;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- what comes with the mod

    /**
     * The trades the mod starts with: copper, torchflowers, the crafting table, the smithing table, the still, the saw
     * (the stonecutter) and the medical table.
     */
    public static Config defaults() {
        Config c = new Config();
        Def copper = new Def();
        copper.id = "minage_cuivre";
        copper.name = "Minage du cuivre";
        copper.game = Game.MINING;
        copper.blocks = new ArrayList<>(List.of("minecraft:copper_ore", "minecraft:deepslate_copper_ore"));
        copper.tools = new ArrayList<>(List.of(new Tool("minecraft:stone_pickaxe", 1.5), new Tool("minecraft:iron_pickaxe", 1),
                new Tool("minecraft:diamond_pickaxe", .85), new Tool("minecraft:netherite_pickaxe", .75)));
        copper.partial = new ArrayList<>(List.of(new Reward("minecraft:raw_copper", 1, 1)));
        copper.perfect = new ArrayList<>(List.of(new Reward("minecraft:raw_copper", 2, 3)));
        copper.depletedBlock = "minecraft:cobblestone";
        copper.respawnSeconds = 300;
        c.metiers.add(copper);

        Def flower = new Def();
        flower.id = "cueillette_torche";
        flower.name = "Cueillette de la fleur-torche";
        flower.game = Game.HARVEST;
        flower.blocks = new ArrayList<>(List.of("minecraft:torchflower"));
        flower.tools = new ArrayList<>();
        flower.durability = 1;
        flower.durabilityCritical = 3;
        flower.partial = new ArrayList<>(List.of(new Reward("minecraft:torchflower", 1, 1)));
        Reward seeds = new Reward("minecraft:torchflower_seeds", 1, 2);
        seeds.chance = .5;
        flower.perfect = new ArrayList<>(List.of(new Reward("minecraft:torchflower", 2, 3), seeds));
        flower.depletedBlock = "minecraft:short_grass";
        flower.respawnSeconds = 600;
        c.metiers.add(flower);

        Def craft = new Def();
        craft.id = "artisanat";
        craft.name = "Artisanat";
        craft.game = Game.ASSEMBLY;
        craft.blocks = new ArrayList<>(List.of("minecraft:crafting_table"));
        Recipe lantern = new Recipe();
        lantern.name = "Lanterne";
        lantern.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:iron_nugget", 8), new Ingredient("minecraft:torch", 1)));
        lantern.partial = new ArrayList<>(List.of(new Reward("minecraft:lantern", 1, 1)));
        lantern.perfect = new ArrayList<>(List.of(new Reward("minecraft:lantern", 2, 2)));
        Recipe chest = new Recipe();
        chest.name = "Coffre";
        chest.difficulty = .8;
        chest.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:oak_planks", 8)));
        chest.partial = new ArrayList<>(List.of(new Reward("minecraft:chest", 1, 1)));
        chest.perfect = new ArrayList<>(List.of(new Reward("minecraft:chest", 1, 1), new Reward("minecraft:oak_planks", 2, 3)));
        craft.recipes = new ArrayList<>(List.of(lantern, chest));
        craft.durability = 0;
        craft.durabilityCritical = 0;
        c.metiers.add(craft);

        Def forge = new Def();
        forge.id = "forge";
        forge.name = "Forge";
        forge.game = Game.FORGE;
        forge.blocks = new ArrayList<>(List.of("minecraft:smithing_table"));
        Recipe sword = new Recipe();
        sword.name = "Épée en fer";
        sword.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:iron_ingot", 2), new Ingredient("minecraft:stick", 1)));
        sword.partial = new ArrayList<>(List.of(new Reward("minecraft:iron_sword", 1, 1)));
        Reward sharp = new Reward("minecraft:iron_sword", 1, 1);
        sharp.components = parseObject("{\"minecraft:enchantments\":{\"levels\":{\"minecraft:sharpness\":2}},"
                + "\"minecraft:custom_name\":\"{\\\"text\\\":\\\"Lame de maître\\\",\\\"italic\\\":false,\\\"color\\\":\\\"gold\\\"}\"}");
        sword.perfect = new ArrayList<>(List.of(sharp));
        sword.critical = new ArrayList<>(List.of(new Reward("minecraft:iron_nugget", 3, 6)));
        Recipe shield = new Recipe();
        shield.name = "Bouclier";
        shield.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:iron_ingot", 1), new Ingredient("minecraft:oak_planks", 6)));
        shield.partial = new ArrayList<>(List.of(new Reward("minecraft:shield", 1, 1)));
        Reward strong = new Reward("minecraft:shield", 1, 1);
        strong.components = parseObject("{\"minecraft:enchantments\":{\"levels\":{\"minecraft:unbreaking\":2}}}");
        shield.perfect = new ArrayList<>(List.of(strong));
        forge.recipes = new ArrayList<>(List.of(sword, shield));
        forge.durability = 0;
        forge.durabilityCritical = 0;
        c.metiers.add(forge);

        Def still = new Def();
        still.id = "alchimie";
        still.name = "Alchimie";
        still.game = Game.DISTILL;
        still.blocks = new ArrayList<>(List.of(ArcanesOublies.MODID + ":alembic"));
        Recipe heal = new Recipe();
        heal.name = "Potion de soin";
        heal.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:glistering_melon_slice", 1), new Ingredient("minecraft:glass_bottle", 1)));
        Reward potion = new Reward("minecraft:potion", 1, 1);
        potion.components = parseObject("{\"minecraft:potion_contents\":{\"potion\":\"minecraft:healing\"}}");
        heal.partial = new ArrayList<>(List.of(potion));
        Reward strongPotion = new Reward("minecraft:potion", 1, 2);
        strongPotion.components = parseObject("{\"minecraft:potion_contents\":{\"potion\":\"minecraft:strong_healing\"}}");
        heal.perfect = new ArrayList<>(List.of(strongPotion));
        Recipe antidote = new Recipe();
        antidote.name = "Remède (lait)";
        antidote.difficulty = .8;
        antidote.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:honey_bottle", 1), new Ingredient("minecraft:bucket", 1)));
        antidote.partial = new ArrayList<>(List.of(new Reward("minecraft:milk_bucket", 1, 1)));
        antidote.perfect = new ArrayList<>(List.of(new Reward("minecraft:milk_bucket", 1, 1), new Reward("minecraft:glass_bottle", 1, 1)));
        still.recipes = new ArrayList<>(List.of(heal, antidote));
        still.durability = 0;
        still.durabilityCritical = 0;
        c.metiers.add(still);

        Def saw = new Def();
        saw.id = "menuiserie";
        saw.name = "Menuiserie";
        saw.game = Game.CARPENTRY;
        saw.blocks = new ArrayList<>(List.of("minecraft:stonecutter"));
        Recipe planks = new Recipe();
        planks.name = "Planches de chêne";
        planks.difficulty = .8;
        planks.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:oak_log", 1)));
        planks.partial = new ArrayList<>(List.of(new Reward("minecraft:oak_planks", 4, 4)));
        planks.perfect = new ArrayList<>(List.of(new Reward("minecraft:oak_planks", 6, 6)));
        planks.critical = new ArrayList<>(List.of(new Reward("minecraft:stick", 2, 3)));
        Recipe door = new Recipe();
        door.name = "Porte en chêne";
        door.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:oak_planks", 6)));
        door.partial = new ArrayList<>(List.of(new Reward("minecraft:oak_door", 2, 2)));
        door.perfect = new ArrayList<>(List.of(new Reward("minecraft:oak_door", 3, 3)));
        Recipe barrel = new Recipe();
        barrel.name = "Tonneau";
        barrel.difficulty = 1.2;
        barrel.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:oak_planks", 6), new Ingredient("minecraft:oak_slab", 2)));
        barrel.partial = new ArrayList<>(List.of(new Reward("minecraft:barrel", 1, 1)));
        barrel.perfect = new ArrayList<>(List.of(new Reward("minecraft:barrel", 1, 1), new Reward("minecraft:oak_slab", 1, 1)));
        saw.recipes = new ArrayList<>(List.of(planks, door, barrel));
        saw.durability = 0;
        saw.durabilityCritical = 0;
        c.metiers.add(saw);

        Def medic = new Def();
        medic.id = "medecine";
        medic.name = "Médecine";
        medic.game = Game.MEDICINE;
        medic.blocks = new ArrayList<>(List.of(ArcanesOublies.MODID + ":medical_table"));
        Recipe poultice = new Recipe();
        poultice.name = "Cataplasme de soin";
        poultice.difficulty = .8;
        poultice.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:bowl", 1), new Ingredient("minecraft:brown_mushroom", 1),
                new Ingredient("minecraft:red_mushroom", 1), new Ingredient("minecraft:oxeye_daisy", 1)));
        Reward stew = new Reward("minecraft:suspicious_stew", 1, 1);
        stew.components = parseObject("{\"minecraft:suspicious_stew_effects\":[{\"id\":\"minecraft:regeneration\",\"duration\":160}],"
                + "\"minecraft:custom_name\":\"{\\\"text\\\":\\\"Cataplasme de soin\\\",\\\"italic\\\":false}\"}");
        Reward strongStew = new Reward("minecraft:suspicious_stew", 1, 1);
        strongStew.components = parseObject("{\"minecraft:suspicious_stew_effects\":[{\"id\":\"minecraft:regeneration\",\"duration\":320}],"
                + "\"minecraft:custom_name\":\"{\\\"text\\\":\\\"Cataplasme du médecin\\\",\\\"italic\\\":false,\\\"color\\\":\\\"gold\\\"}\"}");
        poultice.partial = new ArrayList<>(List.of(stew));
        poultice.perfect = new ArrayList<>(List.of(strongStew));
        Recipe elixir = new Recipe();
        elixir.name = "Élixir de régénération";
        elixir.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:glass_bottle", 1), new Ingredient("minecraft:glistering_melon_slice", 1),
                new Ingredient("minecraft:honey_bottle", 1)));
        Reward regen = new Reward("minecraft:potion", 1, 1);
        regen.components = parseObject("{\"minecraft:potion_contents\":{\"potion\":\"minecraft:regeneration\"}}");
        Reward longRegen = new Reward("minecraft:potion", 1, 1);
        longRegen.components = parseObject("{\"minecraft:potion_contents\":{\"potion\":\"minecraft:long_regeneration\"}}");
        elixir.partial = new ArrayList<>(List.of(regen));
        elixir.perfect = new ArrayList<>(List.of(longRegen, new Reward("minecraft:glass_bottle", 1, 1)));
        medic.recipes = new ArrayList<>(List.of(poultice, elixir, antidote()));
        medic.durability = 0;
        medic.durabilityCritical = 0;
        c.metiers.add(medic);
        return sanitize(c);
    }

    /** The healers' antidote: honey worked with glow berries, which cures poison (a honey bottle). */
    private static Recipe antidote() {
        Recipe r = new Recipe();
        r.name = "Antidote au miel";
        r.difficulty = 1.1;
        r.inputs = new ArrayList<>(List.of(new Ingredient("minecraft:glass_bottle", 2), new Ingredient("minecraft:honeycomb", 1),
                new Ingredient("minecraft:glow_berries", 2)));
        Reward honey = new Reward("minecraft:honey_bottle", 2, 2);
        honey.components = parseObject("{\"minecraft:custom_name\":\"{\\\"text\\\":\\\"Antidote au miel\\\",\\\"italic\\\":false}\"}");
        Reward more = new Reward("minecraft:honey_bottle", 3, 3);
        more.components = honey.components;
        r.partial = new ArrayList<>(List.of(honey));
        r.perfect = new ArrayList<>(List.of(more));
        return r;
    }

    /** A deep copy (for the editor). */
    public static Config copy(Config c) {
        return GSON.fromJson(GSON.toJson(c), new TypeToken<Config>() {}.getType());
    }
}
