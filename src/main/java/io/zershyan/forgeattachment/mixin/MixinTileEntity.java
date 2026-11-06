package io.zershyan.forgeattachment.mixin;

import io.zershyan.forgeattachment.attachment.AttachmentHolder;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * 给 {@link TileEntity} 注入附件存储与 NBT 读写。
 *
 * <p>注入点用 SRG 名：{@code func_189515_b} = writeToNBT，{@code func_145839_a} = readFromNBT。
 */
@Mixin(TileEntity.class)
public abstract class MixinTileEntity implements IAttachmentHolder {

    @Unique
    private AttachmentHolder forgeattachment$attachments;

    @Unique
    private AttachmentHolder forgeattachment$holder() {
        if (this.forgeattachment$attachments == null)
            this.forgeattachment$attachments = new AttachmentHolder();
        return this.forgeattachment$attachments;
    }

    @Override
    public <T> T getData(AttachmentType<T> type) {
        return this.forgeattachment$holder().getData(type);
    }

    @Override
    @Nullable
    public <T> T setData(AttachmentType<T> type, T data) {
        return this.forgeattachment$holder().setData(type, data);
    }

    @Override
    @Nullable
    public <T> T removeData(AttachmentType<T> type) {
        return this.forgeattachment$holder().removeData(type);
    }

    @Override
    public <T> boolean hasData(AttachmentType<T> type) {
        return this.forgeattachment$holder().hasData(type);
    }

    @Inject(method = "func_189515_b", at = @At("RETURN"))
    private void forgeattachment$writeAttachments(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir) {
        this.forgeattachment$holder().writeToNBT(compound);
    }

    @Inject(method = "func_145839_a", at = @At("RETURN"))
    private void forgeattachment$readAttachments(NBTTagCompound compound, CallbackInfo ci) {
        this.forgeattachment$holder().readFromNBT(compound);
    }
}
