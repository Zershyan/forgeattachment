package io.zershyan.forgeattachment.attachment;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 附件类型注册表。
 *
 * <p>1.12.2 没有 {@code DeferredRegister}，这里用静态表，在模组初始化阶段注册。
 */
public final class AttachmentRegistry {

    private static final Map<ResourceLocation, AttachmentType<?>> BY_NAME = new LinkedHashMap<>();
    private static final List<AttachmentType<?>> ALL = new ArrayList<>();

    private AttachmentRegistry() {
    }

    public static <T> AttachmentType<T> register(ResourceLocation name, AttachmentType<T> type) {
        if (BY_NAME.containsKey(name))
            throw new IllegalStateException("Duplicate attachment type: " + name);
        type.bindName(name);
        BY_NAME.put(name, type);
        ALL.add(type);
        return type;
    }

    @Nullable
    public static AttachmentType<?> get(ResourceLocation name) {
        return BY_NAME.get(name);
    }

    public static Collection<AttachmentType<?>> all() {
        return ALL;
    }

    /**
     * 从 NBT 键（注册名）反查类型，读存档时用。
     */
    @Nullable
    static AttachmentType<?> byNbtKey(String key) {
        return BY_NAME.get(new ResourceLocation(key));
    }

    static NBTTagCompound write(AttachmentType<?> type, Object value) {
        @SuppressWarnings("unchecked")
        AttachmentType<Object> typed = (AttachmentType<Object>) type;
        IAttachmentSerializer<Object> serializer = typed.serializer();
        return serializer == null ? null : serializer.write(value);
    }
}
