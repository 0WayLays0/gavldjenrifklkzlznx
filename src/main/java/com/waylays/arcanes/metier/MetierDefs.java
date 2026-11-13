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
        DISTILL
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
        /** Whether the station's own screen (the vanilla crafting table…) can still be opened from its recipe list. */
        public boolean vanillaButton = true;
        /** Seconds between two attempts by the same player (0: none). */
        public int playerCooldown = 0;

        public boolean isStation() {
            return !recipes.isEmpty();
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

    /** The whole file. */
    public static final class Config {
        public int version = 1;
        public List<Def> metiers = new ArrayList<>();
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
            for (Recipe r : d.recipes) {
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

    // ---------------------------------------------------------------- what comes with the mod

    /** The five trades the mod starts with: copper, torchflowers, the crafting table, the smithing table, the still. */
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
        still.name = "Alchimie et médecine";
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
        return sanitize(c);
    }

    /** A deep copy (for the editor). */
    public static Config copy(Config c) {
        return GSON.fromJson(GSON.toJson(c), new TypeToken<Config>() {}.getType());
    }
}
