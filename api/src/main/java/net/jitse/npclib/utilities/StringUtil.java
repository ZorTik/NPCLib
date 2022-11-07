package net.jitse.npclib.utilities;

import org.bukkit.ChatColor;

public final class StringUtil {

    public static String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

}
