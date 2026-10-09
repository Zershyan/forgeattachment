package io.zershyan.forgeattachment.example.registry;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.example.registry.attachment.ExampleAttachmentData;
import io.zershyan.forgeattachment.registry.ForgeAttachmentRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 示例附件类型注册入口。
 *
 * <p>演示 Codec 持久化、copyOnDeath 与同步三项能力，下游 mod 可参照本类注册自己的附件。
 */
public final class ExampleAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(ForgeAttachmentRegistries.ATTACHMENT_TYPES_KEY, ForgeAttachment.MODID);

    public static final RegistryObject<AttachmentType<ExampleAttachmentData>> EXAMPLE =
            REGISTER.register("example", ExampleAttachmentData::createType);

    /**
     * @see io.zershyan.forgeattachment.example.ExampleHandler#doRegister(IEventBus)
     */
    public static void doRegister(IEventBus modBus) {
        REGISTER.register(modBus);
    }

    private ExampleAttachments() {}
}