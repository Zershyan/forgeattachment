package io.zershyan.forgeattachment;

import com.mojang.authlib.GameProfile;
import io.zershyan.forgeattachment.attachment.AttachmentHolder;
import io.zershyan.forgeattachment.attachment.IAttachmentHolder;
import io.zershyan.forgeattachment.example.ExampleAttachments;
import io.zershyan.forgeattachment.network.ForgeAttachmentNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.WorldServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge 1.12.2 版入口。
 */
@Mod(
        modid = ForgeAttachment.MODID,
        name = ForgeAttachment.NAME,
        version = ForgeAttachment.VERSION,
        acceptedMinecraftVersions = "[1.12,1.13)"
)
public class ForgeAttachment {
    public static final String MODID = "forgeattachment";
    public static final String NAME = "ForgeAttachment";
    public static final String VERSION = "0.0.1";

    private static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ForgeAttachmentNetwork.init();
        ExampleAttachments.init();
        boolean entityInjected = IAttachmentHolder.class.isAssignableFrom(Entity.class);
        boolean tileInjected = IAttachmentHolder.class.isAssignableFrom(TileEntity.class);
        if (entityInjected && tileInjected)
            LOGGER.info("Mixin injection OK: Entity and TileEntity implement IAttachmentHolder");
        else
            LOGGER.error("Mixin injection FAILED: Entity={}, TileEntity={}", entityInjected, tileInjected);
    }

    /**
     * 真实对象路径自检：必须经过 Entity/TileEntity 自己的 writeToNBT，才能覆盖到 Mixin 注入点。
     */
    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null)
            return;
        World world = server.getEntityWorld();
        verify("entity", roundTripEntity(world), "entity-path");
        verify("living", roundTripLiving(world), "living-path");
        verify("tileentity", roundTripTileEntity(), "tile-path");
        verify("player", roundTripPlayer(server, world), "player-path");
    }

    private String roundTripEntity(World world) {
        EntityItem source = new EntityItem(world, 0, 100, 0, new ItemStack(Items.STICK));
        ((IAttachmentHolder) source).setData(ExampleAttachments.NOTE, "entity-path");
        NBTTagCompound tag = new NBTTagCompound();
        source.writeToNBT(tag);
        EntityItem restored = new EntityItem(world);
        restored.readFromNBT(tag);
        return ((IAttachmentHolder) restored).getData(ExampleAttachments.NOTE);
    }

    /**
     * EntityLivingBase 子类：它重写了 writeEntityToNBT 且不调用 super，是原先注入点失效的重灾区。
     */
    private String roundTripLiving(World world) {
        EntitySheep source = new EntitySheep(world);
        ((IAttachmentHolder) source).setData(ExampleAttachments.NOTE, "living-path");
        NBTTagCompound tag = new NBTTagCompound();
        source.writeToNBT(tag);
        EntitySheep restored = new EntitySheep(world);
        restored.readFromNBT(tag);
        return ((IAttachmentHolder) restored).getData(ExampleAttachments.NOTE);
    }

    private String roundTripPlayer(MinecraftServer server, World world) {
        try {
            WorldServer worldServer = (WorldServer) world;
            GameProfile profile = new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000001"), "AttachmentTester");
            EntityPlayerMP source = new EntityPlayerMP(server, worldServer, profile, new PlayerInteractionManager(worldServer));
            ((IAttachmentHolder) source).setData(ExampleAttachments.NOTE, "player-path");
            NBTTagCompound tag = new NBTTagCompound();
            source.writeToNBT(tag);
            EntityPlayerMP restored = new EntityPlayerMP(server, worldServer, profile, new PlayerInteractionManager(worldServer));
            restored.readFromNBT(tag);
            return ((IAttachmentHolder) restored).getData(ExampleAttachments.NOTE);
        } catch (Throwable error) {
            LOGGER.warn("Player round-trip skipped: {}", error.toString());
            return "skipped";
        }
    }

    private String roundTripTileEntity() {
        TileEntityChest source = new TileEntityChest();
        ((IAttachmentHolder) source).setData(ExampleAttachments.NOTE, "tile-path");
        NBTTagCompound tag = new NBTTagCompound();
        source.writeToNBT(tag);
        TileEntityChest restored = new TileEntityChest();
        restored.readFromNBT(tag);
        return ((IAttachmentHolder) restored).getData(ExampleAttachments.NOTE);
    }

    private void verify(String path, String actual, String expected) {
        if (expected.equals(actual))
            LOGGER.info("Attachment {} round-trip OK: {}", path, actual);
        else
            LOGGER.error("Attachment {} round-trip FAILED: expected {}, got {}", path, expected, actual);
    }

    /**
     * 纯内存自检，仅覆盖 holder 本身，不代表实体路径可用。
     */
    @SuppressWarnings("unused")
    private void holderSelfCheck() {
        AttachmentHolder holder = new AttachmentHolder();
        holder.setData(ExampleAttachments.NOTE, "round-trip");
        NBTTagCompound tag = new NBTTagCompound();
        holder.writeToNBT(tag);
        AttachmentHolder restored = new AttachmentHolder();
        restored.readFromNBT(tag);
        verify("holder", restored.getData(ExampleAttachments.NOTE), "round-trip");
    }
}
