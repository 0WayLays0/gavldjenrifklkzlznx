package com.waylays.eraphones.arms;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * An effect with a chance: on a weapon, when it strikes ({@code chance} percent of the time); on armour (or anything
 * held), when its bearer is struck. {@code power} is its strength (a level, points of damage or health, a percent),
 * {@code seconds} how long it lasts.
 */
public record Proc(String type, float chance, int power, int seconds) {
    public static final Codec<Proc> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("type").forGetter(Proc::type),
            Codec.FLOAT.fieldOf("chance").forGetter(Proc::chance),
            Codec.INT.fieldOf("power").forGetter(Proc::power),
            Codec.INT.fieldOf("seconds").forGetter(Proc::seconds)).apply(i, Proc::new));
    public static final Codec<List<Proc>> LIST_CODEC = CODEC.listOf();
    public static final StreamCodec<RegistryFriendlyByteBuf, Proc> STREAM = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Proc::type, ByteBufCodecs.FLOAT, Proc::chance, ByteBufCodecs.VAR_INT, Proc::power,
            ByteBufCodecs.VAR_INT, Proc::seconds, Proc::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, List<Proc>> LIST_STREAM = STREAM.apply(ByteBufCodecs.list(16));

    /** The kinds: their name, whether they come of a blow struck (else of a blow taken), what they use. */
    public enum Kind {
        FEU("feu", true, false, true), POISON("poison", true, true, true), WITHER("wither", true, true, true),
        LENTEUR("lenteur", true, true, true), FAIBLESSE("faiblesse", true, true, true), CECITE("cecite", true, false, true),
        NAUSEE("nausee", true, false, true), GEL("gel", true, true, true), FOUDRE("foudre", true, true, false),
        RECUL("recul", true, true, false), VOL_DE_VIE("vol_de_vie", true, true, false), DEGATS("degats", true, true, false),
        LEVITATION("levitation", true, true, true),
        EPINES("epines", false, true, false), BRULURE("brulure", false, false, true), REPOUSSER("repousser", false, true, false),
        RALENTIR("ralentir", false, true, true), SOIN("soin", false, true, false), RESISTANCE("resistance", false, true, true),
        VITESSE("vitesse", false, true, true), ABSORPTION("absorption", false, true, true), REGENERATION("regeneration", false, true, true),
        INVISIBILITE("invisibilite", false, false, true);

        public final String id;
        public final boolean strike, usesPower, usesSeconds;

        Kind(String id, boolean strike, boolean usesPower, boolean usesSeconds) {
            this.id = id;
            this.strike = strike;
            this.usesPower = usesPower;
            this.usesSeconds = usesSeconds;
        }

        public static Kind of(String id) {
            for (Kind k : values()) {
                if (k.id.equals(id)) {
                    return k;
                }
            }
            return null;
        }
    }
}
