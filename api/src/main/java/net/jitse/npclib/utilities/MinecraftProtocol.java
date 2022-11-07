package net.jitse.npclib.utilities;

public class MinecraftProtocol {

    public static boolean isNewMinecraftProtocol(Class<?> packetPlayInUseEntityClazz) {
        return packetPlayInUseEntityClazz != null && isNewMinecraftProtocol(packetPlayInUseEntityClazz.getName());
    }

    public static boolean isNewMinecraftProtocol(String className) {
        return className.startsWith("net.minecraft.network");
    }

}
