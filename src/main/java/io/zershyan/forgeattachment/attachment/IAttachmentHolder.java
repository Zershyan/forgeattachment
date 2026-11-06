package io.zershyan.forgeattachment.attachment;

import javax.annotation.Nullable;

/**
 * 可挂载附件的对象（实体、方块实体）。
 *
 * <p>1.12.2 没有 NeoForge 的 attachment 概念，由 Mixin 把本接口注入到
 * {@code Entity} 与 {@code TileEntity}，因此任何实体与方块实体都天然是 holder。
 */
public interface IAttachmentHolder {

    /**
     * 读取附件，不存在时写入默认值后返回。
     */
    @Nullable
    <T> T getData(AttachmentType<T> type);

    /**
     * 写入附件，返回此前的值（原本不存在时为 null）。
     */
    @Nullable
    <T> T setData(AttachmentType<T> type, T data);

    /**
     * 移除附件，返回此前的值。
     */
    @Nullable
    <T> T removeData(AttachmentType<T> type);

    /**
     * 该附件是否已显式写入过（不触发默认值）。
     */
    <T> boolean hasData(AttachmentType<T> type);
}
