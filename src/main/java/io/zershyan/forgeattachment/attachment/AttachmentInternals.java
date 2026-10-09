package io.zershyan.forgeattachment.attachment;

import io.zershyan.forgeattachment.registry.ForgeAttachmentRegistries;
import io.zershyan.forgeattachment.util.mixin.IMixinBlockEntity;
import io.zershyan.forgeattachment.util.mixin.IMixinEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * 附件机制的内部支撑：复制与注册表查询。
 */
public final class AttachmentInternals {
    /**
     * 把 {@code from} 上的附件复制到 {@code to}。
     *
     * @param isDeath true 时只复制标记了 copyOnDeath 的附件
     */
    public static void copyAttachments(HolderLookup.Provider provider, Object from, Object to, boolean isDeath) {
        AttachmentHolder source = holderOf(from);
        AttachmentHolder target = holderOf(to);
        if (source == null || target == null || source == target) {
            return;
        }
        source.copyAttachmentsTo(target, provider, isDeath);
    }

    /**
     * 取得对象背后的附件存储，对象不支持附件时返回 null。
     */
    @Nullable
    public static AttachmentHolder holderOf(Object object) {
        if (object instanceof IMixinEntity mixin) {
            return mixin.forgeattachment$attachmentHolder();
        }
        if (object instanceof IMixinBlockEntity mixin) {
            return mixin.forgeattachment$attachmentHolder();
        }
        return null;
    }

    /**
     * 根据资源位置查找附件类型。
     */
    @Nullable
    public static AttachmentType<?> byName(ResourceLocation name) {
        return ForgeAttachmentRegistries.ATTACHMENT_TYPES().getValue(name);
    }

    /**
     * 取得附件类型的注册名，未注册时返回 null。
     */
    @Nullable
    public static ResourceLocation nameOf(AttachmentType<?> type) {
        return ForgeAttachmentRegistries.ATTACHMENT_TYPES().getKey(type);
    }

    /**
     * 某实体所在的服务端世界，不在服务端时返回 null。
     */
    @Nullable
    public static ServerLevel serverLevelOf(Entity entity) {
        return entity.level() instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    /**
     * 某方块实体所在的服务端世界，不在服务端时返回 null。
     */
    @Nullable
    public static ServerLevel serverLevelOf(BlockEntity blockEntity) {
        return blockEntity.getLevel() instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    private AttachmentInternals() {}
}