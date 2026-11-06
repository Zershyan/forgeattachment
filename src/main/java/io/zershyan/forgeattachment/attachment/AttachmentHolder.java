package io.zershyan.forgeattachment.attachment;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 附件存储本体，由 Mixin 注入到实体与方块实体的字段持有。
 */
public class AttachmentHolder implements IAttachmentHolder {

    private static final String NBT_ROOT = "ForgeAttachments";

    private final Map<AttachmentType<?>, Object> data = new HashMap<>();
    private boolean dirty;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(AttachmentType<T> type) {
        if (this.data.containsKey(type))
            return (T) this.data.get(type);
        T created = type.createDefault();
        this.data.put(type, created);
        return created;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T setData(AttachmentType<T> type, T data) {
        T previous = (T) this.data.put(type, data);
        markChanged(type);
        return previous;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T removeData(AttachmentType<T> type) {
        T previous = (T) this.data.remove(type);
        markChanged(type);
        return previous;
    }

    @Override
    public <T> boolean hasData(AttachmentType<T> type) {
        return this.data.containsKey(type);
    }

    /**
     * 该附件类型是否配置了同步。
     */
    public boolean isSynced(AttachmentType<?> type) {
        return type.syncCodec() != null;
    }

    /**
     * 取同步用的副本；未同步的类型返回 null。
     */
    @Nullable
    public NBTTagCompound serializeForSync(AttachmentType<?> type) {
        if (!this.data.containsKey(type) || type.syncCodec() == null)
            return null;
        return AttachmentRegistry.write(type, this.data.get(type));
    }

    /**
     * 写入存档：只持久化注册过且配了序列化器的类型。
     */
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound root = new NBTTagCompound();
        for (Map.Entry<AttachmentType<?>, Object> entry : this.data.entrySet()) {
            AttachmentType<?> type = entry.getKey();
            ResourceLocation name = type.getRegistryName();
            if (name == null || type.serializer() == null)
                continue;
            NBTTagCompound tag = AttachmentRegistry.write(type, entry.getValue());
            if (tag != null)
                root.setTag(name.toString(), tag);
        }
        if (!root.isEmpty())
            compound.setTag(NBT_ROOT, root);
        return compound;
    }

    /**
     * 读取存档。
     */
    public void readFromNBT(NBTTagCompound compound) {
        this.data.clear();
        if (!compound.hasKey(NBT_ROOT, 10))
            return;
        NBTTagCompound root = compound.getCompoundTag(NBT_ROOT);
        for (String key : root.getKeySet()) {
            AttachmentType<?> type = AttachmentRegistry.byNbtKey(key);
            if (type == null || type.serializer() == null)
                continue;
            this.data.put(type, readAttachment(type, root.getCompoundTag(key)));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T readAttachment(AttachmentType<?> type, NBTTagCompound tag) {
        AttachmentType<T> typed = (AttachmentType<T>) type;
        return typed.serializer().read(tag);
    }

    /**
     * 复制 copyOnDeath 的附件到新 holder（玩家重生时用）。
     */
    public void copyOnDeathTo(AttachmentHolder target) {
        for (Map.Entry<AttachmentType<?>, Object> entry : this.data.entrySet()) {
            if (!entry.getKey().copyOnDeath())
                continue;
            target.data.put(entry.getKey(), entry.getValue());
        }
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    private void markChanged(AttachmentType<?> type) {
        if (type.syncCodec() != null)
            this.dirty = true;
    }
}
