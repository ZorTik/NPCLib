package net.jitse.npclib.nms.v1_19_R1;

import com.comphenix.tinyprotocol.Reflection;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.scoreboard.CraftScoreboard;

public final class v1_19Util {

    public static PacketPlayOutScoreboardTeam.b prepareScoreboardMeta(String name) {
        PacketPlayOutScoreboardTeam.b b = new PacketPlayOutScoreboardTeam.b(new ScoreboardTeam(((CraftScoreboard) Bukkit.getScoreboardManager().getNewScoreboard()).getHandle(), name));
        Class<? extends PacketPlayOutScoreboardTeam.b> clazz = b.getClass();
        Reflection.getField(clazz, IChatBaseComponent.class, 0).set(b, IChatBaseComponent.a(""));
        Reflection.getField(clazz, IChatBaseComponent.class, 1).set(b, IChatBaseComponent.a(""));
        Reflection.getField(clazz, IChatBaseComponent.class, 2).set(b, IChatBaseComponent.a(""));
        Reflection.getField(clazz, String.class, 0).set(b, "never");
        Reflection.getField(clazz, String.class, 1).set(b, "always");
        Reflection.getField(clazz, EnumChatFormat.class, 0).set(b, EnumChatFormat.valueOf("WHITE"));
        //Reflection.getField(clazz, int.class, 0).set(b, 1);
        return b;
    }

}
