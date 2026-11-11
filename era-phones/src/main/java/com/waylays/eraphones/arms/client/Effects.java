package com.waylays.eraphones.arms.client;

import com.waylays.eraphones.arms.Proc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Every effect the workshop can put on an item: its own actions first, then all the game's and every mod's effects. */
public final class Effects {
    public record Entry(String id, Component name, String mod) {}

    private static final Map<String, ItemStack> SPECIAL_ICONS = Map.of(
            "*feu", new ItemStack(Items.BLAZE_POWDER), "*foudre", new ItemStack(Items.LIGHTNING_ROD), "*recul", new ItemStack(Items.PISTON),
            "*vol_de_vie", new ItemStack(Items.REDSTONE), "*degats", new ItemStack(Items.IRON_SWORD), "*epines", new ItemStack(Items.CACTUS),
            "*soin", new ItemStack(Items.GOLDEN_APPLE), "*gel", new ItemStack(Items.SNOWBALL));

    private Effects() {}

    public static List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        for (String s : Proc.SPECIALS) {
            out.add(new Entry(s, name(s), "atelier"));
        }
        List<Entry> effects = new ArrayList<>();
        for (Map.Entry<net.minecraft.resources.ResourceKey<MobEffect>, MobEffect> e : BuiltInRegistries.MOB_EFFECT.entrySet()) {
            ResourceLocation id = e.getKey().location();
            effects.add(new Entry(id.toString(), Component.translatable(e.getValue().getDescriptionId()), id.getNamespace()));
        }
        effects.sort(Comparator.comparing((Entry x) -> !x.mod.equals("minecraft")).thenComparing(x -> x.name.getString()));
        out.addAll(effects);
        return out;
    }

    public static Component name(String id) {
        if (id.startsWith("*")) {
            return Component.translatable("arms.eraphones.special." + id.substring(1));
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        MobEffect e = rl == null ? null : BuiltInRegistries.MOB_EFFECT.get(rl);
        return e == null ? Component.literal(id) : Component.translatable(e.getDescriptionId());
    }

    /** Whether it is good for whoever gets it (a colour for it). */
    public static int colour(String id) {
        if (id.startsWith("*")) {
            return 0xFFFFC46A;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        MobEffect e = rl == null ? null : BuiltInRegistries.MOB_EFFECT.get(rl);
        if (e == null) {
            return 0xFFB0B0B0;
        }
        return switch (e.getCategory()) {
            case BENEFICIAL -> 0xFF8CE38C;
            case HARMFUL -> 0xFFFF8A80;
            default -> 0xFFC8D2DC;
        };
    }

    /** Its little picture, {@code size} across. */
    public static void icon(GuiGraphics g, String id, int x, int y, int size) {
        if (id.startsWith("*")) {
            ItemStack s = SPECIAL_ICONS.get(id);
            if (s != null) {
                g.pose().pushPose();
                g.pose().translate(x, y, 0);
                g.pose().scale(size / 16f, size / 16f, 1);
                g.renderItem(s, 0, 0);
                g.pose().popPose();
            }
            return;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Holder<MobEffect> h = rl == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
        if (h != null) {
            g.blit(x, y, 0, size, size, Minecraft.getInstance().getMobEffectTextures().get(h));
        }
    }
}
