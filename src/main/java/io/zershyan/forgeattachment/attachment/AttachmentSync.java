package io.zershyan.forgeattachment.attachment;

import io.netty.buffer.Unpooled;
import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.network.RegistryFriendlyByteBuf;
import io.zershyan.forgeattachment.registry.ForgeAttachmentPackets;
import io.zershyan.forgeattachment.registry.packet.SyncAttachmentsPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkDirection;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 附件的服务端同步。
 *
 * <p>只同步配置了 {@link AttachmentSyncHandler} 的附件类型。附件类型按注册名传输，
 * 因此两端各自注册同一批附件即可，无需同步注册表本身。
 */
public final class AttachmentSync {
    /**
     * 同步实体上某个附件类型的更新。
     *
     * <p>玩家不在自己的追踪者列表里，需要单独补上。
     */
    public static void syncEntityUpdate(Entity entity, AttachmentType<?> type) {
        ServerLevel level = AttachmentInternals.serverLevelOf(entity);
        if (level == null || type.syncHandler == null) {
            return;
        }
        Collection<ServerPlayer> watchers = level.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false);
        List<ServerPlayer> targets = new ArrayList<>(watchers);
        if (entity instanceof ServerPlayer self && !targets.contains(self)) {
            targets.add(self);
        }
        send(buildPacket(entity, type, targets), targets);
    }

    /**
     * 同步方块实体上某个附件类型的更新。
     */
    public static void syncBlockEntityUpdate(BlockEntity blockEntity, AttachmentType<?> type) {
        ServerLevel level = AttachmentInternals.serverLevelOf(blockEntity);
        if (level == null || type.syncHandler == null) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        Collection<ServerPlayer> watchers = level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false);
        send(buildPacket(blockEntity, type, watchers), watchers);
    }

    /**
     * 把某个附件类型的当前值同步给指定玩家。
     */
    public static void syncToPlayer(ServerPlayer player, AttachmentType<?> type, Object holder) {
        if (type.syncHandler == null) {
            return;
        }
        send(buildPacket(holder, type, List.of(player)), List.of(player));
    }

    /**
     * 把 holder 上所有可同步的附件一次性打包，用于实体开始被追踪、区块被加载等时机。
     */
    @Nullable
    public static SyncAttachmentsPacket buildInitialPacket(Object holder, ServerPlayer player) {
        AttachmentHolder attachmentHolder = AttachmentInternals.holderOf(holder);
        if (attachmentHolder == null) {
            return null;
        }
        Map<AttachmentType<?>, Object> attachments = attachmentHolder.rawAttachments();
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        RegistryAccess registryAccess = player.level().registryAccess();
        List<SyncAttachmentsPacket.Entry> entries = new ArrayList<>();
        for (Map.Entry<AttachmentType<?>, Object> entry : attachments.entrySet()) {
            AttachmentType<?> type = entry.getKey();
            if (!shouldSend(type, attachmentHolder, player)) {
                continue;
            }
            ResourceLocation name = AttachmentInternals.nameOf(type);
            if (name == null) {
                continue;
            }
            byte[] payload = writePayload(type, entry.getValue(), registryAccess, true);
            if (payload == null) {
                continue;
            }
            entries.add(new SyncAttachmentsPacket.Entry(name, payload));
        }
        return entries.isEmpty() ? null : wrap(holder, entries);
    }

    /**
     * 把已构造好的同步包发给指定玩家。
     */
    public static void sendToPlayer(SyncAttachmentsPacket packet, ServerPlayer player) {
        ForgeAttachmentPackets.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static boolean shouldSend(AttachmentType<?> type, AttachmentHolder holder, ServerPlayer player) {
        return type.syncHandler != null && type.syncHandler.sendToPlayer(holder.getExposedHolder(), player);
    }

    @Nullable
    private static SyncAttachmentsPacket buildPacket(Object holder, AttachmentType<?> type, Collection<ServerPlayer> targets) {
        AttachmentHolder attachmentHolder = AttachmentInternals.holderOf(holder);
        if (attachmentHolder == null) {
            return null;
        }
        ServerPlayer receiver = targets.stream().filter(player -> shouldSend(type, attachmentHolder, player)).findFirst().orElse(null);
        if (receiver == null) {
            return null;
        }
        ResourceLocation name = AttachmentInternals.nameOf(type);
        if (name == null) {
            return null;
        }
        Object value = attachmentHolder.getExistingDataOrNull(type);
        byte[] payload = null;
        if (value != null) {
            payload = writePayload(type, value, receiver.level().registryAccess(), false);
            if (payload == null) {
                // 编码失败，不能发空负载——那会被客户端当成删除
                return null;
            }
        }
        return wrap(holder, List.of(new SyncAttachmentsPacket.Entry(name, payload)));
    }

    @Nullable
    private static SyncAttachmentsPacket wrap(Object holder, List<SyncAttachmentsPacket.Entry> entries) {
        if (holder instanceof Entity entity) {
            return SyncAttachmentsPacket.forEntity(entity.getId(), entries);
        }
        if (holder instanceof BlockEntity blockEntity) {
            return SyncAttachmentsPacket.forBlockEntity(blockEntity.getBlockPos(), entries);
        }
        return null;
    }

    private static void send(@Nullable SyncAttachmentsPacket packet, Collection<ServerPlayer> players) {
        if (packet == null) {
            return;
        }
        for (ServerPlayer player : players) {
            sendToPlayer(packet, player);
        }
    }

    /**
     * 用附件自己的流编解码器把值写成字节负载。
     *
     * <p>返回 null 表示编码失败或无同步处理器。调用方**不得**把 null 当作「附件已移除」发出去，
     * 否则一次编码异常就会让客户端把附件删掉。
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static byte[] writePayload(AttachmentType<?> type, Object value, RegistryAccess registryAccess, boolean initialSync) {
        AttachmentSyncHandler<Object> handler = (AttachmentSyncHandler<Object>) type.syncHandler;
        if (handler == null) {
            return null;
        }
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            handler.write(buf, value, initialSync);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } catch (Exception exception) {
            ForgeAttachment.LOGGER.error("Failed to write synced data attachment {}.", AttachmentInternals.nameOf(type), exception);
            return null;
        }
    }

    private AttachmentSync() {}
}