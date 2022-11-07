package net.jitse.npclib.hologram.impl;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import net.jitse.npclib.hologram.Hologram;
import net.jitse.npclib.internal.NPCBase;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DHHologramImpl implements Hologram {

    private static final Map<String, eu.decentsoftware.holograms.api.holograms.Hologram> HOLOGRAMS
            = new ConcurrentHashMap<>();

    private final NPCBase base;
    private final Location location;
    private List<String> text;

    public DHHologramImpl(NPCBase base, Location location, List<String> text) {
        this.base = base;
        this.location = location;
        this.text = text;
    }

    @Override
    public void updateLines(Player player, List<String> text) {
        eu.decentsoftware.holograms.api.holograms.Hologram hologram = HOLOGRAMS.get(base.getId());
        if(hologram != null) {
            HologramPage page = hologram.getPage(0);
            int i = 0;
            for(String s : text) {
                if(page.getLines().size() <= i) {
                    DHAPI.addHologramLine(hologram, s);
                } else {
                    DHAPI.setHologramLine(hologram, i, s);
                }
                i++;
            }
            int linesSize = page.getLines().size();
            if(linesSize > text.size()) {
                for(int j = text.size(); j < linesSize; j++) {
                    page.removeLine(j);
                }
            }
        }
    }

    @Override
    public void show(Player player) {
        eu.decentsoftware.holograms.api.holograms.Hologram hologram;
        if(!HOLOGRAMS.containsKey(base.getId())) {
            hologram = DHAPI.createHologram(base.getId(), location);
            hologram.hideAll();
            HOLOGRAMS.put(base.getId(), hologram);

            updateLines(player, text);
        } else {
            hologram = HOLOGRAMS.get(base.getId());
        }
        hologram.show(player, 0);
    }

    @Override
    public void hide(Player player) {
        if(HOLOGRAMS.containsKey(base.getId())) {
            eu.decentsoftware.holograms.api.holograms.Hologram hologram = HOLOGRAMS.get(base.getId());
            hologram.hide(player);
            if(hologram.getShowPlayers().isEmpty()) {
                hologram.delete();
                HOLOGRAMS.remove(base.getId());
            }
        }
    }

}
