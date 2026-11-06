package io.zershyan.forgeattachment.network;

import io.zershyan.forgeattachment.ForgeAttachment;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 网络通道。
 */
public final class ForgeAttachmentNetwork {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(ForgeAttachment.MODID);

    private ForgeAttachmentNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(MessageSyncAttachment.Handler.class, MessageSyncAttachment.class, 0, Side.CLIENT);
    }
}
