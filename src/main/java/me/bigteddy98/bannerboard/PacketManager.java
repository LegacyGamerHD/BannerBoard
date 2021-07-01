package me.bigteddy98.bannerboard;

import io.netty.channel.Channel;
import me.bigteddy98.bannerboard.util.VersionUtil;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

public class PacketManager {

    private final static String GAME_NAME = "MINECRAFT";
    private final static Method getPlayerHandle;
    private final static Field playerConnection;
    private final static Field networkManager;
    private static Constructor<?> constructor18;
    private static Constructor<?> constructor19;
    private static Constructor<?> constructor1_14;
    private static Constructor<?> constructor1_17;
    private static Field channel;

    static {
        try {
            try {
                try {
                    constructor1_17 = getNewNMS("net.minecraft.network.protocol.game.PacketPlayOutMap")
                            .getConstructor(Integer.TYPE, Byte.TYPE, Boolean.TYPE, Collection.class, WorldMap.b.class);
                } catch (NoSuchMethodException e3) {
                    constructor1_14 = getNMS("PacketPlayOutMap").getConstructor(Integer.TYPE, Byte.TYPE, Boolean.TYPE,
                            Boolean.TYPE, Collection.class, byte[].class, Integer.TYPE, Integer.TYPE, Integer.TYPE,
                            Integer.TYPE);
                }
            } catch (NoSuchMethodException e2) {
                try {
                    constructor19 = getNMS("PacketPlayOutMap").getConstructor(Integer.TYPE, Byte.TYPE, Boolean.TYPE,
                            Collection.class, byte[].class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                } catch (NoSuchMethodException e) {
                    constructor18 = getNMS("PacketPlayOutMap").getConstructor(Integer.TYPE, Byte.TYPE, Collection.class,
                            byte[].class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                }
            }

            getPlayerHandle = getCraft("entity.CraftPlayer").getMethod("getHandle");
            if (VersionUtil.isHigherThan("v1_16_R3")) {
                playerConnection = getNewNMS("net.minecraft.server.level.EntityPlayer").getDeclaredField("b");
            } else {
                playerConnection = getNMS("EntityPlayer").getDeclaredField("playerConnection");
            }
            if (VersionUtil.isHigherThan("v1_16_R3")) {
                networkManager = getNewNMS("net.minecraft.server.network.PlayerConnection").getDeclaredField("a");
            } else {
                networkManager = getNMS("PlayerConnection").getDeclaredField("networkManager");
            }

            if (VersionUtil.isHigherThan("v1_16_R3")) {
                for (Field field : getNewNMS("net.minecraft.network.NetworkManager").getDeclaredFields()) {
                    if (field.getType() == Channel.class) {
                        channel = field;
                    }
                }
            } else {
                for (Field field : getNMS("NetworkManager").getDeclaredFields()) {
                    if (field.getType() == Channel.class) {
                        channel = field;
                    }
                }
            }
            if (channel == null) {
                throw new RuntimeException("Could not find field with type channel in class NetworkManager");
            }
            channel.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> getNMS(String name) throws ClassNotFoundException {
        // org.bukkit.craftbukkit.v1_9_R1
        // 0 1 2 3
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        return Class.forName("net." + GAME_NAME.toLowerCase() + ".server." + version + "." + name, true,
                Bukkit.class.getClassLoader());
    }

    public static Class<?> getNewNMS(String name) throws ClassNotFoundException {
        return Class.forName(name, true, Bukkit.class.getClassLoader());
    }

    public static Class<?> getCraft(String name) throws ClassNotFoundException {
        // org.bukkit.craftbukkit.v1_9_R1
        // 0 1 2 3
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        return Class.forName("org.bukkit.craftbukkit." + version + "." + name, true, Bukkit.class.getClassLoader());
    }

    public static Object getPacket(short mapId, byte[] buffer)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (constructor19 != null) {
            return constructor19.newInstance(mapId, (byte) 4, false, new ArrayList<>(), buffer, 0, 0, 128, 128);
        }
        if (constructor1_14 != null) {
            return constructor1_14.newInstance(mapId, (byte) 4, false, false, new ArrayList<>(), buffer, 0, 0, 128,
                    128);
        }
        if (constructor1_17 != null) {
            return constructor1_17.newInstance(mapId, (byte) 4, false, new ArrayList<>(), new WorldMap.b(0, 0, 128, 128, buffer));
        }
        return constructor18.newInstance(mapId, (byte) 4, new ArrayList<>(), buffer, 0, 0, 128, 128);
    }

    public static Channel getChannel(Player p)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Object entityPlayer = getPlayerHandle.invoke(p);
        Object connection = playerConnection.get(entityPlayer);
        Object network = networkManager.get(connection);
        return (Channel) channel.get(network);
    }
}