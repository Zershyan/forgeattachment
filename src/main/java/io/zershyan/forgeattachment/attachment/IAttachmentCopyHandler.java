package io.zershyan.forgeattachment.attachment;

import net.minecraft.core.HolderLookup;

import javax.annotation.Nullable;

/**
 * 数据附件的自定义复制处理器，用于替代默认的「序列化再反序列化」实现以提升效率。
 */
public interface IAttachmentCopyHandler<T> {
    /**
     * 复制附件，结果应等价于把附件序列化再反序列化。
     *
     * @param attachment 待复制的附件
     * @param holder     复制后附件所属的 holder
     * @return 复制结果，返回 null 表示不应复制
     */
    @Nullable
    T copy(T attachment, IAttachmentHolder holder, HolderLookup.Provider provider);
}