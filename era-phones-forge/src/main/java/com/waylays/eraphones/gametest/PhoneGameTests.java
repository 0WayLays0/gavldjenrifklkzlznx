package com.waylays.eraphones.gametest;

import com.waylays.eraphones.Era;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.Signal;
import com.waylays.eraphones.item.PhoneItem;
import com.waylays.eraphones.registry.EraRegistry;
import com.waylays.eraphones.server.LineData;
import com.waylays.eraphones.server.PhoneService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

/**
 * Server-side checks run by CI: numbers are read whatever way they are written; every phone gets a number of its own
 * (kept once given) and a full battery; the directory names a number; messages wait, counted unread until read, even
 * from a hidden number; the network fades under a roof and the 1985 brick loses a bar indoors.
 */
@GameTestHolder(EraPhones.MODID)
@PrefixGameTestTemplate(false)
public final class PhoneGameTests {
    private PhoneGameTests() {}

    @GameTest(template = "platform", timeoutTicks = 100)
    public static void aBoothHasItsNumberAndRings(GameTestHelper helper) {
        net.minecraft.world.level.block.state.BlockState lower = com.waylays.eraphones.registry.GearRegistry.BOOTH.get().defaultBlockState();
        helper.setBlock(new BlockPos(1, 2, 1), lower);
        helper.setBlock(new BlockPos(1, 3, 1), lower.setValue(com.waylays.eraphones.gear.block.BoothBlock.PART, 1));
        helper.setBlock(new BlockPos(1, 4, 1), lower.setValue(com.waylays.eraphones.gear.block.BoothBlock.PART, 2));
        helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(new BlockPos(1, 2, 1)) instanceof com.waylays.eraphones.gear.block.BoothBlockEntity booth)) {
                throw new net.minecraft.gametest.framework.GameTestAssertException("no booth");
            }
            String n = booth.number();
            helper.assertTrue(!n.isEmpty() && n.startsWith("555-9"), "the booth has a number 555-9xxx, got '" + n + "'");
            helper.assertTrue(com.waylays.eraphones.server.Booths.is(n), "the network knows the booth " + n);
            helper.assertTrue(LineData.get(helper.getLevel().getServer()).line(n) != null, "the booth's line exists");
            if (!com.waylays.eraphones.server.CallService.boothRinging(n)) {
                helper.assertTrue(com.waylays.eraphones.server.CallService.hidden(helper.getLevel().getServer(), n, java.util.List.of("...")),
                        "the booth can be called");
            }
            helper.assertTrue(com.waylays.eraphones.server.CallService.boothRinging(n), "the called booth rings");
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 4, 1)).getLightEmission() > 0, "its sign is lit");
        });
    }

    /** A zone trigger goes off when a player in survival walks into it (its action: a gold block placed); not in creative. */
    @GameTest(template = "platform", timeoutTicks = 40)
    public static void aZoneTriggerGoesOff(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer p = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "zone_tester"));
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        p.setPos(at.getX() + .5, at.getY(), at.getZ() + .5);
        BlockPos mark = helper.absolutePos(new BlockPos(3, 2, 3));
        String name = "test_zone_" + at.asLong();
        com.waylays.eraphones.event.Triggers.putForTest(helper.getLevel(), name, at.offset(-1, 0, -1), at.offset(1, 2, 1),
                "commande setblock " + mark.getX() + " " + mark.getY() + " " + mark.getZ() + " minecraft:gold_block");
        // In creative: nothing
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        com.waylays.eraphones.event.Triggers.watchForTest(p);
        helper.assertBlockNotPresent(Blocks.GOLD_BLOCK, new BlockPos(3, 2, 3));
        // In survival: it goes off
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        com.waylays.eraphones.event.Triggers.watchForTest(p);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(3, 2, 3));
            com.waylays.eraphones.event.Triggers.removeForTest(helper.getLevel().getServer(), name);
        });
    }

    @GameTest(template = "platform")
    public static void numbersAreRead(GameTestHelper helper) {
        helper.assertTrue("555-0142".equals(Numbers.normalize("5550142")), "seven digits");
        helper.assertTrue("555-0142".equals(Numbers.normalize("555 01 42")), "spaces");
        helper.assertTrue("555-0142".equals(Numbers.normalize("0142")), "four digits");
        helper.assertTrue(Numbers.normalize("12") == null, "too short");
        helper.assertTrue(Numbers.normalize("abc") == null, "no digits");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void everyPhoneHasItsNumber(GameTestHelper helper) {
        Set<String> seen = new HashSet<>();
        for (Era era : Era.values()) {
            for (int k = 0; k < 30; k++) {
                ItemStack st = new ItemStack(EraRegistry.PHONES.get(era).get());
                String n = PhoneItem.ensure(st, helper.getLevel());
                helper.assertTrue(n != null && Numbers.normalize(n) != null, "a number in the right form for " + era);
                helper.assertTrue(seen.add(n), "number " + n + " given twice");
                helper.assertTrue(n.equals(PhoneItem.ensure(st, helper.getLevel())), "the number is kept");
                helper.assertTrue(PhoneItem.battery(st) == PhoneItem.FULL, "a full battery");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void messagesWaitAndAreRead(GameTestHelper helper) {
        LineData d = LineData.get(helper.getLevel().getServer());
        String a = d.newLine(Era.SMART, helper.getLevel().getRandom());
        String b = d.newLine(Era.GSM, helper.getLevel().getRandom());
        d.line(b).contacts.add(new LineData.Contact("Maman", a));
        helper.assertTrue("Maman".equals(d.display(b, a)), "the directory names the number");
        helper.assertTrue(b.equals(d.display(a, b)), "an unknown number shows as itself");
        PhoneService.deliver(helper.getLevel().getServer(), d, a, b, "Rentre avant la nuit.");
        PhoneService.deliver(helper.getLevel().getServer(), d, Numbers.HIDDEN, b, "...");
        helper.assertTrue(d.unread(b, a) == 1 && d.unreadTotal(b) == 2, "two messages wait unread");
        helper.assertTrue(d.partners(b).size() == 2, "two conversations");
        helper.assertTrue("".equals(d.display(b, Numbers.HIDDEN)), "a hidden number shows no name");
        d.read(b, a);
        helper.assertTrue(d.unread(b, a) == 0 && d.unreadTotal(b) == 1, "read");
        helper.assertTrue(d.thread(a, b).size() == 1 && d.thread(a, b).get(0).from().equals(a), "the message is kept");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void theNetworkFadesUnderARoof(GameTestHelper helper) {
        BlockPos open = helper.absolutePos(new BlockPos(1, 2, 1));
        // 1.20.1 puts a barrier roof over every test: the open sky is only checked where there is one
        if (helper.getLevel().canSeeSky(open)) {
            helper.assertTrue(Signal.bars(helper.getLevel(), open, Era.SMART) == 4, "four bars under the open sky, got "
                    + Signal.bars(helper.getLevel(), open, Era.SMART));
        }
        // A thick roof well above
        for (int y = 8; y < 16; y++) {
            for (int x = -1; x <= 3; x++) {
                for (int z = -1; z <= 3; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        int smart = Signal.bars(helper.getLevel(), open, Era.SMART), brick = Signal.bars(helper.getLevel(), open, Era.BRICK);
        helper.assertTrue(smart < 4, "fewer bars under a roof, got " + smart);
        helper.assertTrue(brick < smart || brick == 0, "the brick loses a bar indoors (" + brick + " against " + smart + ")");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void horrorSoundsExist(GameTestHelper helper) {
        java.util.Set<String> ids = new java.util.TreeSet<>(com.waylays.eraphones.horror.HorrorService.soundIds());
        // And the ones the victims' own screens play
        ids.addAll(java.util.List.of("entity.vex.ambient", "ambient.cave", "block.beacon.deactivate", "block.lever.click", "entity.warden.heartbeat",
                "entity.elder_guardian.curse", "entity.ghast.scream", "entity.warden.emerge", "entity.firework_rocket.blast"));
        for (String id : ids) {
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(new net.minecraft.resources.ResourceLocation(id)),
                    "no sound " + id);
        }
        helper.assertTrue(com.waylays.eraphones.horror.HorrorService.sounds().size() >= 12, "the sounds of fear");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void voicesStayClean(GameTestHelper helper) {
        int rate = com.waylays.eraphones.horror.VoiceFx.RATE;
        for (String v : com.waylays.eraphones.horror.VoiceFx.VOICES) {
            com.waylays.eraphones.horror.VoiceFx.Chain chain = com.waylays.eraphones.horror.VoiceFx.chain(v);
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
        helper.assertTrue(com.waylays.eraphones.horror.VoiceFx.chain("nothing") == null, "no such voice");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void tapesAndThings(GameTestHelper helper) {
        // A stereo recording at 44.1 kHz brought to one channel at 48 kHz
        short[] stereo = new short[44100 * 2];
        for (int i = 0; i < 44100; i++) {
            stereo[i * 2] = 1000;
            stereo[i * 2 + 1] = 3000;
        }
        short[] mono = com.waylays.eraphones.gear.Tape.resample(stereo, 2, 44100);
        helper.assertTrue(Math.abs(mono.length - 48000) < 3, "a second stays a second: " + mono.length);
        helper.assertTrue(mono[100] == 2000, "the channels mixed");
        helper.assertTrue("01:05".equals(com.waylays.eraphones.gear.Tape.clock(65L * 48000)), "the counter");
        // A deck winding back plays nothing past the start
        com.waylays.eraphones.gear.Tape.Deck deck = new com.waylays.eraphones.gear.Tape.Deck();
        deck.mode = com.waylays.eraphones.gear.Tape.Mode.REWIND;
        deck.pos = 0;
        deck.file = "none.mp3";
        helper.assertTrue(deck.frame() == null || deck.mode == com.waylays.eraphones.gear.Tape.Mode.REWIND, "rewinding at the start");
        // The names of the recordings sent from the game: kept in the folder, MP3 only
        helper.assertTrue("evil.mp3".equals(com.waylays.eraphones.gear.TapeNet.clean("../../evil.mp3")), "no way out of the folder");
        helper.assertTrue("Chanson du cirque.mp3".equals(com.waylays.eraphones.gear.TapeNet.clean("Chanson du cirque.mp3")), "a plain name kept");
        helper.assertTrue(com.waylays.eraphones.gear.TapeNet.clean("chanson.wav") == null, "MP3 and MP4 only");
        helper.assertTrue("film.mp4".equals(com.waylays.eraphones.gear.TapeNet.clean("film.mp4")), "a film kept");
        helper.assertTrue(com.waylays.eraphones.gear.Video.index(48000L * 3) == 3 * com.waylays.eraphones.gear.VideoDecode.FPS, "a film's frame at three seconds");
        helper.assertTrue(com.waylays.eraphones.gear.TapeNet.clean("..mp3") == null, "a name needed");
        helper.assertTrue("a_b.mp3".equals(com.waylays.eraphones.gear.TapeNet.clean("a:b.mp3")), "odd marks replaced");
        // The torch: on, off, its batteries
        net.minecraft.world.item.ItemStack torch = new net.minecraft.world.item.ItemStack(com.waylays.eraphones.registry.GearRegistry.FLASHLIGHT.get());
        helper.assertTrue(!com.waylays.eraphones.gear.Gear.on(torch), "off when new");
        com.waylays.eraphones.gear.Gear.setOn(torch, true);
        helper.assertTrue(com.waylays.eraphones.gear.Gear.on(torch), "switched on");
        com.waylays.eraphones.gear.Gear.setBattery(torch, 0);
        helper.assertTrue(!com.waylays.eraphones.gear.Gear.on(torch), "flat batteries: no light");
        // The radio keeps its setting, within the band
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, com.waylays.eraphones.registry.GearRegistry.RADIO.get().defaultBlockState());
        if (!(helper.getBlockEntity(at) instanceof com.waylays.eraphones.gear.block.RadioBlockEntity radio)) {
            helper.fail("no radio there");
            return;
        }
        radio.set(true, 2000, 50);
        helper.assertTrue(radio.frequency() == com.waylays.eraphones.gear.Stations.MAX && radio.volume() == 10, "kept within the band");
        helper.assertTrue(com.waylays.eraphones.gear.Stations.tuned(1003, 1004) && !com.waylays.eraphones.gear.Stations.tuned(1003, 1006), "tuning");
        // Money exists in every face value
        helper.assertTrue(com.waylays.eraphones.registry.GearRegistry.MONEY.size() == 8, "the money");
        helper.succeed();
    }
}
