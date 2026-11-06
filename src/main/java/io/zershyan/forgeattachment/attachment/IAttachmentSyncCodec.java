package io.zershyan.forgeattachment.attachment;

import net.minecraft.network.PacketBuffer;

/**
 * 附件的网络序列化器，用于把附件同步给客户端。
 */
public interface IAttachmentSyncCodec<T> {

    void write(PacketBuffer buf, T attachment);

    T read(PacketBuffer buf);
}
