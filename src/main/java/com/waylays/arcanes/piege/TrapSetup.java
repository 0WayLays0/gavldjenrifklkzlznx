package com.waylays.arcanes.piege;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.waylays.arcanes.rp.arms.Proc;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * What a game master set on a trap with /piege: its damage (health points, 2 is a heart; -1: the trap's own), and the
 * effects it may give whoever it catches, each with its chance (a {@link Proc}: an effect of the game or of a mod, or
 * one of the workshop's own: "*feu" sets alight, "*gel" freezes, "*foudre" calls lightning, "*recul" throws back;
 * {@code power} its level, {@code seconds} how long it lasts).
 */
public record TrapSetup(float damage, List<Proc> effects) {
    public static final TrapSetup NONE = new TrapSetup(-1, List.of());
    /** The most effects a trap carries. */
    public static final int MAX_EFFECTS = 8;

    public static final Codec<TrapSetup> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("degats", -1f).forGetter(TrapSetup::damage),
            Proc.LIST_CODEC.optionalFieldOf("effets", List.of()).forGetter(TrapSetup::effects)).apply(i, TrapSetup::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrapSetup> STREAM = StreamCodec.composite(
            ByteBufCodecs.FLOAT, TrapSetup::damage, Proc.LIST_STREAM, TrapSetup::effects, TrapSetup::new);

    /** The damage it deals, for a trap of {@code kind}. */
    public float damageFor(TrapKind kind) {
        return damage < 0 ? kind.damage : damage;
    }

    /** Clamped to sane values: damage 0 to 200, at most {@link #MAX_EFFECTS} effects, each chance 0 to 100, level 0 to 255, 0 to 3600 s. */
    public TrapSetup sane() {
        List<Proc> kept = effects.stream().filter(p -> p != null && p.type() != null && !p.type().isBlank() && p.chance() > 0).limit(MAX_EFFECTS)
                .map(p -> new Proc(p.type(), Math.min(100, p.chance()), Math.max(0, Math.min(255, p.power())), Math.max(0, Math.min(3600, p.seconds())),
                        false, false))
                .toList();
        return new TrapSetup(damage < 0 ? -1 : Math.min(200, damage), kept);
    }
}
