package io.zershyan.forgeattachment.mixin;

import io.zershyan.forgeattachment.attachment.AttachmentHolder;
import io.zershyan.forgeattachment.attachment.AttachmentSync;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * 给 {@link Entity} 注入附件存储与 NBT 读写。
 *
 * <p>注入点必须落在序列化入口上：{@code func_189511_e} = writeToNBT，
 * {@code func_70020_e} = readFromNBT。
 *
 * <p>不能注入 writeEntityToNBT（{@code func_70014_b}）：EntityLivingBase、EntityPlayer
 * 等子类都重写了它且不调用 super，注入的代码在虚方法分派下永远不会执行。
 */
@Mixin(Entity.class)
public abstract class MixinEntity implements IAttachmentHolder {

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
        T previous = this.forgeattachment$holder().setData(type, data);
        AttachmentSync.send((Entity) (Object) this, type, data);
        return previous;
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

    @Inject(method = "func_189511_e", at = @At("RETURN"))
    private void forgeattachment$writeAttachments(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir) {
        this.forgeattachment$holder().writeToNBT(compound);
    }

    @Inject(method = "func_70020_e", at = @At("RETURN"))
    private void forgeattachment$readAttachments(NBTTagCompound compound, CallbackInfo ci) {
        this.forgeattachment$holder().readFromNBT(compound);
    }
}
