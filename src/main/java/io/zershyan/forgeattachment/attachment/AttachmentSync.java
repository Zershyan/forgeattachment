package io.zershyan.forgeattachment.attachment;

import io.zershyan.forgeattachment.network.ForgeAttachmentNetwork;
import io.zershyan.forgeattachment.network.MessageSyncAttachment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * 把附件同步给正在追踪该实体的客户端。
 */
public final class AttachmentSync {

    private AttachmentSync() {
    }

    /**
     * 单条附件同步；调用方需保证在逻辑服务端。
     */
    public static void send(Entity entity, AttachmentType<?> type, Object value) {
        if (entity.world.isRemote || type.syncCodec() == null)
            return;
        MessageSyncAttachment message = MessageSyncAttachment.of(entity, type, value);
        if (message != null)
            ForgeAttachmentNetwork.CHANNEL.sendToAllTracking(message, entity);
    }

    /**
     * 客户端开始追踪实体时补发全部同步附件。
     */
    public static void sendAll(EntityPlayer player, Entity entity) {
        if (entity.world.isRemote || !(player instanceof EntityPlayerMP) || !(entity instanceof IAttachmentHolder))
            return;
        EntityPlayerMP receiver = (EntityPlayerMP) player;
        IAttachmentHolder holder = (IAttachmentHolder) entity;
        for (AttachmentType<?> type : AttachmentRegistry.all()) {
            if (type.syncCodec() == null || !holder.hasData(type))
                continue;
            MessageSyncAttachment message = MessageSyncAttachment.of(entity, type, valueOf(holder, type));
            if (message != null)
                ForgeAttachmentNetwork.CHANNEL.sendTo(message, receiver);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Object valueOf(IAttachmentHolder holder, AttachmentType<?> type) {
        return holder.getData((AttachmentType<T>) type);
    }
}
