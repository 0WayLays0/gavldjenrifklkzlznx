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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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

    @GameTest(template = "platform", skyAccess = true)
    public static void numbersAreRead(GameTestHelper helper) {
        helper.assertTrue("555-0142".equals(Numbers.normalize("5550142")), "seven digits");
        helper.assertTrue("555-0142".equals(Numbers.normalize("555 01 42")), "spaces");
        helper.assertTrue("555-0142".equals(Numbers.normalize("0142")), "four digits");
        helper.assertTrue(Numbers.normalize("12") == null, "too short");
        helper.assertTrue(Numbers.normalize("abc") == null, "no digits");
        helper.succeed();
    }

    @GameTest(template = "platform", skyAccess = true)
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

    @GameTest(template = "platform", skyAccess = true)
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

    @GameTest(template = "platform", skyAccess = true)
    public static void theNetworkFadesUnderARoof(GameTestHelper helper) {
        BlockPos open = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(Signal.bars(helper.getLevel(), open, Era.SMART) == 4, "four bars under the open sky, got "
                + Signal.bars(helper.getLevel(), open, Era.SMART));
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
}
