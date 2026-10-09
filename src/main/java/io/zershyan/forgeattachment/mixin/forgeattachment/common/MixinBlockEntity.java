package io.zershyan.forgeattachment.mixin.forgeattachment.common;

import io.zershyan.forgeattachment.attachment.AttachmentHolder;
import io.zershyan.forgeattachment.attachment.AttachmentSync;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import io.zershyan.forgeattachment.util.mixin.IMixinBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * 让所有方块实体成为附件 holder，并把附件读写追加到原版 NBT 流程末尾。
 *
 * <p>仅做追加，不改变原版任何既有行为。方块实体加载 NBT 时尚未绑定 level，
 * 因此附件数据先暂存，待 {@code setLevel} 后再反序列化。
 */
@Mixin(BlockEntity.class)
public abstract class MixinBlockEntity implements IAttachmentHolder, IMixinBlockEntity {
    @Unique
    private AttachmentHolder forgeattachment$holder;

    @Unique
    @Nullable
    private CompoundTag forgeattachment$pendingAttachments;

    @Override
    public AttachmentHolder forgeattachment$attachmentHolder() {
        return forgeattachment$holder();
    }

    @Override
    public boolean hasAttachments() {
        return this.forgeattachment$holder != null && this.forgeattachment$holder.hasAttachments();
    }

    @Override
    public boolean hasData(AttachmentType<?> type) {
        return this.forgeattachment$holder != null && this.forgeattachment$holder.hasData(type);
    }

    @Override
    public <T> T getData(AttachmentType<T> type) {
        return forgeattachment$holder().getData(type);
    }

    @Override
    @Nullable
    public <T> T getExistingDataOrNull(AttachmentType<T> type) {
        return this.forgeattachment$holder == null ? null : this.forgeattachment$holder.getExistingDataOrNull(type);
    }

    @Override
    public <T> @Nullable T setData(AttachmentType<T> type, T data) {
        T previous = forgeattachment$holder().setData(type, data);
        ((BlockEntity) (Object) this).setChanged();
        return previous;
    }

    @Override
    public <T> @Nullable T removeData(AttachmentType<T> type) {
        if (this.forgeattachment$holder == null) {
            return null;
        }
        T previous = this.forgeattachment$holder.removeData(type);
        ((BlockEntity) (Object) this).setChanged();
        return previous;
    }

    @Override
    public void syncData(AttachmentType<?> type) {
        AttachmentSync.syncBlockEntityUpdate((BlockEntity) (Object) this, type);
    }

    @Unique
    private AttachmentHolder forgeattachment$holder() {
        if (this.forgeattachment$holder == null) {
            this.forgeattachment$holder = new AttachmentHolder(this);
        }
        return this.forgeattachment$holder;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void forgeattachment$saveAttachments(CompoundTag tag, CallbackInfo ci) {
        if (this.forgeattachment$holder == null) {
            return;
        }
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() == null) {
            return;
        }
        CompoundTag attachments = this.forgeattachment$holder.serializeAttachments(self.getLevel().registryAccess());
        if (attachments != null) {
            tag.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, attachments);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void forgeattachment$loadAttachments(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(AttachmentHolder.ATTACHMENTS_NBT_KEY, CompoundTag.TAG_COMPOUND)) {
            this.forgeattachment$pendingAttachments = tag.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY);
        }
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void forgeattachment$flushPendingAttachments(Level level, CallbackInfo ci) {
        if (this.forgeattachment$pendingAttachments == null) {
            return;
        }
        forgeattachment$holder().deserializeAttachments(level.registryAccess(), this.forgeattachment$pendingAttachments);
        this.forgeattachment$pendingAttachments = null;
    }
}