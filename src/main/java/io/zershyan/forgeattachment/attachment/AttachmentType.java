package io.zershyan.forgeattachment.attachment;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * 一种可挂载到 {@link IAttachmentHolder} 上的数据类型。
 *
 * <p>与 NeoForge 1.21.1 的 {@code AttachmentType} 对应，差别在于 1.12.2 没有 Codec：
 * 磁盘用 {@link NBTTagCompound}（{@link IAttachmentSerializer}），网络用 {@link PacketBuffer}
 * （{@link IAttachmentSyncCodec}）。
 *
 * <p>实例必须经 {@link AttachmentRegistry#register(ResourceLocation, AttachmentType)} 注册后才能持久化与同步。
 */
public final class AttachmentType<T> {

    private final Supplier<T> defaultValueSupplier;
    @Nullable
    private final IAttachmentSerializer<T> serializer;
    private final boolean copyOnDeath;
    @Nullable
    private final IAttachmentSyncCodec<T> syncCodec;

    @Nullable
    private ResourceLocation registryName;

    private AttachmentType(Builder<T> builder) {
        this.defaultValueSupplier = builder.defaultValueSupplier;
        this.serializer = builder.serializer;
        this.copyOnDeath = builder.copyOnDeath;
        this.syncCodec = builder.syncCodec;
    }

    public static <T> Builder<T> builder(Supplier<T> defaultValueSupplier) {
        return new Builder<>(defaultValueSupplier);
    }

    /**
     * 默认值的全新实例，每次调用都重新取，避免多个 holder 共享同一对象。
     */
    public T createDefault() {
        return this.defaultValueSupplier.get();
    }

    @Nullable
    public IAttachmentSerializer<T> serializer() {
        return this.serializer;
    }

    @Nullable
    public IAttachmentSyncCodec<T> syncCodec() {
        return this.syncCodec;
    }

    public boolean copyOnDeath() {
        return this.copyOnDeath;
    }

    @Nullable
    public ResourceLocation getRegistryName() {
        return this.registryName;
    }

    void bindName(ResourceLocation name) {
        this.registryName = name;
    }

    public static final class Builder<T> {

        private final Supplier<T> defaultValueSupplier;
        @Nullable
        private IAttachmentSerializer<T> serializer;
        @Nullable
        private IAttachmentSyncCodec<T> syncCodec;
        private boolean copyOnDeath;

        private Builder(Supplier<T> defaultValueSupplier) {
            this.defaultValueSupplier = defaultValueSupplier;
        }

        /**
         * 让附件持久化到存档。
         */
        public Builder<T> serialize(IAttachmentSerializer<T> serializer) {
            this.serializer = serializer;
            return this;
        }

        /**
         * 让附件同步到客户端。
         */
        public Builder<T> sync(IAttachmentSyncCodec<T> syncCodec) {
            this.syncCodec = syncCodec;
            return this;
        }

        /**
         * 玩家死亡重生时保留该附件。
         */
        public Builder<T> copyOnDeath() {
            this.copyOnDeath = true;
            return this;
        }

        public AttachmentType<T> build() {
            return new AttachmentType<>(this);
        }
    }
}
