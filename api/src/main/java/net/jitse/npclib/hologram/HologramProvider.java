package net.jitse.npclib.hologram;

import net.jitse.npclib.hologram.impl.DHHologramImpl;
import net.jitse.npclib.hologram.impl.HDHologramImpl;
import net.jitse.npclib.internal.MinecraftVersion;
import net.jitse.npclib.internal.NPCBase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.PluginManager;

import javax.annotation.Nullable;
import java.util.List;

public class HologramProvider {

    @Nullable
    public static Hologram build(NPCBase base, MinecraftVersion version, Location location, List<String> text) {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        if(!version.isAboveOrEqual(MinecraftVersion.V1_19_R1)) {
            return new HologramImpl(version, location, text);
        } else if(pluginManager.getPlugin("HolographicDisplays") != null) {
            return new HDHologramImpl(base, location, text);
        } else if(pluginManager.getPlugin("DecentHolograms") != null) {
            return new DHHologramImpl(base, location, text);
        }
        return null;
    }

}
