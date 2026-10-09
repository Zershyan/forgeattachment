package io.zershyan.forgeattachment.api;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * 对外稳定门面：为下游 mod 提供附件相关的统一入口。
 *
 * <p>下游 mod 注册 {@link AttachmentType} 后，可通过本类取得任意实体或方块实体上的附件。
 */
public final class ForgeAttachmentApi {
    public static final String MODID = ForgeAttachment.MODID;

    /**
     * 判断某对象是否支持附件。
     *
     * @param holder 实体或方块实体
     */
    public static boolean supports(Object holder) {
        return holder instanceof IAttachmentHolder;
    }

    /**
     * 以附件视角包装一个实体。
     *
     * @param entity 目标实体
     * @return 附件 holder，实体不支持附件时为空
     */
    public static Optional<IAttachmentHolder> of(Entity entity) {
        return entity instanceof IAttachmentHolder holder ? Optional.of(holder) : Optional.empty();
    }

    /**
     * 以附件视角包装一个方块实体。
     *
     * @param blockEntity 目标方块实体
     * @return 附件 holder，方块实体不支持附件时为空
     */
    public static Optional<IAttachmentHolder> of(BlockEntity blockEntity) {
        return blockEntity instanceof IAttachmentHolder holder ? Optional.of(holder) : Optional.empty();
    }

    /**
     * 读取实体上的附件，不存在时存入默认值。
     */
    @Nullable
    public static <T> T getData(Entity entity, AttachmentType<T> type) {
        return entity instanceof IAttachmentHolder holder ? holder.getData(type) : null;
    }

    /**
     * 读取方块实体上的附件，不存在时存入默认值。
     */
    @Nullable
    public static <T> T getData(BlockEntity blockEntity, AttachmentType<T> type) {
        return blockEntity instanceof IAttachmentHolder holder ? holder.getData(type) : null;
    }

    /**
     * 设置实体上的附件。
     *
     * @return 该附件此前的值，若原本不存在则为 null
     */
    @Nullable
    public static <T> T setData(Entity entity, AttachmentType<T> type, T data) {
        return entity instanceof IAttachmentHolder holder ? holder.setData(type, data) : null;
    }

    /**
     * 设置方块实体上的附件。
     *
     * @return 该附件此前的值，若原本不存在则为 null
     */
    @Nullable
    public static <T> T setData(BlockEntity blockEntity, AttachmentType<T> type, T data) {
        return blockEntity instanceof IAttachmentHolder holder ? holder.setData(type, data) : null;
    }

    private ForgeAttachmentApi() {}
}