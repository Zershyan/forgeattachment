package io.zershyan.forgeattachment.attachment;

import io.zershyan.forgeattachment.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * 数据附件的同步处理器。
 */
public interface AttachmentSyncHandler<T> {
    /**
     * 判断是否应把该附件同步给指定玩家。
     *
     * @param holder 附件所属的 holder
     * @param to     接收方玩家
     */
    boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to);

    /**
     * 把附件写入缓冲区。
     *
     * @param initialSync 是否为该玩家的首次同步
     */
    void write(RegistryFriendlyByteBuf buf, T attachment, boolean initialSync);

    /**
     * 从缓冲区读取附件。
     *
     * @param previousValue 该 holder 上此前的附件值，可能为 null
     */
    @Nullable
    T read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable T previousValue);
}