package io.zershyan.forgeattachment.registry.packet;

import io.zershyan.forgeattachment.client.ClientAttachmentSync;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 把某个 holder 上的附件值同步给客户端。
 *
 * <p>附件类型按注册名传输，数据本体由各附件自己的流编解码器解释。
 * 负载为 null 表示该附件已被移除。
 */
public record SyncAttachmentsPacket(Target target, int entityId, @Nullable BlockPos blockPos, List<Entry> entries) {
    /** 同步目标种类。 */
    public enum Target {
        ENTITY,
        BLOCK_ENTITY
    }

    /** 单个附件的同步内容，负载为 null 表示该附件已被移除。 */
    public record Entry(ResourceLocation name, @Nullable byte[] payload) {}

    public static SyncAttachmentsPacket forEntity(int entityId, List<Entry> entries) {
        return new SyncAttachmentsPacket(Target.ENTITY, entityId, null, entries);
    }

    public static SyncAttachmentsPacket forBlockEntity(BlockPos blockPos, List<Entry> entries) {
        return new SyncAttachmentsPacket(Target.BLOCK_ENTITY, -1, blockPos, entries);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(this.target.ordinal());
        if (this.target == Target.ENTITY) {
            buf.writeVarInt(this.entityId);
        } else {
            buf.writeBlockPos(this.blockPos);
        }
        buf.writeVarInt(this.entries.size());
        for (Entry entry : this.entries) {
            buf.writeResourceLocation(entry.name());
            buf.writeBoolean(entry.payload() != null);
            if (entry.payload() != null) {
                buf.writeByteArray(entry.payload());
            }
        }
    }

    public static SyncAttachmentsPacket decode(FriendlyByteBuf buf) {
        Target target = Target.values()[buf.readByte()];
        int entityId = -1;
        BlockPos blockPos = null;
        if (target == Target.ENTITY) {
            entityId = buf.readVarInt();
        } else {
            blockPos = buf.readBlockPos();
        }
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation name = buf.readResourceLocation();
            byte[] payload = buf.readBoolean() ? buf.readByteArray() : null;
            entries.add(new Entry(name, payload));
        }
        return new SyncAttachmentsPacket(target, entityId, blockPos, entries);
    }

    public static void handle(SyncAttachmentsPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAttachmentSync.handle(packet));
        ctx.setPacketHandled(true);
    }
}