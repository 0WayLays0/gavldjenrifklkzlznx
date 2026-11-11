package com.waylays.eraphones.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What Minecraft 1.21 keeps as an item's data (its custom name, its lore, unbreakable, a glint, its attribute
 * modifiers, the items inside it), in the item's tag as 1.20.1 does.
 */
public final class ItemData {
    public static final String GLINT = "eraphones:glint";
    public static final String ITEMS = "eraphones:items";

    private ItemData() {}

    @Nullable
    public static Component name(ItemStack s) {
        return s.hasCustomHoverName() ? s.getHoverName() : null;
    }

    public static void setName(ItemStack s, @Nullable Component name) {
        if (name == null) {
            s.resetHoverName();
        } else {
            s.setHoverName(name);
        }
    }

    public static List<Component> lore(ItemStack s) {
        List<Component> out = new ArrayList<>();
        CompoundTag d = s.getTagElement("display");
        if (d != null && d.contains("Lore", Tag.TAG_LIST)) {
            ListTag l = d.getList("Lore", Tag.TAG_STRING);
            for (int i = 0; i < l.size(); i++) {
                try {
                    Component c = Component.Serializer.fromJson(l.getString(i));
                    if (c != null) {
                        out.add(c);
                    }
                } catch (RuntimeException ignored) {
                    // Not a component
                }
            }
        }
        return out;
    }

    public static void setLore(ItemStack s, List<Component> lines) {
        if (lines.isEmpty()) {
            CompoundTag d = s.getTagElement("display");
            if (d != null) {
                d.remove("Lore");
            }
            return;
        }
        ListTag l = new ListTag();
        for (Component c : lines) {
            l.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        s.getOrCreateTagElement("display").put("Lore", l);
    }

    public static boolean unbreakable(ItemStack s) {
        return s.getTag() != null && s.getTag().getBoolean("Unbreakable");
    }

    public static void setUnbreakable(ItemStack s, boolean on) {
        if (on) {
            s.getOrCreateTag().putBoolean("Unbreakable", true);
        } else if (s.getTag() != null) {
            s.getTag().remove("Unbreakable");
        }
    }

    /** A glint set by the workshop (drawn by ItemStackGlintMixin). */
    public static boolean glint(ItemStack s) {
        return s.getTag() != null && s.getTag().getBoolean(GLINT);
    }

    public static void setGlint(ItemStack s, boolean on) {
        if (on) {
            s.getOrCreateTag().putBoolean(GLINT, true);
        } else if (s.getTag() != null) {
            s.getTag().remove(GLINT);
        }
    }

    public record Modifier(Attribute attribute, AttributeModifier modifier, EquipmentSlot slot) {}

    /** Every attribute modifier the item gives, slot by slot (its own, or those set on it). */
    public static List<Modifier> modifiers(ItemStack s) {
        List<Modifier> out = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (Map.Entry<Attribute, AttributeModifier> e : s.getAttributeModifiers(slot).entries()) {
                out.add(new Modifier(e.getKey(), e.getValue(), slot));
            }
        }
        return out;
    }

    /** Sets the item's attribute modifiers (none given: the item's own again). */
    public static void setModifiers(ItemStack s, List<Modifier> mods) {
        if (s.getTag() != null) {
            s.getTag().remove("AttributeModifiers");
        }
        for (Modifier m : mods) {
            s.addAttributeModifier(m.attribute(), m.modifier(), m.slot());
        }
    }

    /** The items kept inside this one (a backpack's, a walkman's cassette). */
    public static List<ItemStack> items(ItemStack s) {
        List<ItemStack> out = new ArrayList<>();
        if (s.getTag() != null && s.getTag().contains(ITEMS, Tag.TAG_LIST)) {
            ListTag l = s.getTag().getList(ITEMS, Tag.TAG_COMPOUND);
            for (int i = 0; i < l.size(); i++) {
                out.add(ItemStack.of(l.getCompound(i)));
            }
        }
        return out;
    }

    public static void setItems(ItemStack s, List<ItemStack> items) {
        ListTag l = new ListTag();
        for (ItemStack i : items) {
            l.add(i.save(new CompoundTag()));
        }
        boolean any = items.stream().anyMatch(i -> !i.isEmpty());
        if (!any) {
            if (s.getTag() != null) {
                s.getTag().remove(ITEMS);
            }
            return;
        }
        s.getOrCreateTag().put(ITEMS, l);
    }
}
