package io.zershyan.forgeattachment.mixin.forgeattachment.common;

import io.zershyan.forgeattachment.attachment.AttachmentHolder;
import io.zershyan.forgeattachment.attachment.AttachmentSync;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import io.zershyan.forgeattachment.util.mixin.IMixinEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * 让所有实体成为附件 holder，并把附件读写追加到原版 NBT 流程末尾。
 *
 * <p>仅做追加，不改变原版任何既有行为。
 */
@Mixin(Entity.class)
public abstract class MixinEntity implements IAttachmentHolder, IMixinEntity {
    @Unique
    private AttachmentHolder forgeattachment$holder;

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
        return forgeattachment$holder().setData(type, data);
    }

    @Override
    public <T> @Nullable T removeData(AttachmentType<T> type) {
        return this.forgeattachment$holder == null ? null : this.forgeattachment$holder.removeData(type);
    }

    @Override
    public void syncData(AttachmentType<?> type) {
        AttachmentSync.syncEntityUpdate((Entity) (Object) this, type);
    }

    @Unique
    private AttachmentHolder forgeattachment$holder() {
        if (this.forgeattachment$holder == null) {
            this.forgeattachment$holder = new AttachmentHolder(this);
        }
        return this.forgeattachment$holder;
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void forgeattachment$saveAttachments(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.forgeattachment$holder == null) {
            return;
        }
        Entity self = (Entity) (Object) this;
        CompoundTag attachments = this.forgeattachment$holder.serializeAttachments(self.level().registryAccess());
        if (attachments != null) {
            tag.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, attachments);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void forgeattachment$loadAttachments(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains(AttachmentHolder.ATTACHMENTS_NBT_KEY, CompoundTag.TAG_COMPOUND)) {
            return;
        }
        Entity self = (Entity) (Object) this;
        forgeattachment$holder().deserializeAttachments(self.level().registryAccess(), tag.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY));
    }
}