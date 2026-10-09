package io.zershyan.forgeattachment.network.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import io.netty.buffer.ByteBuf;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * 同时具备编解码能力的流编解码器。
 *
 * <p>对应 1.21.1 的 {@code net.minecraft.network.codec.StreamCodec}，
 * 供 1.20.1 上的附件同步使用。
 */
public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B, V> StreamCodec<B, V> of(StreamEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.encode(buf, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(StreamMemberEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.encode(value, buf);
            }
        };
    }

    static <B, V> StreamCodec<B, V> unit(V defaultValue) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return defaultValue;
            }

            @Override
            public void encode(B buf, V value) {
                if (!value.equals(defaultValue)) {
                    throw new IllegalStateException("Can't encode '" + value + "', expected '" + defaultValue + "'");
                }
            }
        };
    }

    default <O> StreamCodec<B, O> apply(CodecOperation<B, V, O> operation) {
        return operation.apply(this);
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        return new StreamCodec<>() {
            @Override
            public O decode(B buf) {
                return to.apply(StreamCodec.this.decode(buf));
            }

            @Override
            public void encode(B buf, O value) {
                StreamCodec.this.encode(buf, from.apply(value));
            }
        };
    }

    default <O extends ByteBuf> StreamCodec<O, V> mapStream(Function<O, ? extends B> op) {
        return new StreamCodec<>() {
            @Override
            public V decode(O buf) {
                return StreamCodec.this.decode(op.apply(buf));
            }

            @Override
            public void encode(O buf, V value) {
                StreamCodec.this.encode(op.apply(buf), value);
            }
        };
    }

    default <U> StreamCodec<B, U> dispatch(Function<? super U, ? extends V> keyGetter,
                                           Function<? super V, ? extends StreamCodec<? super B, ? extends U>> codecGetter) {
        return new StreamCodec<>() {
            @Override
            public U decode(B buf) {
                V key = StreamCodec.this.decode(buf);
                return codecGetter.apply(key).decode(buf);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void encode(B buf, U value) {
                V key = keyGetter.apply(value);
                StreamCodec.this.encode(buf, key);
                ((StreamCodec<B, U>) codecGetter.apply(key)).encode(buf, value);
            }
        };
    }

    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1,
                                                  Function<C, T1> g1,
                                                  Function<T1, C> ctor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                return ctor.apply(c1.decode(buf));
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
            }
        };
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
                                                      StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                      BiFunction<T1, T2, C> ctor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                return ctor.apply(c1.decode(buf), c2.decode(buf));
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
                c2.encode(buf, g2.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
                                                          StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                          StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                          Function3<T1, T2, T3, C> ctor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                return ctor.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf));
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
                c2.encode(buf, g2.apply(value));
                c3.encode(buf, g3.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
                                                              StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                              StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                              StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                              Function4<T1, T2, T3, T4, C> ctor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                return ctor.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf), c4.decode(buf));
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
                c2.encode(buf, g2.apply(value));
                c3.encode(buf, g3.apply(value));
                c4.encode(buf, g4.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
                                                                  StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                                  StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                                  StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                                  StreamCodec<? super B, T5> c5, Function<C, T5> g5,
                                                                  Function5<T1, T2, T3, T4, T5, C> ctor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                return ctor.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf), c4.decode(buf), c5.decode(buf));
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
                c2.encode(buf, g2.apply(value));
                c3.encode(buf, g3.apply(value));
                c4.encode(buf, g4.apply(value));
                c5.encode(buf, g5.apply(value));
            }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1,
                                                                      StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                                      StreamCodec<? super B, T3> c3, Function<C, T3> g3,
                                                                      StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                                      StreamCodec<? super B, T5> c5, Function<C, T5> g5,
                                                                      StreamCodec<? super B, T6> c6, Function<C, T6> g6,
                                                                      Function6<T1, T2, T3, T4, T5, T6, C> ctor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                return ctor.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf), c4.decode(buf), c5.decode(buf), c6.decode(buf));
            }

            @Override
            public void encode(B buf, C value) {
                c1.encode(buf, g1.apply(value));
                c2.encode(buf, g2.apply(value));
                c3.encode(buf, g3.apply(value));
                c4.encode(buf, g4.apply(value));
                c5.encode(buf, g5.apply(value));
                c6.encode(buf, g6.apply(value));
            }
        };
    }

    static <B, T> StreamCodec<B, T> recursive(UnaryOperator<StreamCodec<B, T>> factory) {
        return new StreamCodec<>() {
            private final Supplier<StreamCodec<B, T>> inner = Suppliers.memoize(() -> factory.apply(this));

            @Override
            public T decode(B buf) {
                return this.inner.get().decode(buf);
            }

            @Override
            public void encode(B buf, T value) {
                this.inner.get().encode(buf, value);
            }
        };
    }

    @SuppressWarnings("unchecked")
    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>) this;
    }

    @FunctionalInterface
    interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> codec);
    }
}