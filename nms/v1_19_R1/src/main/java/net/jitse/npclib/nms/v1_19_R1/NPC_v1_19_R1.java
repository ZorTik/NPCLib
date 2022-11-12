package net.jitse.npclib.nms.v1_19_R1;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.jitse.npclib.NPCLib;
import net.jitse.npclib.api.skin.Skin;
import net.jitse.npclib.api.state.NPCAnimation;
import net.jitse.npclib.api.state.NPCSlot;
import net.jitse.npclib.hologram.Hologram;
import net.jitse.npclib.hologram.HologramProvider;
import net.jitse.npclib.internal.MinecraftVersion;
import net.jitse.npclib.internal.NPCBase;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class NPC_v1_19_R1 extends NPCBase {
    private final ProtocolManager protocol;

    private PacketContainer teamRegisterPacket = null;
    private PacketPlayOutScoreboardTeam teamAddEntityPacket = null;
    private PacketContainer playerInfoRemovePacket = null;
    private PacketContainer headRotationPacket = null;
    private PacketContainer spawnPacket = null;
    private PacketPlayOutEntityDestroy destroyPacket = null;

    public NPC_v1_19_R1(NPCLib instance, List<String> text) {
        super(instance, text);
        if(Bukkit.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            throw new RuntimeException("ProtocolLib is required for NPCLib to work on 1.17+.");
        }
        this.protocol = ProtocolLibrary.getProtocolManager();
        buildPackets();
    }

    @Override
    public void updateSkin(Skin skin) {
        for(Player player : Bukkit.getOnlinePlayers()) {
            updateSkin(skin, player);
        }
    }

    public void updateSkin(Player player) {
        PropertyMap map = gameProfile.getProperties();
        Property textures = map.get("textures").stream().findFirst().get();
        updateSkin(new Skin(textures.getValue(), textures.getSignature()), player);
        sendAnimationPacket(player, NPCAnimation.SWING_MAINHAND);
    }

    public void updateSkin(Skin skin, Player player) {
        WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
        profile.getProperties().get("textures").clear();
        profile.getProperties().put("textures", new WrappedSignedProperty("textures", skin.getValue(), skin.getSignature()));
        doInitializationCheck();

        protocol.sendServerPacket(player, playerInfoRemovePacket);
        ((CraftPlayer) player).getHandle().b.a(destroyPacket);
        protocol.sendServerPacket(player, buildPlayerInfoAdd(name, profile));
        protocol.sendServerPacket(player, spawnPacket);
        protocol.sendServerPacket(player, headRotationPacket);
        sendAnimationPacket(player, NPCAnimation.SWING_MAINHAND);
    }

    @Override
    public void createPackets() {
        // Already handled in buildPackets.
    }

    @Override
    public void createPackets(Player player) {
        // Already handled in buildPackets.
    }

    private void buildPackets() {
        Bukkit.getScheduler().runTask(getInstance().getPlugin(), () -> {
            buildTeamRegister(name);
            buildTeamAddEntity(name);
            buildPlayerSpawn();
            buildHeadRotation();
            buildPlayerInfoRemove(name);

            this.destroyPacket = new PacketPlayOutEntityDestroy(this.entityId);
        });
    }

    @Override
    public void sendShowPackets(Player player) {
        sendShowPackets(player, true);
    }

    private void sendShowPackets(Player player, boolean skin) {
        doInitializationCheck();
        if(hasTeamRegistered.add(player.getUniqueId())) {
            protocol.sendServerPacket(player, teamRegisterPacket);
            ((CraftPlayer) player).getHandle().b.a(teamAddEntityPacket);
        }
        protocol.sendServerPacket(player, buildPlayerInfoAdd(name, gameProfile));
        protocol.sendServerPacket(player, spawnPacket);
        protocol.sendServerPacket(player, headRotationPacket);

        sendMetadataPacket(player);
        if(skin)
            updateSkin(player);

        getHologram(player).show(player);
        Bukkit.getScheduler().runTaskLater(instance.getPlugin(), () ->
                protocol.sendServerPacket(player, playerInfoRemovePacket), 200);
    }

    @Override
    public void sendHidePackets(Player player) {
        doInitializationCheck();
        ((CraftPlayer) player).getHandle().b.a(destroyPacket);
        protocol.sendServerPacket(player, playerInfoRemovePacket);
        getHologram(player).hide(player);
    }

    @Override
    public void sendMetadataPacket(Player player) {
        doInitializationCheck();
        // TODO
    }

    @Override
    public void sendEquipmentPacket(Player player, NPCSlot slot, boolean auto) {
        doInitializationCheck();
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, entityId);
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> stackPairList = Lists.newArrayList();
        stackPairList.add(new Pair<>(EnumWrappers.ItemSlot.valueOf(slot.getNmsName()), getItem(slot)));
        packet.getSlotStackPairLists().write(0, stackPairList);
        protocol.sendServerPacket(player, packet);
    }

    @Override
    public void sendAnimationPacket(Player player, NPCAnimation animation) {
        doInitializationCheck();
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ANIMATION);
        packet.getIntegers().write(0, entityId);
        packet.getIntegers().write(1, animation.getId());
        protocol.sendServerPacket(player, packet);
    }

    @Override
    public void sendHeadRotationPackets(Location location) {
        doInitializationCheck();
        // TODO: Look at location.
    }

    @Override
    protected Hologram getHologram(Player player) {
        Hologram hologram = super.getHologram(player);
        if(hologram == null) {
            hologram = HologramProvider.build(this, MinecraftVersion.V1_19_R1, location.clone().add(0, 0.5, 0), getText());
            playerHologram.put(player.getUniqueId(), hologram);
        }
        return hologram;
    }

    private void buildTeamRegister(String name) {
        teamRegisterPacket = protocol.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);

        //teamRegisterPacket.getModifier().writeDefaults();
        teamRegisterPacket.getStrings().write(0, name);
        teamRegisterPacket.getIntegers().write(0, 0);
        teamRegisterPacket.getOptionalStructures().write(0, Optional.of(
                InternalStructure.getConverter().getSpecific(v1_19Util.prepareScoreboardMeta(name))
        ));
    }

    private void buildTeamAddEntity(String name) {
        try {
            Constructor<PacketPlayOutScoreboardTeam> constructor = PacketPlayOutScoreboardTeam.class.getDeclaredConstructor(String.class, int.class, Optional.class, Collection.class);
            constructor.setAccessible(true);
            teamAddEntityPacket = constructor.newInstance(name, 3, Optional.empty(), Collections.singletonList(name));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        /*teamAddEntityPacket = protocol.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        //teamRegisterPacket.getModifier().writeDefaults();
        teamAddEntityPacket.getStrings().write(0, name);
        teamAddEntityPacket.getIntegers().write(0, 3);

        List<PlayerInfoData> dataList = new ArrayList<>();
        // NativeGameMode throws error while trying to find the right EnumGamemode for current version on 09/11 2022.
        // I made custom ProtocolLib fork.
        dataList.add(new PlayerInfoData(new WrappedGameProfile(uuid, name), 0, EnumWrappers.NativeGameMode.NONE, WrappedChatComponent.fromText(name)));
        teamAddEntityPacket.getPlayerInfoDataLists().write(0, dataList);*/
    }

    private PacketContainer buildPlayerInfoAdd(String name, GameProfile gameProfile) {
        return buildPlayerInfoAdd(name, new WrappedGameProfile(gameProfile.getId(), gameProfile.getName()));
    }

    private PacketContainer buildPlayerInfoAdd(String name, WrappedGameProfile gameProfile) {
        PlayerInfoData infoData = new PlayerInfoData(
                gameProfile,
                0, EnumWrappers.NativeGameMode.NOT_SET,
                WrappedChatComponent.fromJson("{\"text\":\"[NPC] " + name + "\",\"color\":\"dark_gray\"}"));
        PacketContainer playerInfoAddPacket = protocol.createPacket(PacketType.Play.Server.PLAYER_INFO);
        playerInfoAddPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        playerInfoAddPacket.getPlayerInfoDataLists().write(0, Collections.singletonList(infoData));
        return playerInfoAddPacket;
    }

    private void buildPlayerInfoRemove(String name) {
        PlayerInfoData infoData = new PlayerInfoData(
                new WrappedGameProfile(gameProfile.getId(), gameProfile.getName()),
                0, EnumWrappers.NativeGameMode.NOT_SET,
                WrappedChatComponent.fromJson("{\"text\":\"[NPC] " + name + "\",\"color\":\"dark_gray\"}"));
        playerInfoRemovePacket = protocol.createPacket(PacketType.Play.Server.PLAYER_INFO);
        playerInfoRemovePacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        playerInfoRemovePacket.getPlayerInfoDataLists().write(0,
                Collections.singletonList(infoData));
    }

    private void buildPlayerSpawn() {
        this.spawnPacket = protocol.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        spawnPacket.getIntegers().write(0, entityId);
        spawnPacket.getUUIDs().write(0, gameProfile.getId());
        spawnPacket.getDoubles().write(0, location.getX());
        spawnPacket.getDoubles().write(1, location.getY());
        spawnPacket.getDoubles().write(2, location.getZ());
        spawnPacket.getBytes().write(0, (byte) (location.getYaw() * 256.0F / 360.0F));
        spawnPacket.getBytes().write(1, (byte) (location.getPitch() * 256.0F / 360.0F));
    }

    private void buildHeadRotation() {
        this.headRotationPacket = protocol.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        headRotationPacket.getIntegers().write(0, entityId);
        headRotationPacket.getBytes().write(0, (byte) (location.getYaw() * 256.0F / 360.0F));
    }

    private void doInitializationCheck() {
        if(teamRegisterPacket == null || playerInfoRemovePacket == null || headRotationPacket == null || spawnPacket == null || destroyPacket == null) {
            createPackets();
        }
    }

}
