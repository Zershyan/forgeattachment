**ForgeAttachment** ports the NeoForge 1.21.1 data attachment system to Forge 1.20.1 and exposes it to other mods as a stable API. It is an unofficial port, not affiliated with or endorsed by the NeoForge team.

Data attachments let a mod store extra data **on existing game objects** (entities, block entities) without editing their classes or maintaining a separate map keyed by object identity. Register an `AttachmentType`, then read and write it on any object that supports attachments.

## Features

- Register your own attachment types with default values, serializers and copy-on-death behaviour
- Attach data to entities and block entities
- Automatic client synchronization, opt-in per attachment type
- Both serialization paths from 1.21.1: `Codec` for saved data and `StreamCodec` for network
- Lifecycle events for attach, detach and sync
- Pure library: no items, blocks or world content of its own

## For developers

```java
public static final AttachmentType<ExampleData> EXAMPLE = AttachmentType
        .builder(() -> new ExampleData("", 0))
        .serialize(ExampleData.CODEC)
        .copyOnDeath()
        .sync(ExampleData.STREAM_CODEC)
        .build();

ExampleData data = ForgeAttachmentApi.getData(entity, EXAMPLE);
ForgeAttachmentApi.setData(entity, EXAMPLE, data);
```

A complete registration example ships in the `example` package of the source tree.

## Requirements

- Minecraft 1.20.1
- Forge 47.x
- Java 17

## Source

https://github.com/Zershyan/forgeattachment

## Credits

API design ported from NeoForge 1.21.1's attachment system; thanks to the NeoForge team for the original design.

---

**ForgeAttachment** 把 NeoForge 1.21.1 的数据附件（Data Attachment）系统移植到 Forge 1.20.1，并以稳定的 API 开放给其他模组使用。本项目为非官方移植，与 NeoForge 团队无隶属关系，也未获其背书。

数据附件允许模组把额外数据**直接挂在已有的游戏对象上**（实体、方块实体），既不用改动原类，也不用自己维护一张以对象为键的映射表。注册一个 `AttachmentType` 之后，就能在任意支持附件的对象上读写它。

## 特性

- 自定义附件类型注册：默认值、序列化器与死亡复制行为
- 实体与方块实体均可挂载附件
- 自动客户端同步，可按附件类型单独开启
- 完整保留 1.21.1 的两条序列化路径：`Codec`（存档）与 `StreamCodec`（网络）
- 附件挂载、移除与同步的生命周期事件
- 纯库模组：不包含任何物品、方块或世界内容

## 面向开发者

```java
public static final AttachmentType<ExampleData> EXAMPLE = AttachmentType
        .builder(() -> new ExampleData("", 0))
        .serialize(ExampleData.CODEC)
        .copyOnDeath()
        .sync(ExampleData.STREAM_CODEC)
        .build();

ExampleData data = ForgeAttachmentApi.getData(entity, EXAMPLE);
ForgeAttachmentApi.setData(entity, EXAMPLE, data);
```

源码 `example` 包内含完整的注册示例。

## 环境要求

- Minecraft 1.20.1
- Forge 47.x
- Java 17

## 源码

https://github.com/Zershyan/forgeattachment

## 鸣谢

API 设计移植自 NeoForge 1.21.1 的附件系统，感谢 NeoForge 团队的原始设计。
