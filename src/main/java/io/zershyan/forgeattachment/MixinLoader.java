package io.zershyan.forgeattachment;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Mixin 引导：注册配置文件。
 *
 * <p>生产环境由 manifest 的 {@code TweakClass} + {@code FMLCorePlugin} 触发，
 * 开发环境（ForgeGradle 直接运行）由 {@code -Dfml.coreMods.load} 触发。
 */
@IFMLLoadingPlugin.Name("ForgeAttachment Mixin Loader")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class MixinLoader implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    @Nullable
    public String getModContainerClass() {
        return null;
    }

    @Override
    @Nullable
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        try {
            Mixins.addConfiguration("forgeattachment.mixins.json");
        } catch (Throwable error) {
            // 开发环境里 Forge 的 log4j 配置会吞掉启动异常，这里直接打到 stderr
            error.printStackTrace();
            throw error;
        }
    }

    @Override
    @Nullable
    public String getAccessTransformerClass() {
        return null;
    }
}
