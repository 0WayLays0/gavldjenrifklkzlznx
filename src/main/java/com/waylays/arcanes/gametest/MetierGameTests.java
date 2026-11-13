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
}
