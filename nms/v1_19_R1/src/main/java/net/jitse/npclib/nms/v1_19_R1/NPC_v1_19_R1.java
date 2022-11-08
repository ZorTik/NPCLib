package net.jitse.npclib.nms.v1_19_R1;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import com.mojang.authlib.GameProfile;
import net.jitse.npclib.NPCLib;
import net.jitse.npclib.api.skin.Skin;
import net.jitse.npclib.api.state.NPCAnimation;
import net.jitse.npclib.api.state.NPCSlot;
import net.jitse.npclib.hologram.Hologram;
import net.jitse.npclib.hologram.HologramProvider;
import net.jitse.npclib.internal.MinecraftVersion;
import net.jitse.npclib.internal.NPCBase;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.comphenix.protocol.wrappers.WrappedChatComponent.fromLegacyText;

public class NPC_v1_19_R1 extends NPCBase {
    private final ProtocolManager protocol;

    private PacketContainer teamRegisterPacket = null;
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
    }

    @Override
    public void updateSkin(Skin skin) {
        WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
        profile.getProperties().get("textures").clear();
        profile.getProperties().put("textures", new WrappedSignedProperty("textures", skin.getValue(), skin.getSignature()));
        for(Player player : Bukkit.getOnlinePlayers()) {
            protocol.sendServerPacket(player, playerInfoRemovePacket);
            ((CraftPlayer) player).getHandle().b.a(destroyPacket);
            protocol.sendServerPacket(player, buildPlayerInfoAdd(name, profile));
            protocol.sendServerPacket(player, spawnPacket);
        }
    }

    @Override
    public void createPackets() {
        Bukkit.getOnlinePlayers().forEach(this::createPackets);
    }

    @Override
    public void createPackets(Player player) {
        buildTeamRegister(name);
        buildPlayerSpawn();
        buildHeadRotation();
        buildPlayerInfoRemove(name);

        this.destroyPacket = new PacketPlayOutEntityDestroy(this.entityId);
    }

    @Override
    public void sendShowPackets(Player player) {
        if(hasTeamRegistered.add(player.getUniqueId())) {
            protocol.sendServerPacket(player, teamRegisterPacket);
        }
        protocol.sendServerPacket(player, buildPlayerInfoAdd(name, gameProfile));
        protocol.sendServerPacket(player, spawnPacket);
        protocol.sendServerPacket(player, headRotationPacket);

        sendMetadataPacket(player);

        getHologram(player).show(player);
        Bukkit.getScheduler().runTaskLater(instance.getPlugin(), () ->
                protocol.sendServerPacket(player, playerInfoRemovePacket), 200);
    }

    @Override
    public void sendHidePackets(Player player) {
        ((CraftPlayer) player).getHandle().b.a(destroyPacket);
        protocol.sendServerPacket(player, playerInfoRemovePacket);
        getHologram(player).hide(player);
    }

    @Override
    public void sendMetadataPacket(Player player) {
        // TODO
    }

    @Override
    public void sendEquipmentPacket(Player player, NPCSlot slot, boolean auto) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, entityId);
        packet.getItemSlots().write(0, EnumWrappers.ItemSlot.valueOf(slot.getNmsName()));
        packet.getItemModifier().write(0, getItem(slot));
        protocol.sendServerPacket(player, packet);
    }

    @Override
    public void sendAnimationPacket(Player player, NPCAnimation animation) {
        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ANIMATION);
        packet.getIntegers().write(0, entityId);
        packet.getIntegers().write(1, animation.getId());
        protocol.sendServerPacket(player, packet);
    }

    @Override
    public void sendHeadRotationPackets(Location location) {
        // TODO: Look at location.
    }

    @Override
    protected Hologram getHologram(Player player) {
        Hologram hologram = super.getHologram(player);
        if(hologram == null) {
            hologram = HologramProvider.build(this, MinecraftVersion.V1_19_R1, getLocation(), getText());
            playerHologram.put(player.getUniqueId(), hologram);
        }
        return hologram;
    }

    private void buildTeamRegister(String name) {
        teamRegisterPacket = protocol.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);

        teamRegisterPacket.getBytes().write(0, (byte) 0x01);
        teamRegisterPacket.getStrings().write(0, name);
        teamRegisterPacket.getStrings().write(1, "never");
        teamRegisterPacket.getStrings().write(2, "always");
        teamRegisterPacket.getStrings().write(3, "");
        teamRegisterPacket.getStrings().write(4, "");
        teamRegisterPacket.getIntegers().write(0, 21);
        teamRegisterPacket.getIntegers().write(1, 1);
        teamRegisterPacket.getStringArrays().write(0, new String[]{name});
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
        //spawnPacket.getIntegers().write(1, (int) EntityType.PLAYER.getTypeId());
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

}
