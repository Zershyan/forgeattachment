package io.zershyan.forgeattachment.attachment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

/**
 * 数据附件的序列化器。
 *
 * @param <S> 序列化后的 {@link Tag} 子类
 * @param <T> 数据附件的类型
 */
public interface IAttachmentSerializer<S extends Tag, T> {
    /**
     * 从 NBT 读取附件。
     *
     * @param holder   附件的 holder，已知子类型时可强转
     * @param tag      序列化后的附件数据
     * @param provider 注册表查询上下文
     */
    T read(IAttachmentHolder holder, S tag, HolderLookup.Provider provider);

    /**
     * 把附件写入 NBT，返回 null 表示不应序列化。
     */
    @Nullable
    S write(T attachment, HolderLookup.Provider provider);
}