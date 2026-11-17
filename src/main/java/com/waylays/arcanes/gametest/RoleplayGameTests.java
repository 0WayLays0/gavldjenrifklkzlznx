package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Server-side checks of the role play and the game masters' tools (once part of Époque RP): a zone trigger goes off
 * for a player in survival, not in creative; every sound /horreur plays exists; every voice of /voix stays a clean,
 * audible voice (and the deep one is lower); a character's name is kept and shown.
 */
@GameTestHolder(ArcanesOublies.MODID)
@PrefixGameTestTemplate(false)
public final class RoleplayGameTests {
    private RoleplayGameTests() {}

    /** A zone trigger goes off when a player in survival walks into it (its action: a gold block placed); not in creative. */
    @GameTest(template = "platform", skyAccess = true, timeoutTicks = 40)
    public static void aZoneTriggerGoesOff(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "zone_tester"));
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        p.setPos(at.getX() + .5, at.getY(), at.getZ() + .5);
        BlockPos mark = helper.absolutePos(new BlockPos(3, 2, 3));
        String name = "test_zone_" + at.asLong();
        com.waylays.arcanes.rp.triggers.Triggers.putForTest(helper.getLevel(), name, at.offset(-1, 0, -1), at.offset(1, 2, 1),
                "commande setblock " + mark.getX() + " " + mark.getY() + " " + mark.getZ() + " minecraft:gold_block");
        // In creative: nothing
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        com.waylays.arcanes.rp.triggers.Triggers.watchForTest(p);
        helper.assertBlockNotPresent(Blocks.GOLD_BLOCK, new BlockPos(3, 2, 3));
        // In survival: it goes off
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        com.waylays.arcanes.rp.triggers.Triggers.watchForTest(p);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(3, 2, 3));
            com.waylays.arcanes.rp.triggers.Triggers.removeForTest(helper.getLevel().getServer(), name);
        });
    }

    @GameTest(template = "platform", skyAccess = true)
    public static void horrorSoundsExist(GameTestHelper helper) {
        java.util.Set<String> ids = new java.util.TreeSet<>(com.waylays.arcanes.rp.horror.HorrorService.soundIds());
        // And the ones the victims' own screens play
        ids.addAll(java.util.List.of("entity.vex.ambient", "ambient.cave", "block.beacon.deactivate", "block.lever.click", "entity.warden.heartbeat",
                "entity.elder_guardian.curse", "entity.ghast.scream", "entity.warden.emerge", "entity.firework_rocket.blast"));
        for (String id : ids) {
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(net.minecraft.resources.ResourceLocation.withDefaultNamespace(id)),
                    "no sound " + id);
        }
        helper.assertTrue(com.waylays.arcanes.rp.horror.HorrorService.sounds().size() >= 12, "the sounds of fear");
        helper.succeed();
    }

    @GameTest(template = "platform", skyAccess = true)
    public static void voicesStayClean(GameTestHelper helper) {
        int rate = com.waylays.arcanes.rp.horror.VoiceFx.RATE;
        for (String v : com.waylays.arcanes.rp.horror.VoiceFx.VOICES) {
            com.waylays.arcanes.rp.horror.VoiceFx.Chain chain = com.waylays.arcanes.rp.horror.VoiceFx.chain(v);
            helper.assertTrue(chain != null, "voice " + v);
            double energy = 0;
            int crossings = 0;
            short last = 0;
            for (int frame = 0; frame < 100; frame++) {
                short[] in = new short[960];
                for (int i = 0; i < 960; i++) {
                    in[i] = (short) (Math.sin(2 * Math.PI * 200 * (frame * 960 + i) / rate) * 12000);
                }
                short[] out = chain.process(in);
                helper.assertTrue(out.length == 960, "a frame stays a frame");
                for (short x : out) {
                    if (frame >= 50) {
                        energy += x * (double) x;
                        if ((x < 0) != (last < 0)) {
                            crossings++;
                        }
                    }
                    last = x;
                }
            }
            double rms = Math.sqrt(energy / (50 * 960));
            helper.assertTrue(rms > 1500 && rms < 30000, "voice " + v + " loudness " + (int) rms);
            if (v.equals("grave")) {
                double hz = crossings / 2.0 / (50 * 960.0 / rate);
                helper.assertTrue(Math.abs(hz - 160) < 12, "the deep voice is lower: " + (int) hz + " Hz");
            }
        }
        helper.assertTrue(com.waylays.arcanes.rp.horror.VoiceFx.chain("nothing") == null, "no such voice");
        helper.succeed();
    }

    /** A character's name, once given, is what the others see of the player (and their account's name is not). */
    @GameTest(template = "platform", skyAccess = true)
    public static void aCharacterHasItsName(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "name_tester"));
        helper.assertFalse(com.waylays.arcanes.rp.RpService.named(p), "no name yet");
        com.waylays.arcanes.rp.RpData.get(helper.getLevel().getServer()).setName(p.getUUID(), "Bill", "Denbrough");
        helper.assertTrue(com.waylays.arcanes.rp.RpService.named(p), "named");
        helper.assertTrue("Bill Denbrough".equals(com.waylays.arcanes.rp.RpService.name(p)), "the name: " + com.waylays.arcanes.rp.RpService.name(p));
        com.waylays.arcanes.rp.RpData.get(helper.getLevel().getServer()).setName(p.getUUID(), "", "");
        helper.succeed();
    }

    /**
     * The new staff and role-play tools: a warp found whatever its capitals and taken; a range read before a text
     * ("/me 40 ..."); the narration's chat line; /repair to full and to a value; the workshop's durability.
     */
    @GameTest(template = "platform", skyAccess = true)
    public static void warpsRangesNarrationAndRepair(GameTestHelper helper) {
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        com.waylays.arcanes.rp.warp.Warps warps = com.waylays.arcanes.rp.warp.Warps.get(server);
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        warps.put(new com.waylays.arcanes.rp.warp.Warps.Warp("Taverne_Test", helper.getLevel().dimension().location(), at.getX() + .5, at.getY(),
                at.getZ() + .5, 90, 0));
        helper.assertTrue(warps.find("taverne_test") != null && warps.find("TAVERNE_TEST") != null, "a warp is found whatever its capitals");
        net.minecraft.server.level.ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "warp_tester"));
        // Where the warp leads (a test's fake player cannot be teleported: its connection does nothing)
        com.waylays.arcanes.rp.warp.Warps.Warp w = warps.find("tAvErNe_TeSt");
        helper.assertTrue(BlockPos.containing(w.x(), w.y(), w.z()).equals(at) && w.yaw() == 90
                && w.world().equals(helper.getLevel().dimension().location()), "the warp keeps where it leads");
        helper.assertTrue(warps.remove("taverne_TEST") != null && warps.find("Taverne_Test") == null, "a warp is deleted whatever its capitals");

        java.util.regex.Matcher m = com.waylays.arcanes.rp.RpCommands.ranged("40 lève la main");
        helper.assertTrue(m != null && m.group(1).equals("40") && m.group(2).equals("lève la main"), "a range before the text");
        helper.assertTrue(com.waylays.arcanes.rp.RpCommands.ranged("lève 2 mains") == null, "no range: the text as it is");

        String line = com.waylays.arcanes.rp.horror.HorrorCommands.narrationLine("La pluie tombe.").getString();
        helper.assertTrue(line.contains("[NARRATION] : La pluie tombe."), "the narration reads [NARRATION] : text, not " + line);

        net.minecraft.world.item.ItemStack sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        int max = sword.getMaxDamage();
        sword.setDamageValue(max - 3);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        net.minecraft.commands.CommandSourceStack source = p.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, "repair 100");
        helper.assertTrue(p.getMainHandItem().getMaxDamage() - p.getMainHandItem().getDamageValue() == 100, "/repair 100 leaves 100");
        server.getCommands().performPrefixedCommand(source, "repair full");
        helper.assertTrue(p.getMainHandItem().getDamageValue() == 0, "/repair full repairs it all");
        helper.succeed();
    }
}
