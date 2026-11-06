package io.zershyan.forgeattachment.attachment;

import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

/**
 * 附件的磁盘序列化器。
 *
 * <p>1.12.2 没有 DataFixerUpper 的 {@code Codec}，读写统一用 {@link NBTTagCompound}。
 */
public interface IAttachmentSerializer<T> {

    /**
     * 写入 NBT，返回 null 表示该值不持久化。
     */
    @Nullable
    NBTTagCompound write(T attachment);

    /**
     * 从 NBT 还原。
     */
    T read(NBTTagCompound tag);
}
