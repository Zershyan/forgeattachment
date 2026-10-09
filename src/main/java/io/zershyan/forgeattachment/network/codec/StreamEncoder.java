package io.zershyan.forgeattachment.network.codec;

import io.netty.buffer.ByteBuf;

/**
 * 把对象编码进缓冲区。
 *
 * <p>对应 1.21.1 的 {@code net.minecraft.network.codec.StreamEncoder}。
 */
@FunctionalInterface
public interface StreamEncoder<B, V> {
    void encode(B buf, V value);
}