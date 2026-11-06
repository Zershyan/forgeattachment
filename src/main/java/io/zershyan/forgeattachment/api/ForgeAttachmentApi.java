package io.zershyan.forgeattachment.api;

import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/**
 * 对外门面：为下游 mod 提供附件读写入口。
 *
 * <p>任意实体与方块实体都支持附件（由 Mixin 注入 {@link IAttachmentHolder}）。
 */
public final class ForgeAttachmentApi {

    private ForgeAttachmentApi() {
    }

    public static boolean supports(Object holder) {
        return holder instanceof IAttachmentHolder;
    }

    /**
     * 读取实体上的附件，不存在时写入默认值。
     */
    @Nullable
    public static <T> T getData(Entity entity, AttachmentType<T> type) {
        return entity instanceof IAttachmentHolder ? ((IAttachmentHolder) entity).getData(type) : null;
    }

    @Nullable
    public static <T> T getData(TileEntity tileEntity, AttachmentType<T> type) {
        return tileEntity instanceof IAttachmentHolder ? ((IAttachmentHolder) tileEntity).getData(type) : null;
    }

    /**
     * 写入实体上的附件，返回此前的值。
     */
    @Nullable
    public static <T> T setData(Entity entity, AttachmentType<T> type, T data) {
        return entity instanceof IAttachmentHolder ? ((IAttachmentHolder) entity).setData(type, data) : null;
    }

    @Nullable
    public static <T> T setData(TileEntity tileEntity, AttachmentType<T> type, T data) {
        return tileEntity instanceof IAttachmentHolder ? ((IAttachmentHolder) tileEntity).setData(type, data) : null;
    }

    @Nullable
    public static <T> T removeData(Entity entity, AttachmentType<T> type) {
        return entity instanceof IAttachmentHolder ? ((IAttachmentHolder) entity).removeData(type) : null;
    }

    @Nullable
    public static <T> T removeData(TileEntity tileEntity, AttachmentType<T> type) {
        return tileEntity instanceof IAttachmentHolder ? ((IAttachmentHolder) tileEntity).removeData(type) : null;
    }

    public static <T> boolean hasData(Entity entity, AttachmentType<T> type) {
        return entity instanceof IAttachmentHolder && ((IAttachmentHolder) entity).hasData(type);
    }

    public static <T> boolean hasData(TileEntity tileEntity, AttachmentType<T> type) {
        return tileEntity instanceof IAttachmentHolder && ((IAttachmentHolder) tileEntity).hasData(type);
    }
}
