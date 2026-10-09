package io.zershyan.forgeattachment.datagen.provider;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.datagen.lang.ForgeAttachmentLang;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 双 locale 语言 Provider。
 */
public class ModLangProvider extends LanguageProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModLangProvider.class);
    private static final String enUs = "en_us";
    private static final String zhCn = "zh_cn";

    private final String locale;

    public ModLangProvider(PackOutput output, String locale) {
        super(output, ForgeAttachment.MODID, locale);
        this.locale = locale;
    }

    public static ModLangProvider runEnUs(PackOutput output) {
        return new ModLangProvider(output, enUs);
    }

    public static ModLangProvider runZhCn(PackOutput output) {
        return new ModLangProvider(output, zhCn);
    }

    @Override
    protected void addTranslations() {
        switch (locale) {
            case enUs -> ForgeAttachmentLang.getAllLang().forEach(langEntry ->
                    addTranslation(langEntry.key(), langEntry.lang().enUs()));
            case zhCn -> ForgeAttachmentLang.getAllLang().forEach(langEntry ->
                    addTranslation(langEntry.key(), langEntry.lang().zhCn()));
            default -> LOGGER.error("Unknown locale: {}", locale);
        }
    }

    private void addTranslation(Object key, String desc) {
        if (key instanceof String string) {
            add(string, desc);
        } else {
            LOGGER.error("Unknown object type: {}", key.getClass());
            add(key.toString(), desc);
        }
    }
}