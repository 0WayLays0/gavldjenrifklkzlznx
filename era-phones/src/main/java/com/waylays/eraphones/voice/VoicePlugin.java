package com.waylays.eraphones.voice;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.server.CallService;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * With Simple Voice Chat (only loaded if it is installed): the two ends of a phone call hear each other wherever they
 * are, as through the phone, while those standing near each still hear them speak; walkie-talkies and radios carry
 * the voice; answering machines and camcorders record it. (A voice changed with /voix, in Arcanes Oubliés, is
 * changed before this hears it.)
 */
@ForgeVoicechatPlugin
public class VoicePlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return EraPhones.MODID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent.class, e -> {
            gear = new GearVoice(e.getVoicechat());
            com.waylays.eraphones.gear.Audio.set(gear);
        });
        registration.registerEvent(de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent.class, e -> {
            gear = null;
            com.waylays.eraphones.gear.Audio.set(null);
        });
        registration.registerEvent(de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent.class, e -> {
            if (gear != null) {
                gear.forget(e.getPlayerUuid());
            }
            VoiceStreams.forget(e.getPlayerUuid());
        });
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || !(sender.getPlayer().getPlayer() instanceof ServerPlayer speaker)) {
            return;
        }
        VoicechatServerApi api = event.getVoicechat();
        if (gear != null) {
            try {
                gear.transmit(speaker, event.getPacket().getOpusEncodedData());
            } catch (Throwable t) {
                if (!gearWarned) {
                    gearWarned = true;
                    EraPhones.LOGGER.warn("Walkie-talkie or radio voice failed: {}", t.toString());
                }
            }
        }
        recordForMachine(speaker, event.getPacket().getOpusEncodedData(), api);
        if (com.waylays.eraphones.EraConfig.phoneVoice()) {
            UUID other = CallService.otherEnd(speaker.getUUID());
            if (other != null) {
                VoicechatConnection c = api.getConnectionOf(other);
                if (c != null) {
                    api.sendStaticSoundPacketTo(c, throughLine(event, speaker, api));
                }
            }
        }
    }

    /**
     * The voice as the other end hears it: through the line and the ear-piece of the phone it is spoken into (an
     * old car phone hisses and crackles, today's is clear); as it is if that cannot be done.
     */
    private de.maxhenkel.voicechat.api.packets.StaticSoundPacket throughLine(MicrophonePacketEvent event, ServerPlayer speaker,
                                                                              VoicechatServerApi api) {
        de.maxhenkel.voicechat.api.packets.MicrophonePacket packet = event.getPacket();
        com.waylays.eraphones.Era era = CallService.voiceEra(speaker.getUUID());
        if (era == null) {
            return packet.toStaticSoundPacket();
        }
        try {
            byte[] heard = VoiceStreams.line(api, speaker.getUUID(), era.item, packet.getOpusEncodedData());
            if (heard == null) {
                return packet.toStaticSoundPacket();
            }
            // The packet made from the line's sound; the microphone's own left as it was for those nearby
            byte[] own = packet.getOpusEncodedData();
            packet.setOpusEncodedData(heard);
            de.maxhenkel.voicechat.api.packets.StaticSoundPacket out = packet.toStaticSoundPacket();
            packet.setOpusEncodedData(own);
            return out;
        } catch (Throwable t) {
            if (!lineWarned) {
                lineWarned = true;
                EraPhones.LOGGER.warn("The phone line's sound could not be made: {}", t.toString());
            }
            return packet.toStaticSoundPacket();
        }
    }

    private final java.util.Map<UUID, de.maxhenkel.voicechat.api.opus.OpusDecoder> machineDecoders = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The voice taken down by things that record it: an answering machine the speaker is leaving a message on, the
     * camcorders filming near them.
     */
    private void recordForMachine(ServerPlayer speaker, byte[] data, VoicechatServerApi api) {
        UUID id = speaker.getUUID();
        boolean machine = com.waylays.eraphones.server.AnswerMachines.recording(id);
        if (!machine && !com.waylays.eraphones.gear.CamNet.anyRecording()) {
            de.maxhenkel.voicechat.api.opus.OpusDecoder old = machineDecoders.remove(id);
            if (old != null) {
                old.close();
            }
            return;
        }
        try {
            de.maxhenkel.voicechat.api.opus.OpusDecoder dec = machineDecoders.computeIfAbsent(id, k -> api.createDecoder());
            if (data == null || data.length == 0) {
                dec.resetState();
                return;
            }
            short[] pcm = dec.decode(data);
            if (machine) {
                com.waylays.eraphones.server.AnswerMachines.voice(id, pcm);
            }
            com.waylays.eraphones.gear.CamNet.voice(speaker, pcm);
        } catch (Throwable t) {
            if (!machineWarned) {
                machineWarned = true;
                EraPhones.LOGGER.warn("A message could not be recorded: {}", t.toString());
            }
        }
    }

    private boolean machineWarned;

    private boolean gearWarned, lineWarned;
    private volatile GearVoice gear;

}
