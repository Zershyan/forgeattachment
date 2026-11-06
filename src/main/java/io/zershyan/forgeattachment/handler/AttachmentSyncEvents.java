package io.zershyan.forgeattachment.handler;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.attachment.AttachmentRegistry;
import io.zershyan.forgeattachment.attachment.AttachmentSync;
import io.zershyan.forgeattachment.attachment.AttachmentType;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 玩家开始追踪实体时补发同步附件。
 */
@Mod.EventBusSubscriber(modid = ForgeAttachment.MODID)
public final class AttachmentSyncEvents {

    private AttachmentSyncEvents() {
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        EntityPlayer player = event.getEntityPlayer();
        Entity target = event.getTarget();
        if (player == null || target == null)
            return;
        AttachmentSync.sendAll(player, target);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        EntityPlayer original = event.getOriginal();
        EntityPlayer replacement = event.getEntityPlayer();
        if (!(original instanceof IAttachmentHolder) || !(replacement instanceof IAttachmentHolder))
            return;
        IAttachmentHolder source = (IAttachmentHolder) original;
        IAttachmentHolder target = (IAttachmentHolder) replacement;
        for (AttachmentType<?> type : AttachmentRegistry.all()) {
            if (!type.copyOnDeath() || !source.hasData(type))
                continue;
            copy(source, target, type);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void copy(IAttachmentHolder source, IAttachmentHolder target, AttachmentType<?> type) {
        AttachmentType<T> typed = (AttachmentType<T>) type;
        target.setData(typed, source.getData(typed));
    }
}
