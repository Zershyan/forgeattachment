package io.zershyan.forgeattachment.example;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.attachment.AttachmentRegistry;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.attachment.IAttachmentSerializer;
import io.zershyan.forgeattachment.attachment.IAttachmentSyncCodec;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

/**
 * 示例附件：演示持久化、同步与死亡复制三件套。
 */
public final class ExampleAttachments {

    /**
     * 挂在实体上的备注字符串。
     */
    public static final AttachmentType<String> NOTE = AttachmentRegistry.register(
            new ResourceLocation(ForgeAttachment.MODID, "note"),
            AttachmentType.<String>builder(() -> "")
                    .serialize(new IAttachmentSerializer<String>() {
                        @Override
                        public NBTTagCompound write(String attachment) {
                            NBTTagCompound tag = new NBTTagCompound();
                            tag.setString("value", attachment);
                            return tag;
                        }

                        @Override
                        public String read(NBTTagCompound tag) {
                            return tag.getString("value");
                        }
                    })
                    .sync(new IAttachmentSyncCodec<String>() {
                        @Override
                        public void write(PacketBuffer buf, String attachment) {
                            buf.writeString(attachment);
                        }

                        @Override
                        public String read(PacketBuffer buf) {
                            return buf.readString(32767);
                        }
                    })
                    .copyOnDeath()
                    .build());

    private ExampleAttachments() {
    }

    public static void init() {
        // 触发类加载即完成注册
    }
}
