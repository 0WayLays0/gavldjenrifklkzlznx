package com.waylays.eraphones.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.waylays.eraphones.EraConfig;
import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.gear.Gear;
import com.waylays.eraphones.item.PhoneItem;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * What the zone triggers of Arcanes Oubliés (/declencheur) ask of this mod's things, through commands for game masters:
 * {@code /epoquerp lampe_casser <joueur>} breaks the flashlight they carry lit (else any they carry), and
 * {@code /epoquerp sms <joueur> <de|inconnu> <texte>} sends a text message to their phone.
 */
final class TriggerHooks {
    private TriggerHooks() {}

    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("epoquerp").requires(s -> s.hasPermission(EraConfig.staffPermission()))
                .then(Commands.literal("lampe_casser").then(Commands.argument("joueur", EntityArgument.player())
                        .executes(c -> breakTorch(EntityArgument.getPlayer(c, "joueur")))))
                .then(Commands.literal("sms").then(Commands.argument("joueur", EntityArgument.player())
                        .then(Commands.argument("de", StringArgumentType.word()).then(Commands.argument("texte", StringArgumentType.greedyString())
                                .executes(c -> sms(EntityArgument.getPlayer(c, "joueur"), StringArgumentType.getString(c, "de"),
                                        StringArgumentType.getString(c, "texte"))))))));
    }

    /** The flashlight the player carries lit (else any they carry) breaks: a pop of glass, darkness. */
    static int breakTorch(ServerPlayer p) {
        ItemStack pick = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(GearRegistry.FLASHLIGHT.get()) && !Gear.broken(s) && (pick.isEmpty() || Gear.on(s))) {
                pick = s;
            }
        }
        if (pick.isEmpty()) {
            return 0;
        }
        pick.set(GearRegistry.BROKEN.get(), true);
        Gear.setOn(pick, false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, .7f, 1.6f);
        return 1;
    }

    /** A text message on the player's phone, from a number or from no one ("inconnu"). */
    static int sms(ServerPlayer p, String from, String text) {
        String fromN = from.equalsIgnoreCase("inconnu") ? Numbers.HIDDEN : Numbers.normalize(from);
        if (fromN == null) {
            fromN = Numbers.HIDDEN;
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.getItem() instanceof PhoneItem) {
                String to = PhoneItem.ensure(s, p.serverLevel());
                if (to != null) {
                    PhoneService.deliver(p.server, LineData.get(p.server), fromN, to, text);
                    return 1;
                }
            }
        }
        return 0;
    }
}
