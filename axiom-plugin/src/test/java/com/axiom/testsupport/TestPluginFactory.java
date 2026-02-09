package com.axiom.testsupport;

import com.axiom.AXIOM;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

public final class TestPluginFactory {
    private static Server server;
    private static BukkitScheduler scheduler;

    private TestPluginFactory() {
    }

    public static AXIOM createPlugin(File dataFolder) {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Failed to create data folder: " + dataFolder);
        }
        ensureServer();
        AXIOM plugin = allocate(AXIOM.class);
        initPlugin(plugin, dataFolder, server);
        return plugin;
    }

    public static void setField(Object target, String fieldName, Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to set field: " + fieldName, e);
            }
        }
        throw new IllegalStateException("Field not found: " + fieldName);
    }

    private static void ensureServer() {
        if (server != null) {
            return;
        }
        scheduler = createScheduler();
        server = createServer(scheduler);
        if (Bukkit.getServer() == null) {
            Bukkit.setServer(server);
        }
    }

    private static BukkitScheduler createScheduler() {
        InvocationHandler handler = (proxy, method, args) -> {
            Class<?> ret = method.getReturnType();
            if (BukkitTask.class.isAssignableFrom(ret)) {
                return new DummyTask(1, true);
            }
            if (ret == int.class) {
                return 1;
            }
            if (ret == boolean.class) {
                return false;
            }
            return defaultValue(ret);
        };
        return (BukkitScheduler) Proxy.newProxyInstance(
            BukkitScheduler.class.getClassLoader(),
            new Class<?>[]{BukkitScheduler.class},
            handler
        );
    }

    private static Server createServer(BukkitScheduler scheduler) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            switch (name) {
                case "getScheduler":
                    return scheduler;
                case "getLogger":
                    return Logger.getLogger("TestServer");
                case "getName":
                    return "TestServer";
                case "getVersion":
                    return "1.0";
                case "getBukkitVersion":
                    return "1.20.1";
                case "getOnlinePlayers":
                    return Collections.emptyList();
                case "getPluginCommand":
                    return null;
                default:
                    return defaultValue(method.getReturnType());
            }
        };
        return (Server) Proxy.newProxyInstance(
            Server.class.getClassLoader(),
            new Class<?>[]{Server.class},
            handler
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == void.class) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == double.class) {
            return 0.0;
        }
        if (returnType == float.class) {
            return 0.0f;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == char.class) {
            return (char) 0;
        }
        if (returnType == String.class) {
            return "";
        }
        if (Iterable.class.isAssignableFrom(returnType)) {
            return Collections.emptyList();
        }
        if (Map.class.isAssignableFrom(returnType)) {
            return Collections.emptyMap();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            return (T) unsafe.allocateInstance(type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to allocate instance: " + type.getName(), e);
        }
    }

    private static void initPlugin(AXIOM plugin, File dataFolder, Server server) {
        try {
            PluginDescriptionFile desc = new PluginDescriptionFile("AXIOM", "test", AXIOM.class.getName());
            PluginLoader loader = (PluginLoader) Proxy.newProxyInstance(
                PluginLoader.class.getClassLoader(),
                new Class<?>[]{PluginLoader.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
            );
            Method init = JavaPlugin.class.getDeclaredMethod(
                "init",
                PluginLoader.class,
                Server.class,
                PluginDescriptionFile.class,
                File.class,
                File.class,
                ClassLoader.class
            );
            init.setAccessible(true);
            init.invoke(
                plugin,
                loader,
                server,
                desc,
                dataFolder,
                new File(dataFolder, "axiom-test.jar"),
                AXIOM.class.getClassLoader()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init plugin", e);
        }
    }

    private static final class DummyTask implements BukkitTask {
        private final int id;
        private final boolean sync;

        private DummyTask(int id, boolean sync) {
            this.id = id;
            this.sync = sync;
        }

        @Override
        public int getTaskId() {
            return id;
        }

        @Override
        public org.bukkit.plugin.Plugin getOwner() {
            return null;
        }

        @Override
        public boolean isSync() {
            return sync;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void cancel() {
        }
    }
}
