package io.zershyan.forgeattachment.example;

import io.zershyan.forgeattachment.example.registry.ExampleAttachments;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * 示例代码入口。
 *
 * <p>只在开发环境且构建期开关 {@code enable_example} 打开时由主类触发，
 * 示例内容不进入正常注册链。
 */
public final class ExampleHandler {
    /**
     * @param modBus mod 总线
     */
    public static void doRegister(IEventBus modBus) {
        ExampleAttachments.doRegister(modBus);
    }

    private ExampleHandler() {}
}