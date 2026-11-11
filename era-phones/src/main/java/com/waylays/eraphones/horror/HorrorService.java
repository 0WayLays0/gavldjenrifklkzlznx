package com.waylays.eraphones.horror;

import com.waylays.eraphones.EraPhones;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The game masters' tools of fear. Sounds only their victims hear, placed round them (steps coming up from behind,
 * knocks, a laugh, children giggling, a music box...), played step by step over the following seconds; what is shown
 * to their senses alone (a whisper in the head, fog, the lights going out, blood, a red balloon, the ground
 * shaking); and what is done to their bodies (floating up, dizziness).
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class HorrorService {
    /** One sound of a sequence: when (ticks), what, how loud and high, where from round the victim (blocks). */
    record Step(int at, String sound, float volume, float pitch, float behind, float side, float up) {}

    private record Pending(UUID target, long due, Step step, @Nullable Vec3 fixed) {}

    private static final Map<String, List<Step>> SOUNDS = new LinkedHashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();
    private static long now;

    static {
        List<Step> steps = new ArrayList<>();
        // Steps coming up from behind, closer each time, then stopping right there
        for (int i = 0; i < 7; i++) {
            steps.add(new Step(i * 8, "block.wood.step", .9f, .8f + (i % 2) * .08f, 8 - i * 1.1f, i % 2 == 0 ? .35f : -.35f, -1.5f));
        }
        SOUNDS.put("pas", List.copyOf(steps));
        SOUNDS.put("toc", List.of(new Step(0, "entity.zombie.attack_wooden_door", .35f, 1.7f, 3.5f, 0, -.3f),
                new Step(5, "entity.zombie.attack_wooden_door", .35f, 1.7f, 3.5f, 0, -.3f), new Step(10, "entity.zombie.attack_wooden_door", .35f, 1.7f, 3.5f, 0, -.3f),
                new Step(40, "entity.zombie.attack_wooden_door", .45f, 1.6f, 3.5f, 0, -.3f), new Step(46, "entity.zombie.attack_wooden_door", .45f, 1.6f, 3.5f, 0, -.3f)));
        SOUNDS.put("porte", List.of(new Step(0, "block.wooden_door.open", 1, .5f, 5, 1, -.5f), new Step(60, "block.wooden_door.close", .8f, .55f, 5, 1, -.5f)));
        SOUNDS.put("rire", List.of(new Step(0, "entity.witch.celebrate", 1, .75f, 6, 0, 0), new Step(14, "entity.witch.celebrate", 1.2f, .74f, 10, 6, 0),
                new Step(28, "entity.witch.celebrate", 1.2f, .73f, 14, -6, 0)));
        SOUNDS.put("enfants", List.of(new Step(0, "entity.allay.ambient_without_item", 1, .7f, 7, -3, -.6f),
                new Step(9, "entity.allay.ambient_without_item", 1, .62f, 8, 3, -.6f), new Step(17, "entity.allay.ambient_without_item", 1, .78f, 9, 0, -.6f),
                new Step(32, "entity.allay.ambient_without_item", 1, .55f, 6, -2, -.6f)));
        SOUNDS.put("murmures", List.of(new Step(0, "ambient.cave", .9f, .7f, 4, 0, 0), new Step(0, "entity.vex.ambient", .8f, .5f, 2, 2, 0),
                new Step(12, "entity.vex.ambient", .8f, .45f, 2, -2, 0), new Step(26, "entity.vex.ambient", .8f, .55f, 1.5f, 1, 0)));
        SOUNDS.put("souffle", List.of(new Step(0, "entity.warden.sniff", .6f, .9f, 1.1f, .3f, 0), new Step(26, "entity.warden.sniff", .7f, .8f, .9f, -.2f, 0)));
        steps = new ArrayList<>();
        int at = 0;
        for (int i = 0; i < 12; i++) {
            steps.add(new Step(at, "entity.warden.heartbeat", 1, 1, 0, 0, -.4f));
            at += Math.max(5, 18 - i * 2);
        }
        SOUNDS.put("coeur", List.copyOf(steps));
        SOUNDS.put("cri", List.of(new Step(0, "entity.ghast.scream", 1.2f, .55f, 12, 0, 0), new Step(4, "block.sculk_shrieker.shriek", 1.2f, .7f, 10, 2, 0)));
        SOUNDS.put("manege", musicBox());
        SOUNDS.put("glas", List.of(new Step(0, "block.bell.use", 1.6f, .5f, 14, 4, 2), new Step(40, "block.bell.use", 1.6f, .5f, 14, 4, 2),
                new Step(80, "block.bell.use", 1.6f, .5f, 14, 4, 2)));
        steps = new ArrayList<>();
        int[] times = {0, 11, 19, 34, 41, 58, 66, 80, 95, 101};
        for (int i = 0; i < times.length; i++) {
            float side = (float) Math.sin(i * 2.4) * 3, behind = (float) Math.cos(i * 1.7) * 3;
            steps.add(new Step(times[i], "block.pointed_dripstone.drip_water", .8f, .8f + (i % 4) * .1f, behind, side, .5f + (i % 3) * .6f));
        }
        SOUNDS.put("gouttes", List.copyOf(steps));
    }

    private HorrorService() {}

    /** Every sound the sequences play, by its id. */
    public static java.util.Set<String> soundIds() {
        java.util.Set<String> ids = new java.util.TreeSet<>();
        for (List<Step> steps : SOUNDS.values()) {
            for (Step s : steps) {
                ids.add(s.sound);
            }
        }
        return ids;
    }

    /** The sounds there are, by name. */
    public static Collection<String> sounds() {
        return SOUNDS.keySet();
    }

    /** A music box winding down: a slow waltz of its own, in a minor key, slightly out of tune, slower at the end. */
    private static List<Step> musicBox() {
        // Semitones from F#4 (the note block's own pitch); 99 is a rest
        int[][] bars = {{3, 6, 10}, {9, 10, 6}, {5, 2, -2}, {3, 99, 99}, {3, 6, 10}, {11, 10, 8}, {6, 5, 2}, {3, -2, -9}};
        int[] bass = {-9, -9, -2, -9, -9, -4, -2, -9};
        List<Step> steps = new ArrayList<>();
        int at = 0;
        for (int b = 0; b < bars.length; b++) {
            int beat = 8 + Math.max(0, b - 5) * 3;
            steps.add(new Step(at, "block.note_block.harp", .6f, pitch(bass[b] + 12) * .5f, 6, 2, 0));
            for (int n : bars[b]) {
                if (n != 99) {
                    float wobble = 1 + (float) Math.sin(at * .7) * .012f;
                    steps.add(new Step(at, "block.note_block.chime", .9f, pitch(n) * wobble, 6, 2, 0));
                }
                at += beat;
            }
        }
        return List.copyOf(steps);
    }

    private static float pitch(int semitones) {
        return (float) Math.pow(2, Math.max(-12, Math.min(12, semitones)) / 12.0);
    }

    // ---------------------------------------------------------------- sounds

    /** Plays the sequence {@code name} to each target, round them or at {@code fixed}; false if there is none. */
    public static boolean sound(Collection<ServerPlayer> targets, String name, @Nullable Vec3 fixed) {
        List<Step> steps = SOUNDS.get(name);
        if (steps == null) {
            return false;
        }
        for (ServerPlayer p : targets) {
            Fear.scare(p, 10);
            for (Step s : steps) {
                PENDING.add(new Pending(p.getUUID(), now + s.at, s, fixed));
            }
        }
        return true;
    }

    private static void play(ServerPlayer p, Step s, @Nullable Vec3 fixed) {
        Optional<Holder.Reference<SoundEvent>> sound = BuiltInRegistries.SOUND_EVENT.getHolder(ResourceLocation.withDefaultNamespace(s.sound));
        if (sound.isEmpty()) {
            EraPhones.LOGGER.warn("No sound {}", s.sound);
            return;
        }
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        Vec3 base = fixed != null ? fixed : p.getEyePosition().subtract(flat.scale(s.behind));
        Vec3 at = base.add(right.scale(s.side)).add(0, s.up, 0);
        p.connection.send(new ClientboundSoundPacket(sound.get(), SoundSource.MASTER, at.x, at.y, at.z, s.volume, s.pitch, p.getRandom().nextLong()));
    }

    // ---------------------------------------------------------------- what is shown, what is done

    /** Sends the effect {@code kind} to each target: for a red balloon, at {@code at} or in front of them. */
    public static void show(Collection<ServerPlayer> targets, String kind, int ticks, String text, @Nullable Vec3 at) {
        for (ServerPlayer p : targets) {
            Vec3 where = at;
            if (where == null) {
                Vec3 look = p.getLookAngle();
                Vec3 flat = new Vec3(look.x, 0, look.z);
                flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
                where = p.getEyePosition().add(flat.scale(6)).add(0, .2, 0);
            }
            PacketDistributor.sendToPlayer(p, new HorrorNet.Effect(kind, ticks, text, where.x, where.y, where.z));
            Fear.scare(p, Fear.fright(kind));
        }
    }

    /** The victims rise slowly into the air and float there, then come down gently. */
    public static void levitate(Collection<ServerPlayer> targets, int ticks) {
        for (ServerPlayer p : targets) {
            p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks, 0, false, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks + 300, 0, false, false, false));
        }
    }

    /** The world turns and sways round the victims. */
    public static void dizzy(Collection<ServerPlayer> targets, int ticks) {
        for (ServerPlayer p : targets) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0, false, false, false));
        }
    }

    /** Ends everything for the targets: the sounds to come, what they see, the dizziness, the floating (gently). */
    public static void stop(Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            UUID id = p.getUUID();
            PENDING.removeIf(e -> e.target.equals(id));
            if (p.hasEffect(MobEffects.LEVITATION)) {
                p.removeEffect(MobEffects.LEVITATION);
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0, false, false, false));
            }
            p.removeEffect(MobEffects.CONFUSION);
            PacketDistributor.sendToPlayer(p, new HorrorNet.Effect("stop", 0, "", 0, 0, 0));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        now++;
        if (PENDING.isEmpty()) {
            return;
        }
        // Sounds due now: played, in order, to whoever is still there
        List<Pending> due = new ArrayList<>();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending e = it.next();
            if (e.due <= now) {
                due.add(e);
                it.remove();
            }
        }
        for (Pending e : due) {
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.target);
            if (p != null) {
                play(p, e.step, e.fixed);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        PENDING.removeIf(e -> e.target.equals(id));
    }
}
