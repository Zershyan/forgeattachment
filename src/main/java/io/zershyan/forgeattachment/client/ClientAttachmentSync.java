package io.zershyan.forgeattachment.client;

import io.netty.buffer.Unpooled;
import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.attachment.AttachmentHolder;
import io.zershyan.forgeattachment.attachment.AttachmentInternals;
import io.zershyan.forgeattachment.attachment.AttachmentSyncHandler;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.network.RegistryFriendlyByteBuf;
import io.zershyan.forgeattachment.registry.packet.SyncAttachmentsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;

/**
 * 客户端侧的附件同步处理。
 *
 * <p>本类只在客户端加载，服务端不得引用。
 */
public final class ClientAttachmentSync {
    /**
     * 收到同步包后写入本地 holder。
     */
    public static void handle(SyncAttachmentsPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Object holder = switch (packet.target()) {
            case ENTITY -> level.getEntity(packet.entityId());
            case BLOCK_ENTITY -> level.getBlockEntity(packet.blockPos());
        };
        if (holder == null) {
            return;
        }
        AttachmentHolder attachmentHolder = AttachmentInternals.holderOf(holder);
        if (attachmentHolder == null) {
            return;
        }
        for (SyncAttachmentsPacket.Entry entry : packet.entries()) {
            AttachmentType<?> type = AttachmentInternals.byName(entry.name());
            if (type == null || type.syncHandler() == null) {
                ForgeAttachment.LOGGER.error("Received synced data attachment without a registered sync handler: {}", entry.name());
                continue;
            }
            if (entry.payload() == null) {
                attachmentHolder.removeDataSilently(type);
            } else {
                readPayload(type, attachmentHolder, level.registryAccess(), entry.payload());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void readPayload(AttachmentType<?> type, AttachmentHolder holder, RegistryAccess registryAccess, byte[] payload) {
        AttachmentSyncHandler<Object> handler = (AttachmentSyncHandler<Object>) type.syncHandler();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), registryAccess);
        try {
            Object previous = holder.getExistingDataOrNull(type);
            Object value = handler.read(holder.getExposedHolder(), buf, previous);
            if (value == null) {
                holder.removeDataSilently(type);
            } else {
                holder.setDataSilently(type, value);
            }
        } catch (Exception exception) {
            ForgeAttachment.LOGGER.error("Failed to read synced data attachment {}.", AttachmentInternals.nameOf(type), exception);
        }
    }

    private ClientAttachmentSync() {}
}