package com.waylays.eraphones.arms;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Map;

/**
 * An effect with a chance, on an item: rolled when its holder strikes ({@code guard} false), or when its wearer (or
 * holder) is struck ({@code guard} true); {@code chance} percent of the time. {@code type} is any effect of the game
 * or of a mod ("minecraft:poison", "somemod:bleeding"), or one of the workshop's own actions ("*feu": sets on fire,
 * "*foudre", "*recul", "*vol_de_vie", "*degats", "*epines", "*soin", "*gel"). It falls on the other one (the one
 * struck, or the attacker) or on oneself ({@code self}). {@code power}: its level, or points, or a percent;
 * {@code seconds}: how long it lasts.
 */
public record Proc(String type, float chance, int power, int seconds, boolean guard, boolean self) {
    public static final Codec<Proc> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("type").forGetter(Proc::type),
            Codec.FLOAT.fieldOf("chance").forGetter(Proc::chance),
            Codec.INT.fieldOf("power").forGetter(Proc::power),
            Codec.INT.fieldOf("seconds").forGetter(Proc::seconds),
            Codec.BOOL.optionalFieldOf("guard", false).forGetter(Proc::guard),
            Codec.BOOL.optionalFieldOf("self", false).forGetter(Proc::self)).apply(i, Proc::new));
    public static final Codec<List<Proc>> LIST_CODEC = CODEC.listOf();
    public static final StreamCodec<RegistryFriendlyByteBuf, Proc> STREAM = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Proc::type, ByteBufCodecs.FLOAT, Proc::chance, ByteBufCodecs.VAR_INT, Proc::power,
            ByteBufCodecs.VAR_INT, Proc::seconds, ByteBufCodecs.BOOL, Proc::guard, ByteBufCodecs.BOOL, Proc::self, Proc::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, List<Proc>> LIST_STREAM = STREAM.apply(ByteBufCodecs.list(16));

    /** The workshop's own actions (not effects of the game). */
    public static final List<String> SPECIALS = List.of("*feu", "*foudre", "*recul", "*vol_de_vie", "*degats", "*epines", "*soin", "*gel");

    public boolean special() {
        return type.startsWith("*");
    }

    /** The first workshop's kinds, read as these. */
    private static final Map<String, String[]> OLD = Map.ofEntries(
            Map.entry("feu", new String[] {"*feu", "0", "0"}), Map.entry("foudre", new String[] {"*foudre", "0", "0"}),
            Map.entry("recul", new String[] {"*recul", "0", "0"}), Map.entry("vol_de_vie", new String[] {"*vol_de_vie", "0", "1"}),
            Map.entry("degats", new String[] {"*degats", "0", "0"}), Map.entry("gel", new String[] {"*gel", "0", "0"}),
            Map.entry("poison", new String[] {"minecraft:poison", "0", "0"}), Map.entry("wither", new String[] {"minecraft:wither", "0", "0"}),
            Map.entry("lenteur", new String[] {"minecraft:slowness", "0", "0"}), Map.entry("faiblesse", new String[] {"minecraft:weakness", "0", "0"}),
            Map.entry("cecite", new String[] {"minecraft:blindness", "0", "0"}), Map.entry("nausee", new String[] {"minecraft:nausea", "0", "0"}),
            Map.entry("levitation", new String[] {"minecraft:levitation", "0", "0"}), Map.entry("epines", new String[] {"*epines", "1", "0"}),
            Map.entry("brulure", new String[] {"*feu", "1", "0"}), Map.entry("repousser", new String[] {"*recul", "1", "0"}),
            Map.entry("ralentir", new String[] {"minecraft:slowness", "1", "0"}), Map.entry("soin", new String[] {"*soin", "1", "1"}),
            Map.entry("resistance", new String[] {"minecraft:resistance", "1", "1"}), Map.entry("vitesse", new String[] {"minecraft:speed", "1", "1"}),
            Map.entry("absorption", new String[] {"minecraft:absorption", "1", "1"}), Map.entry("regeneration", new String[] {"minecraft:regeneration", "1", "1"}),
            Map.entry("invisibilite", new String[] {"minecraft:invisibility", "1", "1"}));

    /** This, as the workshop now reads it (the first workshop's kinds turned to the new ones). */
    public Proc normalized() {
        String[] o = OLD.get(type);
        return o == null ? this : new Proc(o[0], chance, power, seconds, o[1].equals("1"), o[2].equals("1"));
    }
}
