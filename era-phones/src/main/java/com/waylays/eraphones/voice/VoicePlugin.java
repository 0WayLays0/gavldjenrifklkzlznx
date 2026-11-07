package com.waylays.eraphones.voice;

import com.waylays.eraphones.EraPhones;
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
 * With Simple Voice Chat (only loaded if it is installed): the voice of someone seen by one player only (/voir) reaches
 * only those who see them; and the two ends of a phone call hear each other wherever they are, as through the phone,
 * while those standing near each still hear them speak.
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
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || !(sender.getPlayer().getPlayer() instanceof ServerPlayer speaker)) {
            return;
        }
        VoicechatServerApi api = event.getVoicechat();
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

    private static boolean phoneVoice() {
        try {
            return RpConfig.PHONE_VOICE.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
