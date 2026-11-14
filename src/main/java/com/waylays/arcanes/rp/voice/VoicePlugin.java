package com.waylays.arcanes.rp.voice;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.RpVisibility;
import com.waylays.arcanes.rp.horror.Voices;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * With Simple Voice Chat (only loaded if it is installed): a voice put on with /voix (the masked caller, a demon, a
 * clown...) replaces the speaker's own before anyone hears it, Époque RP's phones and radios included (it is done
 * first); and the voice of someone seen by a few players only (/voir) reaches only them (that is done last, once the
 * others have taken the voice where they send it).
 */
@ForgeVoicechatPlugin
public class VoicePlugin implements VoicechatPlugin {
    private static final int FIRST = 1000, LAST = -1000;

    private boolean warned;

    @Override
    public String getPluginId() {
        return ArcanesOublies.MODID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::changeVoice, FIRST);
        registration.registerEvent(MicrophonePacketEvent.class, this::hide, LAST);
        registration.registerEvent(de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent.class, e -> VoiceStreams.forget(e.getPlayerUuid()));
    }

    /** The speaker's voice put on before anyone hears it. */
    private void changeVoice(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || !(sender.getPlayer().getPlayer() instanceof ServerPlayer speaker)) {
            return;
        }
        VoicechatServerApi api = event.getVoicechat();
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
                ArcanesOublies.LOGGER.warn("The voice {} could not be put on: {}", voice, t.toString());
            }
        }
    }

    /** Someone seen by a few players only is heard by them only. */
    private void hide(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || !(sender.getPlayer().getPlayer() instanceof ServerPlayer speaker) || !RpVisibility.hidden(speaker)) {
            return;
        }
        VoicechatServerApi api = event.getVoicechat();
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
}
