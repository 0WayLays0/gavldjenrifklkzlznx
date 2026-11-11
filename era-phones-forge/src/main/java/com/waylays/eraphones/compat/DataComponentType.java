package com.waylays.eraphones.compat;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.UnaryOperator;

/**
 * A piece of data an item carries (a phone's number, a cassette's recording...), as Minecraft 1.21 has them; on
 * 1.20.1 kept in the item's tag under the mod's own key, written with its codec.
 */
public final class DataComponentType<T> {
    private String key = "?";
    private final Codec<T> codec;

    private DataComponentType(Codec<T> codec) {
        this.codec = codec;
    }

    void name(String name) {
        this.key = "eraphones:" + name;
    }

    public String key() {
        return key;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private Codec<T> codec;

        public Builder<T> persistent(Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<T> networkSynchronized(Object ignored) {
            return this;
        }

        public DataComponentType<T> build() {
            return new DataComponentType<>(codec);
        }
    }

    @Nullable
    public T get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) {
            return null;
        }
        return codec.parse(NbtOps.INSTANCE, tag.get(key)).result().orElse(null);
    }

    public T getOrDefault(ItemStack stack, T fallback) {
        T v = get(stack);
        return v == null ? fallback : v;
    }

    public boolean has(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key);
    }

    /** Sets it (null: takes it off); the value it had. */
    @Nullable
    public T set(ItemStack stack, @Nullable T value) {
        T old = get(stack);
        if (value == null) {
            remove(stack);
            return old;
        }
        Tag t = codec.encodeStart(NbtOps.INSTANCE, value).result().orElse(null);
        if (t != null) {
            stack.getOrCreateTag().put(key, t);
        }
        return old;
    }

    @Nullable
    public T remove(ItemStack stack) {
        T old = get(stack);
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(key);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
        return old;
    }

    public T update(ItemStack stack, T fallback, UnaryOperator<T> op) {
        T v = op.apply(getOrDefault(stack, fallback));
        set(stack, v);
        return v;
    }
}
