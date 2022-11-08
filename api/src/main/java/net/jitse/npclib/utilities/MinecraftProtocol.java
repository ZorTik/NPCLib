package net.jitse.npclib.utilities;

public class MinecraftProtocol {

    public static boolean isNewMinecraftProtocol() {
        try {
            Class.forName("net.minecraft.network.NetworkManager");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isNewMinecraftProtocol(Class<?> packetPlayInUseEntityClazz) {
        return packetPlayInUseEntityClazz != null && isNewMinecraftProtocol(packetPlayInUseEntityClazz.getName());
    }

    public static boolean isNewMinecraftProtocol(String className) {
        return className.startsWith("net.minecraft.network");
    }

}
