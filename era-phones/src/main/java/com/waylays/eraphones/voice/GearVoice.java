package com.waylays.eraphones.voice;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.Audio;
import com.waylays.eraphones.gear.Gear;
import com.waylays.eraphones.gear.Stations;
import com.waylays.eraphones.gear.Tape;
import com.waylays.eraphones.gear.WalkieItem;
import com.waylays.eraphones.gear.block.RadioBlockEntity;
import com.waylays.eraphones.registry.GearRegistry;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.mp3.Mp3Decoder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The things' sound through Simple Voice Chat: what comes out of a video recorder (a sound played from its place), a
 * walkie-talkie's voice (sent to every walkie-talkie on the same channel, out of its little speaker), a station on the
 * air (out of every radio tuned to it). The voices go through an old speaker's sound first.
 */
final class GearVoice implements Audio {
    /** How far a walkie-talkie's speaker is heard round whoever carries it. */
    private static final float TALKIE_RANGE = 7;
    /** A gap this long (ms) between two frames: a new transmission (a squelch is heard). */
    private static final long GAP = 450;

    private final VoicechatServerApi api;
    private final Map<UUID, EntityAudioChannel> talkieChannels = new ConcurrentHashMap<>();
    private final Map<RadioBlockEntity, LocationalAudioChannel> radioChannels = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTalk = new ConcurrentHashMap<>();

    GearVoice(VoicechatServerApi api) {
        this.api = api;
    }

    // ---------------------------------------------------------------- Audio

    @Nullable
    @Override
    public short[] decodeMp3(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            Mp3Decoder decoder = api.createMp3Decoder(in);
            if (decoder == null) {
                return null;
            }
            short[] samples = decoder.decode();
            AudioFormat format = decoder.getAudioFormat();
            return Tape.resample(samples, format.getChannels(), format.getSampleRate());
        } catch (Exception e) {
            EraPhones.LOGGER.warn("Cannot decode {}: {}", file, e.toString());
            return null;
        }
    }

    @Override
    public Object play(ServerLevel level, Vec3 pos, float distance, Supplier<short[]> frames) {
        LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(level), api.createPosition(pos.x, pos.y, pos.z));
        if (channel == null) {
            return null;
        }
        channel.setDistance(distance);
        AudioPlayer player = api.createAudioPlayer(channel, api.createEncoder(), frames);
        player.startPlaying();
        return player;
    }

    @Override
    public void stop(Object handle) {
        if (handle instanceof AudioPlayer p) {
            p.stopPlaying();
        }
    }

    @Override
    public boolean playing(Object handle) {
        return handle instanceof AudioPlayer p && !p.isStopped();
    }

    // ---------------------------------------------------------------- the voice through things

    /** A walkie-talkie switched on in either hand of {@code p}, if any. */
    @Nullable
    private static ItemStack talkieInHand(ServerPlayer p) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(hand);
            if (s.getItem() instanceof WalkieItem && Gear.on(s)) {
                return s;
            }
        }
        return null;
    }

    /** Whether {@code p} carries a walkie-talkie switched on to {@code channel}. */
    private static boolean listening(ServerPlayer p, int channel) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof WalkieItem && Gear.on(s) && Gear.channel(s) == channel) {
                return true;
            }
        }
        return false;
    }

    /** {@code data}, a frame of {@code speaker}'s microphone (already in their changed voice), through walkie-talkies and radios. */
    void transmit(ServerPlayer speaker, byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        ItemStack talkie = talkieInHand(speaker);
        Integer onAir = Stations.frequency(speaker.getUUID());
        if (talkie == null && onAir == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastTalk.put(speaker.getUUID(), now);
        boolean fresh = last == null || now - last > GAP;
        UUID key = UUID.nameUUIDFromBytes(("speaker-" + speaker.getUUID()).getBytes());
        byte[] radio = VoiceStreams.process(api, key, "radio", data);
        if (radio == null) {
            return;
        }
        if (talkie != null) {
            int channel = Gear.channel(talkie);
            for (ServerPlayer p : speaker.server.getPlayerList().getPlayers()) {
                if (p == speaker || p.level() != speaker.level() || !listening(p, channel)) {
                    continue;
                }
                if (fresh) {
                    speaker.server.execute(() -> p.level().playSound(null, p.blockPosition(), GearRegistry.TALKIE_SQUELCH.get(), SoundSource.PLAYERS, .45f, 1));
                }
                EntityAudioChannel ch = talkieChannels.computeIfAbsent(p.getUUID(),
                        id -> api.createEntityAudioChannel(UUID.nameUUIDFromBytes(("talkie-" + id).getBytes()), api.fromEntity(p)));
                if (ch != null) {
                    ch.setDistance(TALKIE_RANGE);
                    ch.send(radio);
                }
            }
        }
        if (onAir != null) {
            for (RadioBlockEntity r : Stations.radios(onAir)) {
                if (!(r.getLevel() instanceof ServerLevel level)) {
                    continue;
                }
                LocationalAudioChannel ch = radioChannels.computeIfAbsent(r, b -> {
                    Vec3 c = b.getBlockPos().getCenter();
                    return api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(level), api.createPosition(c.x, c.y, c.z));
                });
                if (ch != null) {
                    ch.setDistance(4 + r.volume() * 2);
                    ch.send(radio);
                }
            }
            radioChannels.keySet().removeIf(RadioBlockEntity::isRemoved);
        }
    }

    /** {@code p} left: their walkie-talkie's channel is let go. */
    void forget(UUID player) {
        talkieChannels.remove(player);
        lastTalk.remove(player);
    }
}
