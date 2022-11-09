package net.jitse.npclib.hologram;

import net.jitse.npclib.hologram.impl.DHHologramImpl;
import net.jitse.npclib.hologram.impl.HDHologramImpl;
import net.jitse.npclib.internal.MinecraftVersion;
import net.jitse.npclib.internal.NPCBase;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class HologramProvider {

    public static Hologram build(NPCBase base, MinecraftVersion version, Location location, List<String> text) {
        HologramType availableType;
        if(!version.isAboveOrEqual(MinecraftVersion.V1_19_R1)) {
            return new HologramImpl(version, location, text);
        } else if((availableType = HologramType.firstAvailable(base).orElse(null)) != null) {
            switch(availableType){
                case HD:
                    return new HDHologramImpl(base, location, text);
                case DH:
                    return new DHHologramImpl(base, location, text);
            }
        }
        throw new RuntimeException("No hologram type available for use. Please check your dependencies.");
    }

    public enum HologramType {
        HD(testPlugin("HolographicDisplays")),
        DH(testPlugin("DecentHolograms"));

        private static Optional<HologramType> firstAvailable(NPCBase base) {
            for(HologramType type : HologramType.values()) {
                if(type.isAvailable(base)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }

        private final Predicate<NPCBase> available;

        HologramType(Predicate<NPCBase> available) {
            this.available = available;
        }

        public boolean isAvailable(NPCBase base) {
            return available.test(base);
        }

        private static Predicate<NPCBase> testPlugin(String name) {
            return base -> Bukkit.getServer().getPluginManager().getPlugin(name) != null;
        }

    }

}
