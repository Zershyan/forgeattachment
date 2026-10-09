package io.zershyan.forgeattachment.handler.common;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.attachment.AttachmentSync;
import io.zershyan.forgeattachment.registry.packet.SyncAttachmentsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 在对象首次被客户端收到时补发初始附件数据。
 */
@Mod.EventBusSubscriber(modid = ForgeAttachment.MODID)
public final class AttachmentSyncEvents {
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SyncAttachmentsPacket packet = AttachmentSync.buildInitialPacket(event.getTarget(), player);
        if (packet != null) {
            AttachmentSync.sendToPlayer(packet, player);
        }
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerPlayer player = event.getPlayer();
        for (var blockEntity : event.getChunk().getBlockEntities().values()) {
            SyncAttachmentsPacket packet = AttachmentSync.buildInitialPacket(blockEntity, player);
            if (packet != null) {
                AttachmentSync.sendToPlayer(packet, player);
            }
        }
    }

    private AttachmentSyncEvents() {}
}