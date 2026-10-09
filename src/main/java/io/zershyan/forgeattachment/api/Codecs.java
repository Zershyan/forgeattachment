package io.zershyan.forgeattachment.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 对外稳定的数据编解码器集合：常用类的 {@link Codec} 与集合包装方法。
 *
 * <p>对应流侧的 {@link StreamCodecs}。1.20.1 没有为聊天文本与任意 NBT 标签提供 {@code Codec}，
 * 这两项只在 {@link StreamCodecs} 中提供。
 */
public final class Codecs {
    /**
     * 资源路径。
     */
    public static final Codec<ResourceLocation> RESOURCE_LOCATION = ResourceLocation.CODEC;

    /**
     * 物品堆。
     */
    public static final Codec<ItemStack> ITEM_STACK = ItemStack.CODEC;

    /**
     * UUID。
     */
    public static final Codec<UUID> UUID = UUIDUtil.CODEC;

    /**
     * 方块坐标。
     */
    public static final Codec<BlockPos> BLOCK_POS = BlockPos.CODEC;

    /**
     * 区块坐标。1.20.1 未提供，按其长整型压缩形式表达。
     */
    public static final Codec<ChunkPos> CHUNK_POS = Codec.LONG.xmap(ChunkPos::new, ChunkPos::toLong);

    /**
     * 复合标签。
     */
    public static final Codec<CompoundTag> COMPOUND_TAG = CompoundTag.CODEC;

    /**
     * 聊天文本，以 JSON 形式表达。
     */
    public static final Codec<Component> COMPONENT = Codec.STRING.comapFlatMap(
            json -> Optional.ofNullable(Component.Serializer.fromJson(json))
                    .<DataResult<Component>>map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Invalid chat component: " + json)),
            Component.Serializer::toJson);

    /**
     * 声音事件本体。
     */
    public static final Codec<SoundEvent> SOUND_EVENT = SoundEvent.DIRECT_CODEC;

    /**
     * 带注册表引用的声音事件。
     */
    public static final Codec<Holder<SoundEvent>> SOUND_EVENT_HOLDER = SoundEvent.CODEC;

    /**
     * 元素列表。
     *
     * @param elementCodec 元素编解码器
     */
    public static <E> Codec<List<E>> list(Codec<E> elementCodec) {
        return Codec.list(elementCodec);
    }

    /**
     * 元素集合，不保证顺序。
     *
     * @param elementCodec 元素编解码器
     */
    public static <E> Codec<Set<E>> set(Codec<E> elementCodec) {
        return Codec.list(elementCodec).xmap(Sets::newHashSet, Lists::newArrayList);
    }

    /**
     * 键值映射。
     *
     * @param keyCodec   键编解码器，其编码结果必须是字符串形式
     * @param valueCodec 值编解码器
     */
    public static <K, V> Codec<Map<K, V>> map(Codec<K> keyCodec, Codec<V> valueCodec) {
        return Codec.unboundedMap(keyCodec, valueCodec);
    }

    /**
     * 二者其一，左侧优先。
     *
     * @param left  左分支编解码器
     * @param right 右分支编解码器
     */
    public static <L, R> Codec<Either<L, R>> either(Codec<L> left, Codec<R> right) {
        return Codec.either(left, right);
    }

    private Codecs() {}
}
