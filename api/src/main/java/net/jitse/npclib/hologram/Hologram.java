package net.jitse.npclib.hologram;

import org.bukkit.entity.Player;

import java.util.List;

public interface Hologram {

    void updateLines(Player player, List<String> text);
    void show(Player player);
    void hide(Player player);

    default void destroy(Player player) {
        // User implementation.
    }

}
