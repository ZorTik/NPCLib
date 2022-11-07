package net.jitse.npclib.hologram.impl;

import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.HologramLine;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import net.jitse.npclib.hologram.Hologram;
import net.jitse.npclib.internal.MinecraftVersion;
import net.jitse.npclib.internal.NPCBase;
import net.jitse.npclib.utilities.StringUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class HDHologramImpl implements Hologram {

    private final Plugin plugin;
    private final Location location;
    private List<String> text;
    private com.gmail.filoghost.holographicdisplays.api.Hologram holo;

    public HDHologramImpl(NPCBase base, Location location, List<String> text) {
        this.plugin = base.getInstance().getPlugin();
        this.location = location;
        this.text = text;

        build();
        updateLines(null, text);
    }

    @Override
    public void updateLines(Player player, List<String> text) {
        int i = 0;
        for(String s : text) {
            s = prepareLine(s);
            if(holo.size() <= i) {
                holo.appendTextLine(s);
            } else {
                HologramLine current = holo.getLine(i);
                if(current instanceof TextLine) {
                    ((TextLine) current).setText(s);
                } else {
                    holo.removeLine(i);
                    holo.insertTextLine(i, s);
                }
            }
            i++;
        }
        if(holo.size() > text.size()) {
            for(int j = text.size(); j < holo.size(); j++) {
                holo.removeLine(j);
            }
        }
        this.text = text;
    }

    @Override
    public void show(Player player) {
        if(holo == null) {
            build();
            updateLines(player, text);
        }
        if(!holo.getVisibilityManager().isVisibleTo(player)) {
            holo.getVisibilityManager().showTo(player);
        }
    }

    @Override
    public void hide(Player player) {
        holo.delete();
        holo = null;
    }

    private void build() {
        if(holo == null) {
            holo = HologramsAPI.createHologram(plugin, location);
            holo.getVisibilityManager().setVisibleByDefault(false);
        }
    }

    private String prepareLine(String line) {
        line = StringUtil.colorize(line);
        // TODO
        return line;
    }

}
