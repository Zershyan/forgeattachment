package io.zershyan.forgeattachment.util.mixin;

import io.zershyan.forgeattachment.attachment.AttachmentHolder;

/**
 * {@code BlockEntity} 的 mixin 接口：暴露附件存储。
 */
public interface IMixinBlockEntity {
    AttachmentHolder forgeattachment$attachmentHolder();
}