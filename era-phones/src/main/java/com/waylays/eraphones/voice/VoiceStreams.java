package com.waylays.eraphones.voice;

import com.waylays.eraphones.horror.VoiceFx;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The changed voices, speaker by speaker: each frame of the microphone decoded, worked by the speaker's voice (which
 * keeps its state from one frame to the next), and encoded again.
 */
final class VoiceStreams {
    private static final Map<UUID, Stream> STREAMS = new ConcurrentHashMap<>();
    /** The voices as the other end of a phone call hears them, through the line. */
    private static final Map<UUID, Stream> LINES = new ConcurrentHashMap<>();

    private VoiceStreams() {}

    private static final class Stream {
        final String voice;
        final OpusDecoder decoder;
        final OpusEncoder encoder;
        final VoiceFx.Chain chain;

        Stream(String voice, OpusDecoder decoder, OpusEncoder encoder, VoiceFx.Chain chain) {
            this.voice = voice;
            this.decoder = decoder;
            this.encoder = encoder;
            this.chain = chain;
        }

        void close() {
            decoder.close();
            encoder.close();
        }
    }

    /**
     * The frame {@code data} of {@code speaker}, in the voice {@code voice}; null when it is to go as it is (their own
     * voice, or the empty frame that ends a sentence).
     */
    @Nullable
    static byte[] process(VoicechatApi api, UUID speaker, @Nullable String voice, byte[] data) {
        voice = voice == null ? null : voice + "@" + com.waylays.eraphones.horror.Voices.strength(speaker);
        Stream s = STREAMS.get(speaker);
        if (voice == null) {
            if (s != null) {
                STREAMS.remove(speaker);
                s.close();
            }
            return null;
        }
        if (s == null || !s.voice.equals(voice)) {
            int at = voice.indexOf('@');
            VoiceFx.Chain chain = VoiceFx.chain(voice.substring(0, at), Integer.parseInt(voice.substring(at + 1)));
            if (chain == null) {
                return null;
            }
            if (s != null) {
                s.close();
            }
            s = new Stream(voice, api.createDecoder(), api.createEncoder(), chain);
            STREAMS.put(speaker, s);
        }
        synchronized (s) {
            if (data == null || data.length == 0) {
                s.decoder.resetState();
                s.encoder.resetState();
                return null;
            }
            short[] pcm = s.decoder.decode(data);
            if (pcm == null || pcm.length == 0) {
                return null;
            }
            return s.encoder.encode(s.chain.process(pcm));
        }
    }

    /**
     * The frame {@code data} of {@code speaker} as heard through a phone of the time {@code era} (its line, its
     * ear-piece); null when it is to go as it is.
     */
    @Nullable
    static byte[] line(VoicechatApi api, UUID speaker, String era, byte[] data) {
        String voice = "telephone_" + era;
        Stream s = LINES.get(speaker);
        if (s == null || !s.voice.equals(voice)) {
            VoiceFx.Chain chain = VoiceFx.chain(voice);
            if (chain == null) {
                return null;
            }
            if (s != null) {
                s.close();
            }
            s = new Stream(voice, api.createDecoder(), api.createEncoder(), chain);
            LINES.put(speaker, s);
        }
        synchronized (s) {
            if (data == null || data.length == 0) {
                s.decoder.resetState();
                s.encoder.resetState();
                return null;
            }
            short[] pcm = s.decoder.decode(data);
            if (pcm == null || pcm.length == 0) {
                return null;
            }
            return s.encoder.encode(s.chain.process(pcm));
        }
    }

    /** Forgets a speaker's streams (they left). */
    static void forget(UUID speaker) {
        for (Map<UUID, Stream> m : java.util.List.of(STREAMS, LINES)) {
            Stream s = m.remove(speaker);
            if (s != null) {
                s.close();
            }
        }
    }
}
