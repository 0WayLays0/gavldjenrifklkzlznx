package com.waylays.arcanes.compat.epicnpc.client;

import com.waylays.arcanes.compat.epicnpc.EpicNpc;
import com.waylays.arcanes.compat.epicnpc.EpicNpcNet;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;

/**
 * The "Combat Epic Fight: Oui / Non" switch in Custom NPCs' NPC edit screens (opened with the NPC wand): a button
 * beside Custom NPCs' panel, on each of its tabs, that switches the NPC between Epic Fight's combat and Custom NPCs'
 * plain combat. The choice is the server's ({@link EpicNpcNet}); only game masters and players in creative mode can
 * make it.
 */
public final class NpcScreenSwitch {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    private NpcScreenSwitch() {}

    static void init() {
        NeoForge.EVENT_BUS.addListener(NpcScreenSwitch::onInit);
    }

    private static void onInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        // Custom NPCs' NPC edit screens (its main menu tabs: display, stats, AI, inventory, advanced)
        if (!screen.getClass().getName().startsWith("noppes.npcs.client.gui.mainmenu.")) {
            return;
        }
        Entity npc = editedNpc(screen);
        if (npc == null) {
            return;
        }
        int[] at = place(screen);
        Button button = Button.builder(label(npc), b -> {
            boolean toEpicFight = npc.getTags().contains(EpicNpc.CLASSIC_TAG);
            EpicNpcNet.setClassic(npc, !toEpicFight);
            PacketDistributor.sendToServer(new EpicNpcNet.Switch(npc.getId(), toEpicFight));
            b.setMessage(label(npc));
        }).bounds(at[0], at[1], WIDTH, HEIGHT).tooltip(Tooltip.create(Component.translatable("gui.arcanes_oublies.epicnpc.tooltip"))).build();
        event.addListener(button);
    }

    private static Component label(Entity npc) {
        boolean on = !npc.getTags().contains(EpicNpc.CLASSIC_TAG);
        return Component.translatable("gui.arcanes_oublies.epicnpc.switch",
                Component.translatable(on ? "gui.arcanes_oublies.epicnpc.on" : "gui.arcanes_oublies.epicnpc.off"));
    }

    /** The NPC the screen edits: the first field of the screen (or its parents) that holds a Custom NPCs NPC. */
    private static Entity editedNpc(Screen screen) {
        for (Class<?> c = screen.getClass(); c != null && c != Screen.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!Entity.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    if (f.get(screen) instanceof Entity e && EpicNpc.isNpc(e)) {
                        return e;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // not readable: keep looking
                }
            }
        }
        return null;
    }

    /**
     * Where the button goes: right of Custom NPCs' panel if there is room, else above its right corner, else below it,
     * else in the screen's top right corner.
     */
    private static int[] place(Screen screen) {
        int left = intField(screen, "guiLeft", intField(screen, "leftPos", -1));
        int top = intField(screen, "guiTop", intField(screen, "topPos", -1));
        int width = intField(screen, "xSize", intField(screen, "imageWidth", -1));
        int height = intField(screen, "ySize", intField(screen, "imageHeight", -1));
        if (left >= 0 && top >= 0 && width > 0 && height > 0) {
            if (left + width + 4 + WIDTH <= screen.width) {
                return new int[] {left + width + 4, top};
            }
            if (top >= HEIGHT + 4) {
                return new int[] {Math.max(2, left + width - WIDTH), top - HEIGHT - 4};
            }
            if (top + height + 2 + HEIGHT <= screen.height) {
                return new int[] {Math.max(2, left + width - WIDTH), top + height + 2};
            }
        }
        return new int[] {screen.width - WIDTH - 4, 4};
    }

    private static int intField(Object owner, String name, int otherwise) {
        for (Class<?> c = owner.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                if (f.getType() != int.class) {
                    return otherwise;
                }
                f.setAccessible(true);
                return f.getInt(owner);
            } catch (NoSuchFieldException ignored) {
                // look in the parent class
            } catch (ReflectiveOperationException | RuntimeException ex) {
                return otherwise;
            }
        }
        return otherwise;
    }
}
