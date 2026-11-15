package com.waylays.arcanes.compat.epicnpc;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * What Arcanes reads of a Custom NPCs NPC, by reflection (Custom NPCs is not needed to build Arcanes): the mob it is
 * disguised as, the size set in its menu, whether "Basic Limited Renderer" is on, and the armour layer of its renderer.
 * Every read gives up quietly (null, 5, false) if Custom NPCs changes.
 */
public final class Cnpc {
    private static boolean failed;
    private static Field modelData;
    private static Method getEntity;
    private static Field simpleRender;
    private static Field display;
    private static Method getSize;

    private Cnpc() {}

    /** The mob the NPC is disguised as in Custom NPCs' model menu, or null (it looks like a person). */
    public static LivingEntity disguise(Entity npc) {
        Object data = modelData(npc);
        if (data == null) {
            return null;
        }
        try {
            if (getEntity == null) {
                for (Method m : data.getClass().getMethods()) {
                    if (m.getName().equals("getEntity") && m.getParameterCount() == 1) {
                        getEntity = m;
                        break;
                    }
                }
                if (getEntity == null) {
                    return give("ModelData.getEntity");
                }
            }
            return getEntity.invoke(data, npc) instanceof LivingEntity e ? e : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return give(ex.toString());
        }
    }

    /** Whether "Basic Limited Renderer" is on in the NPC's model menu. */
    public static boolean simpleRender(Entity npc) {
        Object data = modelData(npc);
        if (data == null) {
            return false;
        }
        try {
            if (simpleRender == null) {
                simpleRender = data.getClass().getField("simpleRender");
            }
            return simpleRender.getBoolean(data);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            give(ex.toString());
            return false;
        }
    }

    /** The NPC's size in its display menu (5 is a person's size). */
    public static int size(Entity npc) {
        if (failed) {
            return 5;
        }
        try {
            if (display == null) {
                display = field(npc.getClass(), "display");
            }
            Object d = display == null ? null : display.get(npc);
            if (d == null) {
                return 5;
            }
            if (getSize == null) {
                getSize = d.getClass().getMethod("getSize");
            }
            return getSize.invoke(d) instanceof Integer i ? i : 5;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            give(ex.toString());
            return 5;
        }
    }

    /** A public field of a Custom NPCs object (its renderer's armour layer, for one), or null. */
    public static Object publicField(Object owner, String name) {
        if (owner == null || failed) {
            return null;
        }
        try {
            return owner.getClass().getField(name).get(owner);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static Object modelData(Entity npc) {
        if (failed || npc == null) {
            return null;
        }
        try {
            if (modelData == null) {
                modelData = field(npc.getClass(), "modelData");
                if (modelData == null) {
                    return null;
                }
            }
            return modelData.getDeclaringClass().isInstance(npc) ? modelData.get(npc) : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            give(ex.toString());
            return null;
        }
    }

    private static Field field(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // keep looking in the parent class
            }
        }
        return null;
    }

    private static <T> T give(String why) {
        if (!failed) {
            failed = true;
            ArcanesOublies.LOGGER.warn("Custom NPCs could not be read ({}): its NPCs keep Epic Fight's plain human body", why);
        }
        return null;
    }
}
