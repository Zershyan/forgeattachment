package io.zershyan.forgeattachment.config;

/**
 * 构建期开关。
 *
 * <p>由 {@code gradle.properties} 的 {@code enable_example} 在构建时展开生成，
 * 因此开关必须在编译期决定，不能在运行期读取配置。
 */
public final class StartupConfig {
    public static final boolean ENABLE_EXAMPLE = ${enable_example};

    private StartupConfig() {}
}