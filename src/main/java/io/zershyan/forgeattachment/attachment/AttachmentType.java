package io.zershyan.forgeattachment.attachment;

import com.mojang.serialization.Codec;
import io.zershyan.forgeattachment.network.RegistryFriendlyByteBuf;
import io.zershyan.forgeattachment.network.codec.StreamCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 一个数据附件类型：可挂载到任意 {@link IAttachmentHolder} 上的数据。
 *
 * <p>附件类型必须注册到 {@link io.zershyan.forgeattachment.registry.ForgeAttachmentRegistries#ATTACHMENT_TYPES}。
 *
 * <p>与 NeoForge 1.21.1 的 {@code AttachmentType} 对应，差异在于：
 * <ul>
 *   <li>序列化上下文使用 1.20.1 的 {@code HolderLookup.Provider}，而非 1.21.1 的 {@code RegistryOps} 包装。</li>
 *   <li>同步使用本 mod 的 {@link io.zershyan.forgeattachment.network.codec.StreamCodec}。</li>
 * </ul>
 */
public class AttachmentType<T> {
    final Function<IAttachmentHolder, T> defaultValueSupplier;
    @Nullable
    final IAttachmentSerializer<?, T> serializer;
    final boolean copyOnDeath;
    final IAttachmentCopyHandler<T> copyHandler;
    @Nullable
    AttachmentSyncHandler<T> syncHandler;

    private AttachmentType(Builder<T> builder) {
        this.defaultValueSupplier = builder.defaultValueSupplier;
        this.serializer = builder.serializer;
        this.copyOnDeath = builder.copyOnDeath;
        this.copyHandler = builder.copyHandler != null ? builder.copyHandler : defaultCopyHandler(builder.serializer);
        this.syncHandler = builder.syncHandler;
    }

    /**
     * 该附件类型的同步处理器，未配置同步时返回 null。
     *
     * <p>供 {@code client} 包读取，故不与其他字段同为包内可见。
     */
    @Nullable
    public AttachmentSyncHandler<T> syncHandler() {
        return this.syncHandler;
    }

    @SuppressWarnings("unchecked")
    private static <T> IAttachmentCopyHandler<T> defaultCopyHandler(@Nullable IAttachmentSerializer<?, T> serializer) {
        if (serializer == null) {
            return (attachment, holder, provider) -> {
                throw new UnsupportedOperationException("Cannot copy non-serializable attachments");
            };
        }
        IAttachmentSerializer<Tag, T> typed = (IAttachmentSerializer<Tag, T>) serializer;
        return (attachment, holder, provider) -> {
            Tag serialized = typed.write(attachment, provider);
            return serialized == null ? null : typed.read(holder, serialized, provider);
        };
    }

    /**
     * 创建一个附件类型构建器。
     *
     * @param defaultValueSupplier 默认值供应器
     */
    public static <T> Builder<T> builder(Supplier<T> defaultValueSupplier) {
        return builder(holder -> defaultValueSupplier.get());
    }

    /**
     * 创建一个附件类型构建器，允许默认值构造时捕获其 holder。
     *
     * @param defaultValueConstructor 默认值构造器，参数为持有该附件的对象
     */
    public static <T> Builder<T> builder(Function<IAttachmentHolder, T> defaultValueConstructor) {
        return new Builder<>(defaultValueConstructor);
    }

    /**
     * 创建使用 {@link INBTSerializable} 进行序列化的附件类型构建器。
     */
    public static <S extends Tag, T extends INBTSerializable<S>> Builder<T> serializable(Supplier<T> defaultValueSupplier) {
        return serializable(holder -> defaultValueSupplier.get());
    }

    /**
     * 创建使用 {@link INBTSerializable} 进行序列化的附件类型构建器，允许默认值构造时捕获其 holder。
     */
    public static <S extends Tag, T extends INBTSerializable<S>> Builder<T> serializable(Function<IAttachmentHolder, T> defaultValueConstructor) {
        return builder(defaultValueConstructor).serialize(new IAttachmentSerializer<S, T>() {
            @Override
            public T read(IAttachmentHolder holder, S tag, HolderLookup.Provider provider) {
                T ret = defaultValueConstructor.apply(holder);
                ret.deserializeNBT(tag);
                return ret;
            }

            @Nullable
            @Override
            public S write(T attachment, HolderLookup.Provider provider) {
                return attachment.serializeNBT();
            }
        });
    }

    public static class Builder<T> {
        private final Function<IAttachmentHolder, T> defaultValueSupplier;
        @Nullable
        private IAttachmentSerializer<?, T> serializer;
        private boolean copyOnDeath;
        @Nullable
        private IAttachmentCopyHandler<T> copyHandler;
        @Nullable
        private AttachmentSyncHandler<T> syncHandler;

        private Builder(Function<IAttachmentHolder, T> defaultValueSupplier) {
            this.defaultValueSupplier = defaultValueSupplier;
        }

        /**
         * 使该附件持久化到磁盘（仅在逻辑服务端）。
         */
        public Builder<T> serialize(IAttachmentSerializer<?, T> serializer) {
            if (this.serializer != null)
                throw new IllegalStateException("Serializer already set");
            this.serializer = serializer;
            return this;
        }

        /**
         * 使用 {@link Codec} 使该附件持久化到磁盘。
         *
         * <p>基于 Codec 的附件无法捕获其 holder。
         */
        public Builder<T> serialize(Codec<T> codec) {
            return serialize(codec, value -> true);
        }

        /**
         * 使用 {@link Codec} 使该附件持久化到磁盘，并可指定是否序列化。
         */
        public Builder<T> serialize(Codec<T> codec, Predicate<? super T> shouldSerialize) {
            return serialize(new IAttachmentSerializer<Tag, T>() {
                @Override
                public T read(IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                    return codec.parse(ops(provider), tag).getOrThrow(true, msg -> buildException("read", msg));
                }

                @Nullable
                @Override
                public Tag write(T attachment, HolderLookup.Provider provider) {
                    if (!shouldSerialize.test(attachment)) {
                        return null;
                    }
                    return codec.encodeStart(ops(provider), attachment).getOrThrow(true, msg -> buildException("write", msg));
                }

                /** 1.20.1 没有 HolderLookup.Provider#createSerializationContext，改由 RegistryOps 包装 NbtOps。 */
                private RegistryOps<Tag> ops(HolderLookup.Provider provider) {
                    return RegistryOps.create(NbtOps.INSTANCE, provider);
                }

                private RuntimeException buildException(String operation, String error) {
                    return new IllegalStateException("Unable to " + operation + " attachment due to an internal codec error: " + error);
                }
            });
        }

        /**
         * 使该附件在玩家重生或生物转化时被复制。
         */
        public Builder<T> copyOnDeath() {
            if (this.serializer == null)
                throw new IllegalStateException("copyOnDeath requires a serializer");
            this.copyOnDeath = true;
            return this;
        }

        /**
         * 覆盖默认的复制处理器。默认处理器通过序列化再反序列化完成复制。
         */
        public Builder<T> copyHandler(IAttachmentCopyHandler<T> cloner) {
            if (this.serializer == null)
                throw new IllegalStateException("copyHandler requires a serializer");
            this.copyHandler = cloner;
            return this;
        }

        /**
         * 使用给定的同步处理器把该附件同步给客户端。
         */
        public Builder<T> sync(AttachmentSyncHandler<T> syncHandler) {
            this.syncHandler = syncHandler;
            return this;
        }

        /**
         * 使用给定的流编解码器把该附件同步给所有能收到该 holder 的客户端。
         */
        public Builder<T> sync(StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
            return sync((holder, to) -> true, streamCodec);
        }

        /**
         * 使用给定的流编解码器把该附件同步给部分客户端。
         *
         * @param sendToPlayer 判断是否应向某个玩家同步
         */
        public Builder<T> sync(BiPredicate<IAttachmentHolder, ServerPlayer> sendToPlayer,
                               StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
            return sync(new AttachmentSyncHandler<>() {
                @Override
                public boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to) {
                    return sendToPlayer.test(holder, to);
                }

                @Override
                public void write(RegistryFriendlyByteBuf buf, T attachment, boolean initialSync) {
                    streamCodec.encode(buf, attachment);
                }

                @Override
                public T read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable T previousValue) {
                    return streamCodec.decode(buf);
                }
            });
        }

        public AttachmentType<T> build() {
            return new AttachmentType<>(this);
        }
    }
}