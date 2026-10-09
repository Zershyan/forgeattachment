package io.zershyan.forgeattachment.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Function;

/**
 * 携带注册表访问上下文的 {@link FriendlyByteBuf}。
 *
 * <p>对应 1.21.1 的 {@code net.minecraft.network.RegistryFriendlyByteBuf}，
 * 用于让流编解码器在 1.20.1 上也能访问注册表。
 */
public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    private final RegistryAccess registryAccess;

    public RegistryFriendlyByteBuf(ByteBuf source, RegistryAccess registryAccess) {
        super(source);
        this.registryAccess = registryAccess;
    }

    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess registryAccess) {
        return source -> new RegistryFriendlyByteBuf(source, registryAccess);
    }
}