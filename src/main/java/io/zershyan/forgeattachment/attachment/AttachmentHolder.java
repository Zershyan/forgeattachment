package io.zershyan.forgeattachment.attachment;

import io.zershyan.forgeattachment.ForgeAttachment;
import io.zershyan.forgeattachment.registry.ForgeAttachmentRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 附件存储的实现。
 *
 * <p>实体与方块实体通过 mixin 各持有一个本类实例，并在自身 NBT 读写末尾追加一次调用，
 * 因此原版数据流不受影响。
 */
public class AttachmentHolder implements IAttachmentHolder {
    /** 附件在实体/方块实体 NBT 中占用的键。 */
    public static final String ATTACHMENTS_NBT_KEY = ForgeAttachment.MODID + ":attachments";

    private final IAttachmentHolder exposedHolder;

    @Nullable
    private Map<AttachmentType<?>, Object> attachments;

    /**
     * @param exposedHolder 暴露给使用者的宿主对象，通常是实体或方块实体自身
     */
    public AttachmentHolder(IAttachmentHolder exposedHolder) {
        this.exposedHolder = exposedHolder;
    }

    /**
     * 返回暴露给使用者的 holder。
     */
    public IAttachmentHolder getExposedHolder() {
        return this.exposedHolder;
    }

    private Map<AttachmentType<?>, Object> attachmentMap() {
        if (this.attachments == null) {
            this.attachments = new IdentityHashMap<>(4);
        }
        return this.attachments;
    }

    /**
     * 内部存储视图，供同步与复制逻辑使用。尚未创建时返回 null。
     */
    @Nullable
    public Map<AttachmentType<?>, Object> rawAttachments() {
        return this.attachments;
    }

    private void validate(AttachmentType<?> type) {
        Objects.requireNonNull(type);
        if (!ForgeAttachmentRegistries.ATTACHMENT_TYPES().containsValue(type)) {
            throw new IllegalArgumentException("Data attachment type must be registered!");
        }
    }

    @Override
    public final boolean hasAttachments() {
        return this.attachments != null && !this.attachments.isEmpty();
    }

    @Override
    public final boolean hasData(AttachmentType<?> type) {
        validate(type);
        return this.attachments != null && this.attachments.containsKey(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getData(AttachmentType<T> type) {
        validate(type);
        Map<AttachmentType<?>, Object> map = attachmentMap();
        T ret = (T) map.get(type);
        if (ret == null) {
            ret = type.defaultValueSupplier.apply(this.exposedHolder);
            map.put(type, ret);
            syncData(type);
        }
        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getExistingDataOrNull(AttachmentType<T> type) {
        validate(type);
        return this.attachments == null ? null : (T) this.attachments.get(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T setData(AttachmentType<T> type, T data) {
        validate(type);
        Objects.requireNonNull(data);
        T previous = (T) attachmentMap().put(type, data);
        syncData(type);
        return previous;
    }

    /**
     * 写入附件但不触发同步，供网络接收端使用。
     */
    public void setDataSilently(AttachmentType<?> type, Object data) {
        attachmentMap().put(type, data);
    }

    /**
     * 移除附件但不触发同步，供网络接收端使用。
     */
    public void removeDataSilently(AttachmentType<?> type) {
        if (this.attachments != null) {
            this.attachments.remove(type);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T removeData(AttachmentType<T> type) {
        validate(type);
        if (this.attachments == null) {
            return null;
        }
        T previous = (T) this.attachments.remove(type);
        syncData(type);
        return previous;
    }

    /**
     * 把可序列化的附件写入 NBT。没有可序列化附件时返回 null。
     */
    @Nullable
    public final CompoundTag serializeAttachments(HolderLookup.Provider provider) {
        if (this.attachments == null) {
            return null;
        }
        CompoundTag tag = null;
        for (Map.Entry<AttachmentType<?>, Object> entry : this.attachments.entrySet()) {
            AttachmentType<?> type = entry.getKey();
            if (type.serializer == null) {
                continue;
            }
            ResourceLocation key = ForgeAttachmentRegistries.ATTACHMENT_TYPES().getKey(type);
            if (key == null) {
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                Tag serialized = ((IAttachmentSerializer<Tag, Object>) type.serializer).write(entry.getValue(), provider);
                if (serialized != null) {
                    if (tag == null) {
                        tag = new CompoundTag();
                    }
                    tag.put(key.toString(), serialized);
                }
            } catch (Exception exception) {
                ForgeAttachment.LOGGER.error("Failed to serialize data attachment {}. Skipping.", key, exception);
            }
        }
        return tag;
    }

    /**
     * 把本 holder 上的附件复制到另一个 holder。
     *
     * @param isDeath true 时只复制标记了 copyOnDeath 的附件
     */
    @SuppressWarnings("unchecked")
    public final void copyAttachmentsTo(AttachmentHolder to, HolderLookup.Provider provider, boolean isDeath) {
        if (this.attachments == null) {
            return;
        }
        for (Map.Entry<AttachmentType<?>, Object> entry : this.attachments.entrySet()) {
            AttachmentType<?> type = entry.getKey();
            if (type.serializer == null) {
                continue;
            }
            if (isDeath && !type.copyOnDeath) {
                continue;
            }
            IAttachmentCopyHandler<Object> copyHandler = (IAttachmentCopyHandler<Object>) type.copyHandler;
            Object copy = copyHandler.copy(entry.getValue(), to.getExposedHolder(), provider);
            if (copy != null) {
                to.attachmentMap().put(type, copy);
            }
        }
    }

    /**
     * 读取此前由 {@link #serializeAttachments(HolderLookup.Provider)} 写出的附件。
     *
     * <p>不会触发附件的同步。
     */
    public final void deserializeAttachments(HolderLookup.Provider provider, CompoundTag tag) {        for (String key : tag.getAllKeys()) {
            ResourceLocation keyLocation = ResourceLocation.tryParse(key);
            if (keyLocation == null) {
                ForgeAttachment.LOGGER.error("Encountered invalid data attachment key {}. Skipping.", key);
                continue;
            }
            AttachmentType<?> type = ForgeAttachmentRegistries.ATTACHMENT_TYPES().getValue(keyLocation);
            if (type == null || type.serializer == null) {
                ForgeAttachment.LOGGER.error("Encountered unknown or non-serializable data attachment {}. Skipping.", key);
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                IAttachmentSerializer<Tag, Object> serializer = (IAttachmentSerializer<Tag, Object>) type.serializer;
                attachmentMap().put(type, serializer.read(this.exposedHolder, tag.get(key), provider));
            } catch (Exception exception) {
                ForgeAttachment.LOGGER.error("Failed to deserialize data attachment {}. Skipping.", key, exception);
            }
        }
    }
}