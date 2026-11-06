package io.zershyan.forgeattachment.network;

import io.zershyan.forgeattachment.attachment.AttachmentHolder;
import io.zershyan.forgeattachment.attachment.AttachmentRegistry;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import io.zershyan.forgeattachment.attachment.IAttachmentSyncCodec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

/**
 * 服务端 → 客户端：把某个实体上的一个附件同步过去。
 */
public class MessageSyncAttachment implements IMessage {

    private int entityId;
    private ResourceLocation typeName;
    private NBTTagCompound payload;

    public MessageSyncAttachment() {
    }

    private MessageSyncAttachment(int entityId, ResourceLocation typeName, NBTTagCompound payload) {
        this.entityId = entityId;
        this.typeName = typeName;
        this.payload = payload;
    }

    /**
     * 用附件的同步编解码器把值写成 NBT 中转；没有同步编解码器时返回 null。
     */
    @Nullable
    public static MessageSyncAttachment of(Entity entity, AttachmentType<?> type, Object value) {
        IAttachmentSyncCodec<?> codec = type.syncCodec();
        if (codec == null || type.getRegistryName() == null)
            return null;
        PacketBuffer buffer = new PacketBuffer(io.netty.buffer.Unpooled.buffer());
        writeValue(codec, buffer, value);
        NBTTagCompound wrapper = new NBTTagCompound();
        wrapper.setByteArray("data", buffer.array());
        return new MessageSyncAttachment(entity.getEntityId(), type.getRegistryName(), wrapper);
    }

    @SuppressWarnings("unchecked")
    private static <T> void writeValue(IAttachmentSyncCodec<T> codec, PacketBuffer buffer, Object value) {
        codec.write(buffer, (T) value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readValue(IAttachmentSyncCodec<T> codec, PacketBuffer buffer) {
        return codec.read(buffer);
    }

    @Override
    public void fromBytes(io.netty.buffer.ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.entityId = buffer.readVarInt();
        this.typeName = new ResourceLocation(buffer.readString(256));
        try {
            this.payload = buffer.readCompoundTag();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read attachment payload", e);
        }
    }

    @Override
    public void toBytes(io.netty.buffer.ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeVarInt(this.entityId);
        buffer.writeString(this.typeName.toString());
        buffer.writeCompoundTag(this.payload);
    }

    public static class Handler implements IMessageHandler<MessageSyncAttachment, IMessage> {

        @Override
        public IMessage onMessage(MessageSyncAttachment message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> apply(message));
            return null;
        }

        private void apply(MessageSyncAttachment message) {
            net.minecraft.client.multiplayer.WorldClient world = net.minecraft.client.Minecraft.getMinecraft().world;
            if (world == null)
                return;
            Entity entity = world.getEntityByID(message.entityId);
            if (!(entity instanceof IAttachmentHolder))
                return;
            AttachmentType<?> type = AttachmentRegistry.get(message.typeName);
            if (type == null || type.syncCodec() == null)
                return;
            PacketBuffer buffer = new PacketBuffer(io.netty.buffer.Unpooled.wrappedBuffer(message.payload.getByteArray("data")));
            Object value = readValue(type.syncCodec(), buffer);
            ((IAttachmentHolder) entity).setData(unchecked(type), value);
        }

        @SuppressWarnings("unchecked")
        private static <T> AttachmentType<T> unchecked(AttachmentType<?> type) {
            return (AttachmentType<T>) type;
        }
    }
}
