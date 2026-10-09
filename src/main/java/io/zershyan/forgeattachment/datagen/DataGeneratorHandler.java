package io.zershyan.forgeattachment.datagen;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.datagen.provider.ModLangProvider;
import io.zershyan.forgeattachment.datagen.provider.PackMetadataProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ForgeAttachment.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGeneratorHandler {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        generator.addProvider(event.includeServer(), new PackMetadataProvider(packOutput));
        generator.addProvider(event.includeClient(), ModLangProvider.runEnUs(packOutput));
        generator.addProvider(event.includeClient(), ModLangProvider.runZhCn(packOutput));
    }
}