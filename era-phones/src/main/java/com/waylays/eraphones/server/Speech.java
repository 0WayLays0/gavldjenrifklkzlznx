package com.waylays.eraphones.server;

import com.waylays.eraphones.EraConfig;
import com.waylays.eraphones.EraPhones;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * A line said into a phone, as those around hear it: through Arcanes Oubliés' role play when it is there (the
 * character's name, the range of speech, those who cannot see the speaker left out), else as a plain line in the chat
 * of the players nearby.
 */
final class Speech {
    private static final int RANGE = 20;
    private static MethodHandle say;
    private static boolean looked;

    private Speech() {}

    static void aloud(ServerPlayer p, String text) {
        MethodHandle h = roleplay();
        if (h != null) {
            try {
                h.invoke(p, text);
                return;
            } catch (Throwable t) {
                EraPhones.LOGGER.warn("Arcanes Oubliés could not say the line: {}", t.toString());
            }
        }
        Component line = Component.literal(EraConfig.name(p)).withStyle(ChatFormatting.BOLD)
                .append(Component.literal(" : « " + text + " »").withStyle(s -> s.withBold(false)));
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (o.level() == p.level() && o.distanceTo(p) <= RANGE) {
                o.sendSystemMessage(line);
            }
        }
    }

    private static synchronized MethodHandle roleplay() {
        if (!looked) {
            looked = true;
            if (net.neoforged.fml.ModList.get().isLoaded("arcanes_oublies")) {
                try {
                    Class<?> c = Class.forName("com.waylays.arcanes.rp.RpService");
                    say = MethodHandles.publicLookup().findStatic(c, "say", MethodType.methodType(void.class, ServerPlayer.class, String.class));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    EraPhones.LOGGER.warn("Arcanes Oubliés' speech was not found: {}", e.toString());
                }
            }
        }
        return say;
    }
}
