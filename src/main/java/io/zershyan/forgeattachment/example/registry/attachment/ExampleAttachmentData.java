package io.zershyan.forgeattachment.example.registry.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.network.RegistryFriendlyByteBuf;
import io.zershyan.forgeattachment.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 示例附件数据，演示如何同时提供 Codec 与 StreamCodec。
 */
public record ExampleAttachmentData(String note, int value) {
    public static final Codec<ExampleAttachmentData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("note").forGetter(ExampleAttachmentData::note),
            Codec.INT.fieldOf("value").forGetter(ExampleAttachmentData::value)
    ).apply(instance, ExampleAttachmentData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExampleAttachmentData> STREAM_CODEC = StreamCodec.composite(
            StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf),
            ExampleAttachmentData::note,
            StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt),
            ExampleAttachmentData::value,
            ExampleAttachmentData::new
    );

    /**
     * 构建示例附件类型：持久化 + 死亡复制 + 同步。
     */
    public static AttachmentType<ExampleAttachmentData> createType() {
        return AttachmentType.builder(() -> new ExampleAttachmentData("", 0))
                .serialize(CODEC)
                .copyOnDeath()
                .sync(STREAM_CODEC)
                .build();
    }
}