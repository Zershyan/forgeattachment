package io.zershyan.forgeattachment.datagen.provider;

import io.zershyan.forgeattachment.datagen.lang.ForgeAttachmentKeyLang;
import net.minecraft.DetectedVersion;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;

/**
 * 包描述用语言键，版本用 {@link DetectedVersion}。
 */
public class PackMetadataProvider extends PackMetadataGenerator {
    public PackMetadataProvider(PackOutput output) {
        super(output);
        add(PackMetadataSection.TYPE, new PackMetadataSection(
                ForgeAttachmentKeyLang.Resource,
                DetectedVersion.BUILT_IN.getPackVersion(PackType.SERVER_DATA)
        ));
    }
}