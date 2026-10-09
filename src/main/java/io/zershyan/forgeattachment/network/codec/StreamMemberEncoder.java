package io.zershyan.forgeattachment.network.codec;

import io.netty.buffer.ByteBuf;

/**
 * 以对象自身为第一个参数的编码器。
 *
 * <p>对应 1.21.1 的 {@code net.minecraft.network.codec.StreamMemberEncoder}。
 */
@FunctionalInterface
public interface StreamMemberEncoder<B, V> {
    void encode(V value, B buf);
}