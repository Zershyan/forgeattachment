package io.zershyan.forgeattachment;

import io.zershyan.forgeattachment.attachment.AttachmentHolder;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import io.zershyan.forgeattachment.example.ExampleAttachments;
import io.zershyan.forgeattachment.network.ForgeAttachmentNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Forge 1.12.2 版入口。
 */
@Mod(
        modid = ForgeAttachment.MODID,
        name = ForgeAttachment.NAME,
        version = ForgeAttachment.VERSION,
        acceptedMinecraftVersions = "[1.12,1.13)"
)
public class ForgeAttachment {
    public static final String MODID = "forgeattachment";
    public static final String NAME = "ForgeAttachment";
    public static final String VERSION = "0.0.1";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ForgeAttachmentNetwork.init();
        ExampleAttachments.init();
        selfCheck(event);
    }

    /**
     * 启动自检：确认 Mixin 注入生效、附件能完成 NBT 往返。
     */
    private void selfCheck(FMLPreInitializationEvent event) {
        boolean entityInjected = IAttachmentHolder.class.isAssignableFrom(Entity.class);
        boolean tileInjected = IAttachmentHolder.class.isAssignableFrom(TileEntity.class);
        if (entityInjected && tileInjected) {
            event.getModLog().info("Mixin injection OK: Entity and TileEntity implement IAttachmentHolder");
        } else {
            event.getModLog().error("Mixin injection FAILED: Entity={}, TileEntity={}", entityInjected, tileInjected);
        }

        AttachmentHolder holder = new AttachmentHolder();
        holder.setData(ExampleAttachments.NOTE, "round-trip");
        NBTTagCompound tag = new NBTTagCompound();
        holder.writeToNBT(tag);
        AttachmentHolder restored = new AttachmentHolder();
        restored.readFromNBT(tag);
        String value = restored.getData(ExampleAttachments.NOTE);
        if ("round-trip".equals(value)) {
            event.getModLog().info("Attachment NBT round-trip OK: {}", value);
        } else {
            event.getModLog().error("Attachment NBT round-trip FAILED: {}", value);
        }
    }
}
