package io.zershyan.forgeattachment.network.codec;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.zershyan.forgeattachment.network.RegistryFriendlyByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * 1.21.1 的 {@code net.minecraft.network.codec.ByteBufCodecs} 在 1.20.1 上的移植。
 *
 * <p>常量名与签名与原版一致，便于按 1.21.1 的写法直接使用。
 * 与原版的差异来自 1.20.1 缺少的 API：原版把 NBT、UTF 字符串、UUID 等读写做成了
 * {@code FriendlyByteBuf} 的静态方法，1.20.1 只有实例方法，因此这里统一按需包装出
 * {@link FriendlyByteBuf}；{@code VarInt}/{@code VarLong}/{@code Utf8String} 这些独立类型
 * 在 1.20.1 不存在，改用 {@code FriendlyByteBuf} 上的对应方法。
 */
public interface ByteBufCodecs {
    int MAX_INITIAL_COLLECTION_SIZE = 65536;

    StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    StreamCodec<ByteBuf, Byte> BYTE = StreamCodec.of((buf, value) -> buf.writeByte(value), buf -> buf.readByte());
    StreamCodec<ByteBuf, Short> SHORT = StreamCodec.of((buf, value) -> buf.writeShort(value), buf -> buf.readShort());
    StreamCodec<ByteBuf, Integer> UNSIGNED_SHORT = StreamCodec.of(ByteBuf::writeShort, ByteBuf::readUnsignedShort);
    StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(
            (buf, value) -> friendly(buf).writeVarInt(value),
            buf -> friendly(buf).readVarInt());
    StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(
            (buf, value) -> friendly(buf).writeVarLong(value),
            buf -> friendly(buf).readVarLong());
    StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of(ByteBuf::writeDouble, ByteBuf::readDouble);
    StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = StreamCodec.of(
            (buf, value) -> friendly(buf).writeByteArray(value),
            buf -> friendly(buf).readByteArray());
    StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
    StreamCodec<ByteBuf, Tag> TAG = tagCodec(() -> new NbtAccounter(2097152L));
    StreamCodec<ByteBuf, Tag> TRUSTED_TAG = tagCodec(() -> NbtAccounter.UNLIMITED);
    StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = compoundTagCodec(() -> new NbtAccounter(2097152L));
    StreamCodec<ByteBuf, CompoundTag> TRUSTED_COMPOUND_TAG = compoundTagCodec(() -> NbtAccounter.UNLIMITED);
    StreamCodec<ByteBuf, Optional<CompoundTag>> OPTIONAL_COMPOUND_TAG = StreamCodec.of(
            (buf, value) -> friendly(buf).writeNbt(value.orElse(null)),
            buf -> Optional.ofNullable(friendly(buf).readNbt()));
    StreamCodec<ByteBuf, Vector3f> VECTOR3F = StreamCodec.of(
            (buf, value) -> friendly(buf).writeVector3f(value),
            buf -> friendly(buf).readVector3f());
    StreamCodec<ByteBuf, Quaternionf> QUATERNIONF = StreamCodec.of(
            (buf, value) -> friendly(buf).writeQuaternion(value),
            buf -> friendly(buf).readQuaternion());
    StreamCodec<ByteBuf, PropertyMap> GAME_PROFILE_PROPERTIES = new StreamCodec<>() {
        private static final int MAX_PROPERTY_NAME_LENGTH = 64;
        private static final int MAX_PROPERTY_VALUE_LENGTH = 32767;
        private static final int MAX_PROPERTY_SIGNATURE_LENGTH = 1024;
        private static final int MAX_PROPERTIES = 16;

        @Override
        public PropertyMap decode(ByteBuf buf) {
            int count = readCount(buf, MAX_PROPERTIES);
            PropertyMap properties = new PropertyMap();
            for (int i = 0; i < count; i++) {
                FriendlyByteBuf friendly = friendly(buf);
                String name = friendly.readUtf(MAX_PROPERTY_NAME_LENGTH);
                String value = friendly.readUtf(MAX_PROPERTY_VALUE_LENGTH);
                String signature = friendly.readNullable(friendlyBuf -> friendlyBuf.readUtf(MAX_PROPERTY_SIGNATURE_LENGTH));
                Property property = new Property(name, value, signature);
                properties.put(property.getName(), property);
            }
            return properties;
        }

        @Override
        public void encode(ByteBuf buf, PropertyMap properties) {
            writeCount(buf, properties.size(), MAX_PROPERTIES);
            for (Property property : properties.values()) {
                FriendlyByteBuf friendly = friendly(buf);
                friendly.writeUtf(property.getName(), MAX_PROPERTY_NAME_LENGTH);
                friendly.writeUtf(property.getValue(), MAX_PROPERTY_VALUE_LENGTH);
                friendly.writeNullable(property.getSignature(),
                        (friendlyBuf, signature) -> friendlyBuf.writeUtf(signature, MAX_PROPERTY_SIGNATURE_LENGTH));
            }
        }
    };
    StreamCodec<ByteBuf, GameProfile> GAME_PROFILE = new StreamCodec<>() {
        private static final int MAX_NAME_LENGTH = 16;

        @Override
        public GameProfile decode(ByteBuf buf) {
            FriendlyByteBuf friendly = friendly(buf);
            GameProfile profile = new GameProfile(friendly.readUUID(), friendly.readUtf(MAX_NAME_LENGTH));
            profile.getProperties().putAll(GAME_PROFILE_PROPERTIES.decode(buf));
            return profile;
        }

        @Override
        public void encode(ByteBuf buf, GameProfile profile) {
            FriendlyByteBuf friendly = friendly(buf);
            friendly.writeUUID(profile.getId());
            friendly.writeUtf(profile.getName(), MAX_NAME_LENGTH);
            GAME_PROFILE_PROPERTIES.encode(buf, profile.getProperties());
        }
    };

    static StreamCodec<ByteBuf, byte[]> byteArray(int maxLength) {
        return StreamCodec.of((buf, value) -> {
            if (value.length > maxLength) {
                throw new EncoderException("ByteArray with size " + value.length + " is bigger than allowed " + maxLength);
            }
            friendly(buf).writeByteArray(value);
        }, buf -> friendly(buf).readByteArray(maxLength));
    }

    static StreamCodec<ByteBuf, String> stringUtf8(int maxLength) {
        return StreamCodec.of(
                (buf, value) -> friendly(buf).writeUtf(value, maxLength),
                buf -> friendly(buf).readUtf(maxLength));
    }

    static StreamCodec<ByteBuf, Tag> tagCodec(Supplier<NbtAccounter> sizeTracker) {
        return StreamCodec.of(
                (buf, value) -> {
                    if (value == EndTag.INSTANCE) {
                        throw new EncoderException("Expected non-null compound tag");
                    }
                    writeTag(buf, value);
                },
                buf -> {
                    Tag tag = readTag(buf, sizeTracker.get());
                    if (tag == null) {
                        throw new DecoderException("Expected non-null compound tag");
                    }
                    return tag;
                });
    }

    static StreamCodec<ByteBuf, CompoundTag> compoundTagCodec(Supplier<NbtAccounter> sizeTracker) {
        return tagCodec(sizeTracker).map(tag -> {
            if (tag instanceof CompoundTag compoundTag) {
                return compoundTag;
            }
            throw new DecoderException("Not a compound tag: " + tag);
        }, tag -> tag);
    }

    static <T> StreamCodec<ByteBuf, T> fromCodecTrusted(Codec<T> codec) {
        return fromCodec(codec, () -> NbtAccounter.UNLIMITED);
    }

    static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec) {
        return fromCodec(codec, () -> new NbtAccounter(2097152L));
    }

    static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec, Supplier<NbtAccounter> sizeTracker) {
        return tagCodec(sizeTracker).map(
                tag -> getOrThrow(codec.parse(NbtOps.INSTANCE, tag),
                        error -> new DecoderException("Failed to decode: " + error + " " + tag)),
                value -> getOrThrow(codec.encodeStart(NbtOps.INSTANCE, value),
                        error -> new EncoderException("Failed to encode: " + error + " " + value)));
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistriesTrusted(Codec<T> codec) {
        return fromCodecWithRegistries(codec, () -> NbtAccounter.UNLIMITED);
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec) {
        return fromCodecWithRegistries(codec, () -> new NbtAccounter(2097152L));
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec, Supplier<NbtAccounter> sizeTracker) {
        StreamCodec<ByteBuf, Tag> tagStreamCodec = tagCodec(sizeTracker);
        return StreamCodec.of((buf, value) -> {
            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, buf.registryAccess());
            Tag tag = getOrThrow(codec.encodeStart(ops, value),
                    error -> new EncoderException("Failed to encode: " + error + " " + value));
            tagStreamCodec.encode(buf, tag);
        }, buf -> {
            Tag tag = tagStreamCodec.decode(buf);
            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, buf.registryAccess());
            return getOrThrow(codec.parse(ops, tag),
                    error -> new DecoderException("Failed to decode: " + error + " " + tag));
        });
    }

    static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<B, V> codec) {
        return StreamCodec.of((buf, value) -> {
            if (value.isPresent()) {
                buf.writeBoolean(true);
                codec.encode(buf, value.get());
            } else {
                buf.writeBoolean(false);
            }
        }, buf -> buf.readBoolean() ? Optional.of(codec.decode(buf)) : Optional.empty());
    }

    static int readCount(ByteBuf buf, int maxSize) {
        int count = friendly(buf).readVarInt();
        if (count > maxSize) {
            throw new DecoderException(count + " elements exceeded max size of: " + maxSize);
        }
        return count;
    }

    static void writeCount(ByteBuf buf, int size, int maxSize) {
        if (size > maxSize) {
            throw new EncoderException(size + " elements exceeded max size of: " + maxSize);
        }
        friendly(buf).writeVarInt(size);
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> elementCodec) {
        return collection(factory, elementCodec, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> elementCodec, int maxSize) {
        return StreamCodec.of((buf, value) -> {
            writeCount(buf, value.size(), maxSize);
            for (V element : value) {
                elementCodec.encode(buf, element);
            }
        }, buf -> {
            int count = readCount(buf, maxSize);
            C collection = factory.apply(Math.min(count, MAX_INITIAL_COLLECTION_SIZE));
            for (int i = 0; i < count; i++) {
                collection.add(elementCodec.decode(buf));
            }
            return collection;
        });
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> factory) {
        return codec -> collection(factory, codec);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return codec -> collection(ArrayList::new, codec);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int maxSize) {
        return codec -> collection(ArrayList::new, codec, maxSize);
    }

    static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> factory, StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec) {
        return map(factory, keyCodec, valueCodec, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> factory, StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec, int maxSize) {
        return StreamCodec.of((buf, value) -> {
            writeCount(buf, value.size(), maxSize);
            value.forEach((key, entryValue) -> {
                keyCodec.encode(buf, key);
                valueCodec.encode(buf, entryValue);
            });
        }, buf -> {
            int count = readCount(buf, maxSize);
            M map = factory.apply(Math.min(count, MAX_INITIAL_COLLECTION_SIZE));
            for (int i = 0; i < count; i++) {
                map.put(keyCodec.decode(buf), valueCodec.decode(buf));
            }
            return map;
        });
    }

    static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(StreamCodec<? super B, L> left, StreamCodec<? super B, R> right) {
        return StreamCodec.of((buf, value) -> value.ifLeft(leftValue -> {
            buf.writeBoolean(true);
            left.encode(buf, leftValue);
        }).ifRight(rightValue -> {
            buf.writeBoolean(false);
            right.encode(buf, rightValue);
        }), buf -> buf.readBoolean() ? Either.left(left.decode(buf)) : Either.right(right.decode(buf)));
    }

    static <T> StreamCodec<ByteBuf, T> idMapper(IntFunction<T> indexToValue, ToIntFunction<T> valueToIndex) {
        return StreamCodec.of(
                (buf, value) -> friendly(buf).writeVarInt(valueToIndex.applyAsInt(value)),
                buf -> indexToValue.apply(friendly(buf).readVarInt()));
    }

    /**
     * 1.20.1 的 {@link IdMap} 没有 {@code getIdOrThrow}，未登记的值会写成 -1，
     * 并在解码时由 {@code byIdOrThrow} 抛出。
     */
    static <T> StreamCodec<ByteBuf, T> idMapper(IdMap<T> idMap) {
        return idMapper(idMap::byIdOrThrow, idMap::getId);
    }

    private static <T, R> StreamCodec<RegistryFriendlyByteBuf, R> registry(ResourceKey<? extends Registry<T>> registryKey, Function<Registry<T>, IdMap<R>> registryTransformer) {
        return StreamCodec.of(
                (buf, value) -> friendly(buf).writeVarInt(registryTransformer.apply(buf.registryAccess().registryOrThrow(registryKey)).getId(value)),
                buf -> registryTransformer.apply(buf.registryAccess().registryOrThrow(registryKey)).byIdOrThrow(friendly(buf).readVarInt()));
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, T> registry(ResourceKey<? extends Registry<T>> registryKey) {
        return registry(registryKey, registry -> registry);
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderRegistry(ResourceKey<? extends Registry<T>> registryKey) {
        return registry(registryKey, Registry::asHolderIdMap);
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holder(ResourceKey<? extends Registry<T>> registryKey, StreamCodec<? super RegistryFriendlyByteBuf, T> directCodec) {
        return new StreamCodec<>() {
            private static final int DIRECT_HOLDER_ID = 0;

            private IdMap<Holder<T>> holderIdMap(RegistryFriendlyByteBuf buf) {
                return buf.registryAccess().registryOrThrow(registryKey).asHolderIdMap();
            }

            @Override
            public Holder<T> decode(RegistryFriendlyByteBuf buf) {
                int id = friendly(buf).readVarInt();
                return id == DIRECT_HOLDER_ID
                        ? Holder.direct(directCodec.decode(buf))
                        : holderIdMap(buf).byIdOrThrow(id - 1);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, Holder<T> value) {
                switch (value.kind()) {
                    case REFERENCE -> friendly(buf).writeVarInt(holderIdMap(buf).getId(value) + 1);
                    case DIRECT -> {
                        friendly(buf).writeVarInt(DIRECT_HOLDER_ID);
                        directCodec.encode(buf, value.value());
                    }
                }
            }
        };
    }

    static <T> StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>> holderSet(ResourceKey<? extends Registry<T>> registryKey) {
        return new StreamCodec<>() {
            private static final int NAMED_SET = -1;
            private final StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderCodec = holderRegistry(registryKey);

            @Override
            public HolderSet<T> decode(RegistryFriendlyByteBuf buf) {
                int size = friendly(buf).readVarInt() - 1;
                if (size == NAMED_SET) {
                    return buf.registryAccess().registryOrThrow(registryKey)
                            .getTag(TagKey.create(registryKey, friendly(buf).readResourceLocation()))
                            .orElseThrow();
                }
                List<Holder<T>> holders = new ArrayList<>(Math.min(size, MAX_INITIAL_COLLECTION_SIZE));
                for (int i = 0; i < size; i++) {
                    holders.add(holderCodec.decode(buf));
                }
                return HolderSet.direct(holders);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, HolderSet<T> value) {
                Optional<TagKey<T>> tagKey = value.unwrapKey();
                if (tagKey.isPresent()) {
                    friendly(buf).writeVarInt(NAMED_SET);
                    friendly(buf).writeResourceLocation(tagKey.get().location());
                } else {
                    friendly(buf).writeVarInt(value.size() + 1);
                    for (Holder<T> holder : value) {
                        holderCodec.encode(buf, holder);
                    }
                }
            }
        };
    }

    /** 1.20.1 的 DFU 没有 {@code getOrThrow(Function)} 重载，用 {@code resultOrPartial} 达到同样效果。 */
    private static <R> R getOrThrow(DataResult<R> result, Function<String, RuntimeException> errorFactory) {
        return result.resultOrPartial(message -> {
            throw errorFactory.apply(message);
        }).orElseThrow(() -> errorFactory.apply("Unknown error"));
    }

    /**
     * 把任意 {@link ByteBuf} 视作 {@link FriendlyByteBuf}；本身已是 FriendlyByteBuf 时原样返回。
     *
     * <p>原版把这些读写做成了 {@code ByteBuf} 上的静态方法，1.20.1 只有实例方法，故需要此适配。
     */
    static FriendlyByteBuf friendly(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buf);
    }

    /** 写入带前导标记的标签，{@code null} 写 0。与 1.20.1 的 {@code FriendlyByteBuf.writeNbt} 同格式。 */
    private static void writeTag(ByteBuf buf, @Nullable Tag tag) {
        if (tag == null) {
            buf.writeByte(0);
            return;
        }
        try {
            buf.writeByte(tag.getId());
            buf.writeShort(0);
            tag.write(new ByteBufOutputStream(buf));
        } catch (IOException exception) {
            throw new EncoderException(exception);
        }
    }

    /**
     * 读取带前导标记的标签，前导为 0 时返回 {@code null}。
     *
     * <p>1.20.1 的 {@code FriendlyByteBuf.readNbt} 只认 {@link CompoundTag} 根，而这里的编解码器
     * 需要任意 {@link Tag}，故按 {@code NbtIo} 的格式自行读取。
     */
    @Nullable
    private static Tag readTag(ByteBuf buf, NbtAccounter sizeTracker) {
        DataInput input = new ByteBufInputStream(buf);
        try {
            byte id = input.readByte();
            if (id == 0) {
                return null;
            }
            input.readUTF();
            return TagTypes.getType(id).load(input, 0, sizeTracker);
        } catch (IOException exception) {
            throw new DecoderException(exception);
        }
    }
}
