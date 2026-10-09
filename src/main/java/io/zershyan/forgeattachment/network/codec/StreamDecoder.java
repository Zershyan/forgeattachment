package io.zershyan.forgeattachment.network.codec;

import io.netty.buffer.ByteBuf;

/**
 * 把对象从缓冲区解码。
 *
 * <p>对应 1.21.1 的 {@code net.minecraft.network.codec.StreamDecoder}。
 */
@FunctionalInterface
public interface StreamDecoder<B, V> {
    V decode(B buf);
}