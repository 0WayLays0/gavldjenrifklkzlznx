package com.waylays.eraphones.voice;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.horror.Voices;
import com.waylays.eraphones.rp.RpConfig;
import com.waylays.eraphones.rp.RpVisibility;
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
 * With Simple Voice Chat (only loaded if it is installed): a voice put on with /voix (the masked caller, a demon, a
 * clown...) replaces the speaker's own, for all who hear them; the voice of someone seen by one player only (/voir)
 * reaches only those who see them; and the two ends of a phone call hear each other wherever they are, as through the
 * phone, while those standing near each still hear them speak.
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
        });
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || !(sender.getPlayer().getPlayer() instanceof ServerPlayer speaker)) {
            return;
        }
        VoicechatServerApi api = event.getVoicechat();
        changeVoice(event, sender, speaker, api);
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
        if (RpVisibility.hidden(speaker)) {
            // Heard only by those who see them
            event.cancel();
            double range = api.getVoiceChatDistance();
            for (ServerPlayer listener : RpVisibility.viewers(speaker)) {
                if (listener.level() != speaker.level() || listener.distanceTo(speaker) > range) {
                    continue;
                }
                VoicechatConnection c = api.getConnectionOf(listener.getUUID());
                if (c != null) {
                    api.sendEntitySoundPacketTo(c, event.getPacket().toEntitySoundPacket(speaker.getUUID(), event.getPacket().isWhispering()));
                }
            }
        }
        if (phoneVoice()) {
            UUID other = CallService.otherEnd(speaker.getUUID());
            if (other != null) {
                VoicechatConnection c = api.getConnectionOf(other);
                if (c != null) {
                    api.sendStaticSoundPacketTo(c, event.getPacket().toStaticSoundPacket());
                }
            }
        }
    }

    /** The speaker's voice put on before anyone hears it (so also through /voir and the phone). */
    private void changeVoice(MicrophonePacketEvent event, VoicechatConnection sender, ServerPlayer speaker, VoicechatServerApi api) {
        String voice = Voices.of(speaker.getUUID());
        try {
            byte[] changed = VoiceStreams.process(api, speaker.getUUID(), voice, event.getPacket().getOpusEncodedData());
            if (changed == null) {
                return;
            }
            event.getPacket().setOpusEncodedData(changed);
            if (Voices.listening(speaker.getUUID())) {
                api.sendStaticSoundPacketTo(sender, event.getPacket().toStaticSoundPacket());
            }
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                EraPhones.LOGGER.warn("The voice {} could not be put on: {}", voice, t.toString());
            }
        }
    }

    private boolean warned, gearWarned;
    private volatile GearVoice gear;

    private static boolean phoneVoice() {
        try {
            return RpConfig.PHONE_VOICE.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
