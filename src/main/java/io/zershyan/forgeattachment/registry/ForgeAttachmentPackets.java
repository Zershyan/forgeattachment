package io.zershyan.forgeattachment.registry;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.registry.packet.SyncAttachmentsPacket;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;

/**
 * 本 mod 的网络通道。
 */
public final class ForgeAttachmentPackets {
    @NotNull
    private static final String PROTOCOL_VERSION = ModList.get()
            .getModContainerById(ForgeAttachment.MODID)
            .map(ModContainer::getModInfo)
            .map(IModInfo::getVersion)
            .map(Object::toString)
            .orElse("unknown");

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ForgeAttachment.id("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void doRegister(IEventBus modBus) {
        int id = 0;
        CHANNEL.messageBuilder(SyncAttachmentsPacket.class, id++)
                .encoder(SyncAttachmentsPacket::encode)
                .decoder(SyncAttachmentsPacket::decode)
                .consumerMainThread(SyncAttachmentsPacket::handle)
                .add();
    }

    private ForgeAttachmentPackets() {}
}