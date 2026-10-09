package io.zershyan.forgeattachment;

import io.zershyan.forgeattachment.config.StartupConfig;
import io.zershyan.forgeattachment.example.ExampleHandler;
import io.zershyan.forgeattachment.registry.ForgeAttachmentPackets;
import io.zershyan.forgeattachment.registry.ForgeAttachmentRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 把 NeoForge 1.21.1 的数据附件（Data Attachment）机制移植到 Forge 1.20.1。
 *
 * <p>下游 mod 可以像在 1.21.1 中一样注册 {@link io.zershyan.forgeattachment.attachment.AttachmentType}，
 * 并通过 {@link io.zershyan.forgeattachment.attachment.IAttachmentHolder} 读写附件。
 */
@Mod(ForgeAttachment.MODID)
public class ForgeAttachment {
    public static final String MODID = "forgeattachment";
    public static final Logger LOGGER = LoggerFactory.getLogger(ForgeAttachment.class);

    public ForgeAttachment() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ForgeAttachmentRegistries.doRegister(modBus);
        ForgeAttachmentPackets.doRegister(modBus);

        //Example
        if (!FMLEnvironment.production && StartupConfig.ENABLE_EXAMPLE) {
            ExampleHandler.doRegister(modBus);
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}