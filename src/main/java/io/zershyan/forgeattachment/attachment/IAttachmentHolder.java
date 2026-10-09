package io.zershyan.forgeattachment.attachment;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 可持有数据附件的对象。
 *
 * <p>与 NeoForge 1.21.1 的 {@code IAttachmentHolder} 一致。
 */
public interface IAttachmentHolder {
    /**
     * @return 该 holder 是否持有任何数据附件
     */
    boolean hasAttachments();

    /**
     * @return 该 holder 是否持有指定类型的数据附件
     */
    boolean hasData(AttachmentType<?> type);

    /**
     * @return 该 holder 是否持有指定类型的数据附件
     */
    default <T> boolean hasData(Supplier<AttachmentType<T>> type) {
        return hasData(type.get());
    }

    /**
     * {@return 指定类型的数据附件；若不存在则存入默认值并返回}
     */
    <T> T getData(AttachmentType<T> type);

    /**
     * {@return 指定类型的数据附件；若不存在则存入默认值并返回}
     */
    default <T> T getData(Supplier<AttachmentType<T>> type) {
        return getData(type.get());
    }

    /**
     * {@return 指定类型的数据附件；若不存在返回空 Optional}
     */
    default <T> Optional<T> getExistingData(AttachmentType<T> type) {
        return Optional.ofNullable(getExistingDataOrNull(type));
    }

    /**
     * {@return 指定类型的数据附件；若不存在返回空 Optional}
     */
    default <T> Optional<T> getExistingData(Supplier<AttachmentType<T>> type) {
        return getExistingData(type.get());
    }

    /**
     * @return 已存在的指定类型数据附件，若不存在则为 null
     */
    @Nullable
    <T> T getExistingDataOrNull(AttachmentType<T> type);

    /**
     * @return 已存在的指定类型数据附件，若不存在则为 null
     */
    @Nullable
    default <T> T getExistingDataOrNull(Supplier<AttachmentType<T>> type) {
        return getExistingDataOrNull(type.get());
    }

    /**
     * 设置指定类型的数据附件。
     *
     * @return 该类型此前的值，若原本不存在则为 null
     */
    <T> @Nullable T setData(AttachmentType<T> type, T data);

    /**
     * 设置指定类型的数据附件。
     *
     * @return 该类型此前的值，若原本不存在则为 null
     */
    default <T> @Nullable T setData(Supplier<AttachmentType<T>> type, T data) {
        return setData(type.get(), data);
    }

    /**
     * 移除指定类型的数据附件。
     *
     * @return 被移除的值，若原本不存在则为 null
     */
    <T> @Nullable T removeData(AttachmentType<T> type);

    /**
     * 移除指定类型的数据附件。
     *
     * @return 被移除的值，若原本不存在则为 null
     */
    default <T> @Nullable T removeData(Supplier<AttachmentType<T>> type) {
        return removeData(type.get());
    }

    /**
     * 把指定类型的数据附件同步给所有相关客户端。
     *
     * <p>若当前不存在该类型的附件，则把移除操作同步给客户端。
     */
    default void syncData(AttachmentType<?> type) {}

    /**
     * 把指定类型的数据附件同步给所有相关客户端。
     */
    default void syncData(Supplier<? extends AttachmentType<?>> type) {
        syncData(type.get());
    }
}