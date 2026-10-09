package io.zershyan.forgeattachment.registry;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

/**
 * 本 mod 的自定义注册表。
 *
 * <p>附件类型按名注册。网络同步以 {@link net.minecraft.resources.ResourceLocation} 为标识，
 * 因此注册表本身无需同步——两端各自注册同一批附件类型即可。
 */
public final class ForgeAttachmentRegistries {
    /**
     * 附件类型的注册表。
     */
    public static final ResourceKey<Registry<AttachmentType<?>>> ATTACHMENT_TYPES_KEY =
            ResourceKey.createRegistryKey(ForgeAttachment.id("attachment_types"));

    private static Supplier<IForgeRegistry<AttachmentType<?>>> attachmentTypes;

    /**
     * 附件类型注册表。仅在 {@link #doRegister(IEventBus)} 所挂的 {@link NewRegistryEvent} 触发后可用。
     */
    public static IForgeRegistry<AttachmentType<?>> ATTACHMENT_TYPES() {
        return attachmentTypes.get();
    }

    private static void createRegistries(NewRegistryEvent event) {
        attachmentTypes = event.create(new RegistryBuilder<AttachmentType<?>>()
                .setName(ATTACHMENT_TYPES_KEY.location())
                .disableSaving()
                .disableSync());
    }

    public static void doRegister(IEventBus modBus) {
        modBus.addListener(ForgeAttachmentRegistries::createRegistries);
    }

    private ForgeAttachmentRegistries() {}
}