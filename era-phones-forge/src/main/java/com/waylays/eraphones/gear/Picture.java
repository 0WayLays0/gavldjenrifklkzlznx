package com.waylays.eraphones.gear;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import com.waylays.eraphones.compat.ByteBufCodecs;
import com.waylays.eraphones.compat.StreamCodec;

import java.util.Arrays;

/**
 * A photograph's picture, squeezed, as an item keeps it. Items' data must compare by value (a bare array of bytes is
 * refused), so the bytes are wrapped here; they are never changed once made.
 */
public record Picture(byte[] data) {
    public static final Codec<Picture> CODEC = Codec.BYTE_BUFFER.xmap(b -> {
        byte[] a = new byte[b.remaining()];
        b.duplicate().get(a);
        return new Picture(a);
    }, p -> java.nio.ByteBuffer.wrap(p.data));
    public static final StreamCodec<ByteBuf, Picture> STREAM_CODEC = ByteBufCodecs.BYTE_ARRAY.map(Picture::new, Picture::data);

    @Override
    public boolean equals(Object o) {
        return o instanceof Picture p && Arrays.equals(data, p.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return "Picture[" + data.length + " bytes]";
    }
}
