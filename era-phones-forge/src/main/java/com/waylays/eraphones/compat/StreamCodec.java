package com.waylays.eraphones.compat;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * What a packet's fields are written as and read back from, as Minecraft 1.21 has it (its StreamCodec), for the
 * Forge 1.20.1 build: the mod's network code reads the same on both.
 */
public interface StreamCodec<B, V> {
    V decode(B buf);

    void encode(B buf, V value);

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.apply(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.accept(buf, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(BiConsumer<V, B> encoder, Function<B, V> decoder) {
        return of((b, v) -> encoder.accept(v, b), decoder);
    }

    static <B, V> StreamCodec<B, V> unit(V value) {
        return of((b, v) -> {}, b -> value);
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        StreamCodec<B, V> self = this;
        return of((b, o) -> self.encode(b, from.apply(o)), b -> to.apply(self.decode(b)));
    }

    default <O> StreamCodec<B, O> apply(CodecOperation<B, V, O> op) {
        return op.apply(this);
    }

    @SuppressWarnings("unchecked")
    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>) this;
    }

    @FunctionalInterface
    interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> codec);
    }

    interface F3<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    interface F4<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    interface F5<A, B, C, D, E, R> {
        R apply(A a, B b, C c, D d, E e);
    }

    interface F6<A, B, C, D, E, F, R> {
        R apply(A a, B b, C c, D d, E e, F f);
    }

    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, Function<T1, C> ctor) {
        return of((b, v) -> c1.encode(b, g1.apply(v)), b -> ctor.apply(c1.decode(b)));
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2,
                                                     Function<C, T2> g2, java.util.function.BiFunction<T1, T2, C> ctor) {
        return of((b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
        }, b -> ctor.apply(c1.decode(b), c2.decode(b)));
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2,
                                                         Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, F3<T1, T2, T3, C> ctor) {
        return of((b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
            c3.encode(b, g3.apply(v));
        }, b -> ctor.apply(c1.decode(b), c2.decode(b), c3.decode(b)));
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2,
                                                             Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                             StreamCodec<? super B, T4> c4, Function<C, T4> g4, F4<T1, T2, T3, T4, C> ctor) {
        return of((b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
            c3.encode(b, g3.apply(v));
            c4.encode(b, g4.apply(v));
        }, b -> ctor.apply(c1.decode(b), c2.decode(b), c3.decode(b), c4.decode(b)));
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2,
                                                                 Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                                 StreamCodec<? super B, T4> c4, Function<C, T4> g4, StreamCodec<? super B, T5> c5,
                                                                 Function<C, T5> g5, F5<T1, T2, T3, T4, T5, C> ctor) {
        return of((b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
            c3.encode(b, g3.apply(v));
            c4.encode(b, g4.apply(v));
            c5.encode(b, g5.apply(v));
        }, b -> ctor.apply(c1.decode(b), c2.decode(b), c3.decode(b), c4.decode(b), c5.decode(b)));
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2,
                                                                     Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                                     StreamCodec<? super B, T4> c4, Function<C, T4> g4, StreamCodec<? super B, T5> c5,
                                                                     Function<C, T5> g5, StreamCodec<? super B, T6> c6, Function<C, T6> g6,
                                                                     F6<T1, T2, T3, T4, T5, T6, C> ctor) {
        return of((b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
            c3.encode(b, g3.apply(v));
            c4.encode(b, g4.apply(v));
            c5.encode(b, g5.apply(v));
            c6.encode(b, g6.apply(v));
        }, b -> ctor.apply(c1.decode(b), c2.decode(b), c3.decode(b), c4.decode(b), c5.decode(b), c6.decode(b)));
    }
}
