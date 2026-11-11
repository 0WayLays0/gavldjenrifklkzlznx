package com.waylays.eraphones.compat;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** The usual fields' codecs, as Minecraft 1.21 names them, over a 1.20.1 FriendlyByteBuf. */
public final class ByteBufCodecs {
    private ByteBufCodecs() {}

    static FriendlyByteBuf f(ByteBuf b) {
        return b instanceof FriendlyByteBuf fb ? fb : new FriendlyByteBuf(b);
    }

    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of((b, v) -> b.writeBoolean(v), ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of((b, v) -> f(b).writeVarInt(v), b -> f(b).readVarInt());
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of((b, v) -> f(b).writeVarLong(v), b -> f(b).readVarLong());
    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of((b, v) -> b.writeInt(v), ByteBuf::readInt);
    public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of((b, v) -> b.writeFloat(v), ByteBuf::readFloat);
    public static final StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of((b, v) -> b.writeDouble(v), ByteBuf::readDouble);
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
    public static final StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = StreamCodec.of((b, v) -> f(b).writeByteArray(v), b -> f(b).readByteArray());

    public static StreamCodec<ByteBuf, String> stringUtf8(int max) {
        return StreamCodec.of((b, v) -> f(b).writeUtf(v, max), b -> f(b).readUtf(max));
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int max) {
        return codec -> StreamCodec.of((b, list) -> {
            f(b).writeVarInt(list.size());
            for (V v : list) {
                codec.encode(b, v);
            }
        }, b -> {
            int n = f(b).readVarInt();
            if (n > max) {
                throw new io.netty.handler.codec.DecoderException(n + " elements, at most " + max);
            }
            List<V> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                out.add(codec.decode(b));
            }
            return out;
        });
    }
}
