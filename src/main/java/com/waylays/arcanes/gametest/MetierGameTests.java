package com.waylays.arcanes.gametest;

import com.mojang.authlib.GameProfile;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.metier.Deposits;
import com.waylays.arcanes.metier.MetierDefs;
import com.waylays.arcanes.metier.MetierService;
import com.waylays.arcanes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * Server-side checks run by CI for the trades: the five default trades are there, a minigame on copper ore can only
 * be started with one of its pickaxes, and its outcome is settled as set (perfect: 2 to 3 ore and 1 durability;
 * partial: 1 ore and 1; critical failure: no ore and 5), the ore turns to cobblestone and comes back, survival players
 * cannot break it, and a station's recipe takes its ingredients and gives its result.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class MetierGameTests {
    private static final BlockPos ORE = new BlockPos(2, 2, 2);

    private MetierGameTests() {}

    private static ServerPlayer player(GameTestHelper helper, ItemStack held) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "metier_tester"));
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 4));
        p.setPos(at.getX() + .5, at.getY(), at.getZ() + .5);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        return p;
    }

    /** Copper ore placed, a minigame started on it and settled with {@code score}; returns the player. */
    private static ServerPlayer mine(GameTestHelper helper, ItemStack pick, float score, boolean critical) {
        helper.setBlock(ORE, Blocks.COPPER_ORE);
        ServerPlayer p = player(helper, pick);
        BlockPos pos = helper.absolutePos(ORE);
        helper.assertTrue(MetierService.startForTest(p, pos), "the minigame did not start with " + pick.getHoverName().getString());
        helper.assertTrue(MetierService.sessionOf(p) >= 0, "no session going on");
        MetierService.finishForTest(p, score, critical);
        helper.assertTrue(MetierService.sessionOf(p) < 0, "the session was not settled");
        return p;
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void theFiveTradesAreThere(GameTestHelper helper) {
        helper.assertTrue(MetierDefs.forBlock(Blocks.COPPER_ORE.defaultBlockState()) != null, "no trade on copper ore");
        helper.assertTrue(MetierDefs.forBlock(Blocks.COPPER_ORE.defaultBlockState()).game == MetierDefs.Game.MINING, "copper ore is not mined");
        helper.assertTrue(MetierDefs.forBlock(Blocks.TORCHFLOWER.defaultBlockState()).game == MetierDefs.Game.HARVEST, "torchflowers are not gathered");
        MetierDefs.Def craft = MetierDefs.forBlock(Blocks.CRAFTING_TABLE.defaultBlockState());
        helper.assertTrue(craft != null && craft.isStation() && craft.game == MetierDefs.Game.ASSEMBLY, "the crafting table is not a station");
        helper.assertTrue(MetierDefs.forBlock(Blocks.SMITHING_TABLE.defaultBlockState()).game == MetierDefs.Game.FORGE, "the smithing table does not forge");
        MetierDefs.Def still = MetierDefs.forBlock(ModBlocks.ALEMBIC.get().defaultBlockState());
        helper.assertTrue(still != null && still.game == MetierDefs.Game.DISTILL, "the alembic does not distill");
        MetierDefs.Def saw = MetierDefs.forBlock(Blocks.STONECUTTER.defaultBlockState());
        helper.assertTrue(saw != null && saw.isStation() && saw.game == MetierDefs.Game.CARPENTRY, "the stonecutter is not the carpenter's saw");
        MetierDefs.Def medic = MetierDefs.forBlock(ModBlocks.MEDICAL_TABLE.get().defaultBlockState());
        helper.assertTrue(medic != null && medic.isStation() && medic.game == MetierDefs.Game.MEDICINE, "the medical table is not the healers' station");
        ItemStack poultice = MetierDefs.stack(medic.recipes.get(0).partial.get(0), 1, helper.getLevel().registryAccess());
        helper.assertTrue(poultice.is(Items.SUSPICIOUS_STEW) && poultice.has(DataComponents.SUSPICIOUS_STEW_EFFECTS), "the poultice lost its effect: " + poultice);
        // A file from before the two new trades gets them, and keeps its own
        MetierDefs.Config old = new MetierDefs.Config();
        old.version = 1;
        MetierDefs.Def mine = new MetierDefs.Def();
        mine.id = "ma_mine";
        old.metiers.add(mine);
        MetierDefs.upgrade(old);
        helper.assertTrue(old.metiers.size() == 3 && old.metiers.get(0).id.equals("ma_mine") && old.version == MetierDefs.VERSION,
                "an old file was not brought up to date: " + old.metiers.stream().map(d -> d.id).toList());
        // A file of 1.8.0: the healers' golden apple becomes the antidote, the rest of the trade kept
        MetierDefs.Config v2 = MetierDefs.copy(MetierDefs.defaults());
        v2.version = 2;
        MetierDefs.Def healers = v2.metiers.stream().filter(d -> d.id.equals("medecine")).findFirst().orElseThrow();
        MetierDefs.Recipe apple = new MetierDefs.Recipe();
        apple.name = "Pomme de vie";
        healers.recipes.set(2, apple);
        healers.name = "Mes soigneurs";
        MetierDefs.upgrade(v2);
        helper.assertTrue(healers.recipes.get(2).name.equals("Antidote au miel") && healers.name.equals("Mes soigneurs"),
                "the 1.8.0 healers' table was not brought up to date: " + healers.recipes.get(2).name);
        // A recipe with its own minigame keeps it through the file
        MetierDefs.Config own = MetierDefs.copy(MetierDefs.defaults());
        own.metiers.get(2).recipes.get(0).game = MetierDefs.Game.DISTILL;
        helper.assertTrue(MetierDefs.fromJson(MetierDefs.toJson(own)).metiers.get(2).recipes.get(0).game == MetierDefs.Game.DISTILL,
                "a recipe's own minigame was lost");
        // The file's round trip, and an item with data
        MetierDefs.Config again = MetierDefs.fromJson(MetierDefs.toJson(MetierDefs.get()));
        helper.assertTrue(again.metiers.size() == MetierDefs.all().size(), "the trades do not survive their JSON");
        MetierDefs.Def forge = MetierDefs.byId("forge");
        helper.assertTrue(forge != null, "no forge trade");
        ItemStack blade = MetierDefs.stack(forge.recipes.get(0).perfect.get(0), 1, helper.getLevel().registryAccess());
        helper.assertTrue(blade.is(Items.IRON_SWORD) && blade.isEnchanted() && blade.has(DataComponents.CUSTOM_NAME),
                "the master's blade lost its enchantment or name: " + blade);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void aPerfectStrikeGivesTwoOrThreeOre(GameTestHelper helper) {
        ServerPlayer p = mine(helper, new ItemStack(Items.IRON_PICKAXE), .97f, false);
        int ore = p.getInventory().countItem(Items.RAW_COPPER);
        helper.assertTrue(ore >= 2 && ore <= 3, "a perfect result gives 2 to 3 raw copper, got " + ore);
        helper.assertTrue(p.getMainHandItem().getDamageValue() == 1, "a perfect result costs 1 durability, cost " + p.getMainHandItem().getDamageValue());
        helper.assertBlockPresent(Blocks.COBBLESTONE, ORE);
        helper.assertTrue(Deposits.get(helper.getLevel().getServer()).spent(helper.getLevel().dimension(), helper.absolutePos(ORE)) != null,
                "the worked-out deposit is not remembered");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void aPartialStrikeGivesOneOre(GameTestHelper helper) {
        ServerPlayer p = mine(helper, new ItemStack(Items.IRON_PICKAXE), .5f, false);
        helper.assertTrue(p.getInventory().countItem(Items.RAW_COPPER) == 1, "a partial result gives 1 raw copper");
        helper.assertTrue(p.getMainHandItem().getDamageValue() == 1, "a partial result costs 1 durability");
        helper.assertBlockPresent(Blocks.COBBLESTONE, ORE);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void aCriticalFailureCostsFiveAndGivesNothing(GameTestHelper helper) {
        ServerPlayer p = mine(helper, new ItemStack(Items.STONE_PICKAXE), .1f, false);
        helper.assertTrue(p.getInventory().countItem(Items.RAW_COPPER) == 0, "a critical failure gives nothing");
        helper.assertTrue(p.getMainHandItem().getDamageValue() == 5, "a critical failure costs 5 durability, cost " + p.getMainHandItem().getDamageValue());
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void givingUpIsACriticalFailure(GameTestHelper helper) {
        ServerPlayer p = mine(helper, new ItemStack(Items.IRON_PICKAXE), .99f, true);
        helper.assertTrue(p.getInventory().countItem(Items.RAW_COPPER) == 0, "giving up gives nothing");
        helper.assertTrue(p.getMainHandItem().getDamageValue() == 5, "giving up costs 5 durability");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void copperNeedsAPickaxe(GameTestHelper helper) {
        helper.setBlock(ORE, Blocks.COPPER_ORE);
        ServerPlayer p = player(helper, new ItemStack(Items.STICK));
        helper.assertFalse(MetierService.startForTest(p, helper.absolutePos(ORE)), "a stick started the mining minigame");
        helper.assertTrue(MetierService.sessionOf(p) < 0, "a session was opened without a pickaxe");
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, timeoutTicks = 80)
    public static void theDepositComesBack(GameTestHelper helper) {
        mine(helper, new ItemStack(Items.IRON_PICKAXE), .97f, false);
        helper.assertBlockPresent(Blocks.COBBLESTONE, ORE);
        // Bring its return forward to now
        Deposits deposits = Deposits.get(helper.getLevel().getServer());
        BlockPos pos = helper.absolutePos(ORE);
        Deposits.Spent spent = deposits.spent(helper.getLevel().dimension(), pos);
        helper.assertTrue(spent != null, "no worked-out deposit");
        deposits.forget(helper.getLevel().dimension(), pos);
        deposits.deplete(helper.getLevel().dimension(), pos, spent.original, spent.def, helper.getLevel().getServer().overworld().getGameTime());
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.COPPER_ORE, ORE);
            helper.assertTrue(deposits.spent(helper.getLevel().dimension(), pos) == null, "the deposit is still listed as worked out");
        });
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void survivalPlayersCannotBreakTheOre(GameTestHelper helper) {
        helper.setBlock(ORE, Blocks.COPPER_ORE);
        ServerPlayer p = player(helper, new ItemStack(Items.IRON_PICKAXE));
        p.gameMode.destroyBlock(helper.absolutePos(ORE));
        helper.assertBlockPresent(Blocks.COPPER_ORE, ORE);
        helper.succeed();
    }

    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void aStationRecipeTakesAndGives(GameTestHelper helper) {
        helper.setBlock(ORE, Blocks.CRAFTING_TABLE);
        ServerPlayer p = player(helper, ItemStack.EMPTY);
        p.getInventory().add(new ItemStack(Items.IRON_NUGGET, 8));
        p.getInventory().add(new ItemStack(Items.TORCH, 1));
        MetierService.pickForTest(p, "artisanat", helper.absolutePos(ORE), 0);
        helper.assertTrue(MetierService.sessionOf(p) >= 0, "choosing the lantern did not start the minigame");
        MetierService.finishForTest(p, .97f, false);
        helper.assertTrue(p.getInventory().countItem(Items.LANTERN) == 2, "a perfect lantern gives 2, got " + p.getInventory().countItem(Items.LANTERN));
        helper.assertTrue(p.getInventory().countItem(Items.IRON_NUGGET) == 0 && p.getInventory().countItem(Items.TORCH) == 0,
                "the ingredients were not used up");
        helper.assertBlockPresent(Blocks.CRAFTING_TABLE, ORE);
        // Without the ingredients, nothing starts
        MetierService.pickForTest(p, "artisanat", helper.absolutePos(ORE), 0);
        helper.assertTrue(MetierService.sessionOf(p) < 0, "a recipe started without its ingredients");
        helper.succeed();
    }

    /**
     * The grindstone: a critical success gives back 200 durability and keeps the weapon away for 12 hours; a failure
     * takes 50 without breaking it; a weapon kept from the grindstone, and an unworn one, do not start it.
     */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true)
    public static void grindstoneRepairsAndRests(GameTestHelper helper) {
        helper.setBlock(ORE, Blocks.GRINDSTONE);
        BlockPos pos = helper.absolutePos(ORE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(1000);
        ServerPlayer p = player(helper, sword);
        helper.assertTrue(MetierService.grindForTest(p, pos), "the grindstone's minigame did not start with a worn sword");
        MetierService.finishForTest(p, .95f, false);
        helper.assertTrue(p.getMainHandItem().getDamageValue() == 800, "a critical success gives back 200, left " + p.getMainHandItem().getDamageValue());
        helper.assertTrue(!MetierService.grindForTest(p, pos), "the same sword went back to the grindstone before 12 hours");

        ItemStack other = new ItemStack(Items.IRON_SWORD);
        other.setDamageValue(other.getMaxDamage() - 10);
        p.setItemInHand(InteractionHand.MAIN_HAND, other);
        helper.assertTrue(MetierService.grindForTest(p, pos), "the grindstone's minigame did not start with another sword");
        MetierService.finishForTest(p, .1f, false);
        helper.assertTrue(p.getMainHandItem().getDamageValue() == p.getMainHandItem().getMaxDamage() - 1 && !p.getMainHandItem().isEmpty(),
                "a failure wears the blade without breaking it");

        ItemStack whole = new ItemStack(Items.IRON_SWORD);
        p.setItemInHand(InteractionHand.MAIN_HAND, whole);
        helper.assertTrue(!MetierService.grindForTest(p, pos), "an unworn sword started the grindstone");
        helper.succeed();
    }

    /**
     * A station's recipes sorted into folders (as game masters do in the trades' editor): a folder made, renamed with
     * its recipe, moved, deleted (its recipes back outside), a recipe moved within its folder; the folders survive the
     * file, and a folder a recipe names but the list lacks is added. Done on a copy: the trades are not touched.
     */
    @GameTest(template = "platform", setupTicks = GameTestSetup.TICKS, skyAccess = true, batch = "metier_folders")
    public static void stationRecipesGoInFolders(GameTestHelper helper) {
        MetierDefs.Def craft = MetierDefs.copy(MetierDefs.get()).metiers.stream().filter(d -> d.id.equals("artisanat")).findFirst().orElseThrow();
        helper.assertTrue(craft.recipes.size() >= 2, "no crafting station with two recipes");
        String first = craft.recipes.get(0).name, second = craft.recipes.get(1).name;
        String light = craft.newFolder("  Éclairage ");
        String other = craft.newFolder("Éclairage");
        helper.assertTrue(light.equals("Éclairage") && other.equals("Éclairage 2"), "the folders are named " + craft.folders);
        craft.recipes.get(0).folder = light;
        helper.assertTrue(craft.renameFolder(light, "Lumières") && craft.recipes.get(0).folder.equals("Lumières"), "the recipe did not follow its folder's new name");
        helper.assertTrue(!craft.renameFolder("Lumières", "Éclairage 2"), "a folder took a name already taken");
        craft.moveFolder("Éclairage 2", -1);
        helper.assertTrue(craft.folders.equals(List.of("Éclairage 2", "Lumières")), "the folder did not move up: " + craft.folders);
        // Two recipes outside any folder swap; the one in a folder is passed over
        craft.recipes.get(1).folder = "";
        craft.recipes.add(new MetierDefs.Recipe());
        int moved = craft.moveRecipe(1, 1);
        helper.assertTrue(moved == craft.recipes.size() - 1 && craft.recipes.get(moved).name.equals(second), "the recipe did not move within its folder");
        MetierDefs.Config config = new MetierDefs.Config();
        config.metiers.add(craft);
        MetierDefs.Def again = MetierDefs.fromJson(MetierDefs.toJson(config)).metiers.get(0);
        helper.assertTrue(again.folders.equals(craft.folders) && again.recipes.get(0).folder.equals("Lumières"), "the folders do not survive the file");
        craft.deleteFolder("Lumières");
        helper.assertTrue(!craft.folders.contains("Lumières") && craft.recipes.get(0).folder.isEmpty() && craft.recipes.get(0).name.equals(first),
                "the deleted folder's recipe was not put back outside");
        craft.recipes.get(0).folder = "Oubli";
        MetierDefs.Config lost = new MetierDefs.Config();
        lost.metiers.add(craft);
        helper.assertTrue(MetierDefs.fromJson(MetierDefs.toJson(lost)).metiers.get(0).folders.contains("Oubli"), "a recipe's folder missing from the list was not added");
        helper.assertTrue(MetierDefs.byId("artisanat").folders.isEmpty(), "the real crafting station was changed");
        helper.succeed();
    }
}
