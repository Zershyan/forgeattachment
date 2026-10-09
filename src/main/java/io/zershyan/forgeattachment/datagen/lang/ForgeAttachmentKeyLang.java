package io.zershyan.forgeattachment.datagen.lang;

import io.zershyan.forgeattachment.ForgeAttachment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用文本。
 */
public class ForgeAttachmentKeyLang extends ForgeAttachmentLang {
    private static final List<FinalEntry<String>> TranslatableLang = new ArrayList<>();
    private static final String ModId = ForgeAttachment.MODID;
    private static final String ModName = ForgeAttachment.class.getSimpleName();

    public static final MutableComponent Resource = entry(ModId + ".resources", "Resources for " + ModName, ModName + "资源");

    private static MutableComponent entry(String key, String enUs, String zhCn) {
        TranslatableLang.add(new FinalEntry<>(key, enUs, zhCn));
        return Component.translatable(key);
    }

    @Override
    public List<Entry> init(List<Entry> entries) {
        entries.addAll(TranslatableLang);
        return entries;
    }
}