package io.zershyan.forgeattachment.handler.common;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.attachment.AttachmentInternals;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 处理需要复制附件的原版时机：玩家重生与生物转化。
 */
@Mod.EventBusSubscriber(modid = ForgeAttachment.MODID)
public final class AttachmentCopyHandler {
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Entity original = event.getOriginal();
        AttachmentInternals.copyAttachments(original.level().registryAccess(), original, event.getEntity(), event.isWasDeath());
    }

    @SubscribeEvent
    public static void onLivingConvert(LivingConversionEvent.Post event) {
        AttachmentInternals.copyAttachments(event.getEntity().level().registryAccess(), event.getEntity(), event.getOutcome(), true);
    }

    private AttachmentCopyHandler() {}
}