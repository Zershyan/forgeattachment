package io.zershyan.forgeattachment.api;

import com.mojang.datafixers.util.Either;
import io.netty.buffer.ByteBuf;
import io.zershyan.forgeattachment.network.RegistryFriendlyByteBuf;
import io.zershyan.forgeattachment.network.codec.ByteBufCodecs;
import io.zershyan.forgeattachment.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 对外稳定的流编解码器集合：常用类的 {@link StreamCodec} 与集合包装方法。
 *
 * <p>基础类型（INT、BOOL、字符串、NBT 等）见 {@link ByteBufCodecs}，本类只收拢下游经常需要的类。
 * 集合包装方法转发到 {@link ByteBufCodecs}，不重复实现。
 */
public final class StreamCodecs {
    /**
     * 资源路径。
     */
    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = StreamCodec.of(
            (buf, value) -> friendly(buf).writeResourceLocation(value),
            buf -> friendly(buf).readResourceLocation());

    /**
     * 物品堆。1.20.1 的 {@code FriendlyByteBuf} 自带空堆与计数的处理。
     */
    public static final StreamCodec<ByteBuf, ItemStack> ITEM_STACK = StreamCodec.of(
            (buf, value) -> friendly(buf).writeItem(value),
            buf -> friendly(buf).readItem());

    /**
     * UUID。
     */
    public static final StreamCodec<ByteBuf, UUID> UUID = StreamCodec.of(
            (buf, value) -> friendly(buf).writeUUID(value),
            buf -> friendly(buf).readUUID());

    /**
     * 方块坐标。
     */
    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = StreamCodec.of(
            (buf, value) -> friendly(buf).writeBlockPos(value),
            buf -> friendly(buf).readBlockPos());

    /**
     * 区块坐标。
     */
    public static final StreamCodec<ByteBuf, ChunkPos> CHUNK_POS = StreamCodec.of(
            (buf, value) -> friendly(buf).writeChunkPos(value),
            buf -> friendly(buf).readChunkPos());

    /**
     * 聊天文本。
     */
    public static final StreamCodec<ByteBuf, Component> COMPONENT = StreamCodec.of(
            (buf, value) -> friendly(buf).writeComponent(value),
            buf -> friendly(buf).readComponent());

    /**
     * 任意 NBT 标签，带长度上限。
     */
    public static final StreamCodec<ByteBuf, Tag> TAG = ByteBufCodecs.TAG;

    /**
     * NBT 复合标签，带长度上限。
     */
    public static final StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = ByteBufCodecs.COMPOUND_TAG;

    /**
     * 声音事件。
     *
     * <p>1.21.1 的声音事件流编解码器还会带上固定音距，1.20.1 侧按注册名同步；
     * 需要精确保留音距的场合请自行组合。
     */
    public static final StreamCodec<ByteBuf, SoundEvent> SOUND_EVENT = RESOURCE_LOCATION.map(
            SoundEvent::createVariableRangeEvent, SoundEvent::getLocation);

    /**
     * 带注册表引用的声音事件。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<SoundEvent>> SOUND_EVENT_HOLDER =
            ByteBufCodecs.holder(Registries.SOUND_EVENT, SOUND_EVENT);

    /**
     * 元素列表。
     *
     * @param elementCodec 元素编解码器
     */
    public static <B extends ByteBuf, V> StreamCodec<B, List<V>> list(StreamCodec<? super B, V> elementCodec) {
        return ByteBufCodecs.collection(ArrayList::new, elementCodec);
    }

    /**
     * 有长度上限的元素列表。
     *
     * @param elementCodec 元素编解码器
     * @param maxSize      最大元素数
     */
    public static <B extends ByteBuf, V> StreamCodec<B, List<V>> list(StreamCodec<? super B, V> elementCodec, int maxSize) {
        return ByteBufCodecs.collection(ArrayList::new, elementCodec, maxSize);
    }

    /**
     * 元素集合，不保证顺序。
     *
     * @param elementCodec 元素编解码器
     */
    public static <B extends ByteBuf, V> StreamCodec<B, Set<V>> set(StreamCodec<? super B, V> elementCodec) {
        return ByteBufCodecs.collection(HashSet::new, elementCodec);
    }

    /**
     * 有长度上限的元素集合，不保证顺序。
     *
     * @param elementCodec 元素编解码器
     * @param maxSize      最大元素数
     */
    public static <B extends ByteBuf, V> StreamCodec<B, Set<V>> set(StreamCodec<? super B, V> elementCodec, int maxSize) {
        return ByteBufCodecs.collection(HashSet::new, elementCodec, maxSize);
    }

    /**
     * 键值映射。
     *
     * @param keyCodec   键编解码器
     * @param valueCodec 值编解码器
     */
    public static <B extends ByteBuf, K, V> StreamCodec<B, Map<K, V>> map(StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec) {
        return ByteBufCodecs.map(HashMap::new, keyCodec, valueCodec);
    }

    /**
     * 有长度上限的键值映射。
     *
     * @param keyCodec   键编解码器
     * @param valueCodec 值编解码器
     * @param maxSize    最大条目数
     */
    public static <B extends ByteBuf, K, V> StreamCodec<B, Map<K, V>> map(StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec, int maxSize) {
        return ByteBufCodecs.map(HashMap::new, keyCodec, valueCodec, maxSize);
    }

    /**
     * 二者其一，左侧优先。
     *
     * @param left  左分支编解码器
     * @param right 右分支编解码器
     */
    public static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(StreamCodec<? super B, L> left, StreamCodec<? super B, R> right) {
        return ByteBufCodecs.either(left, right);
    }

    /**
     * 可选值，空值也会写入一个标记位。
     *
     * @param codec 值编解码器
     */
    public static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<B, V> codec) {
        return ByteBufCodecs.optional(codec);
    }

    private static FriendlyByteBuf friendly(ByteBuf buf) {
        return ByteBufCodecs.friendly(buf);
    }

    private StreamCodecs() {}
}
