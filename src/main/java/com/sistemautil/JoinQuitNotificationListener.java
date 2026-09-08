package com.sistemautil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class JoinQuitNotificationListener implements Listener {
    private static final String AUTH_PLUGIN_NAME = "LoginPlus";
    private final SistemaUtil plugin;
    private final UtilidadesPreferences preferences;
    private volatile Plugin authPlugin;
    private volatile Method authCheckMethod;
    private volatile Plugin cargoPlugin;
    private volatile Method cargoPermissionsMethod;
    private volatile Method cargoGroupMethod;
    private volatile Method cargoJoinEnabledMethod;
    private volatile Method cargoQuitEnabledMethod;
    private volatile Method cargoJoinMessageMethod;
    private volatile Method cargoQuitMessageMethod;
    private volatile Method cargoDisplayNameMethod;

    public JoinQuitNotificationListener(SistemaUtil plugin, UtilidadesPreferences preferences) {
        this.plugin = plugin;
        this.preferences = preferences;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        checkJoin(event.getPlayer(), 0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        if (!preferences.globallyReceivesQuit() || !preferences.broadcastsQuit(event.getPlayer())) return;

        String group = cargoGroup(event.getPlayer());
        if (group.isBlank() || !cargoBoolean(group, false)) return;

        String message = cargoMessage(group, false);
        if (message.isBlank()) return;
        message = formatMessage(message, event.getPlayer(), group);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (preferences.receivesQuit(viewer)) viewer.sendMessage(message);
        }
    }

    private void checkJoin(Player player, int attempt) {
        if (!player.isOnline() || attempt >= 60) return;
        if (!isAuthenticated(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkJoin(player, attempt + 1), 20L);
            return;
        }
        if (!preferences.broadcastsJoin(player)) return;

        String group = cargoGroup(player);
        if (group.isBlank() || !cargoBoolean(group, true)) return;

        String message = cargoMessage(group, true);
        if (message.isBlank()) return;
        message = formatMessage(message, player, group);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player) || preferences.receivesJoin(viewer)) viewer.sendMessage(message);
        }
    }

    private boolean isAuthenticated(Player player) {
        if (player == null || !player.isOnline()) return false;
        Plugin current = Bukkit.getPluginManager().getPlugin(AUTH_PLUGIN_NAME);
        if (current == null || !current.isEnabled()) return true;

        Method method = authCheckMethod;
        if (authPlugin != current || method == null) {
            synchronized (this) {
                if (authPlugin != current || authCheckMethod == null) {
                    authPlugin = current;
                    try { authCheckMethod = current.getClass().getMethod("isAuthenticated", Player.class); }
                    catch (ReflectiveOperationException | LinkageError ex) { authCheckMethod = null; }
                    method = authCheckMethod;
                }
            }
        }
        if (method == null) return false;
        try {
            Object result = method.invoke(current, player);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private String cargoGroup(Player player) {
        Object permissions = cargoPermissions();
        if (permissions == null || cargoGroupMethod == null) return "";
        try {
            Object result = cargoGroupMethod.invoke(permissions, player.getUniqueId());
            return result instanceof String value ? value.trim().toLowerCase(java.util.Locale.ROOT) : "";
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "";
        }
    }

    private boolean cargoBoolean(String group, boolean join) {
        Plugin cargo = cargoPlugin();
        Method method = join ? cargoJoinEnabledMethod : cargoQuitEnabledMethod;
        if (cargo == null || method == null) return false;
        try {
            Object result = method.invoke(cargo, group);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private String cargoMessage(String group, boolean join) {
        Plugin cargo = cargoPlugin();
        Method method = join ? cargoJoinMessageMethod : cargoQuitMessageMethod;
        if (cargo == null || method == null) return "";
        try {
            Object result = method.invoke(cargo, group);
            return result instanceof String value ? value : "";
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "";
        }
    }

    private String cargoDisplayName(String group) {
        Plugin cargo = cargoPlugin();
        if (cargo == null || cargoDisplayNameMethod == null) return group;
        try {
            Object result = cargoDisplayNameMethod.invoke(cargo, group);
            return result instanceof String value && !value.isBlank() ? value : group;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return group;
        }
    }

    private Object cargoPermissions() {
        Plugin cargo = cargoPlugin();
        if (cargo == null || cargoPermissionsMethod == null) return null;
        try {
            return cargoPermissionsMethod.invoke(cargo);
        } catch (ReflectiveOperationException | LinkageError ex) {
            return null;
        }
    }

    private Plugin cargoPlugin() {
        Plugin current = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (current == null || !current.isEnabled()) return null;

        if (cargoPlugin != current || cargoPermissionsMethod == null || cargoGroupMethod == null
                || cargoJoinEnabledMethod == null || cargoQuitEnabledMethod == null
                || cargoJoinMessageMethod == null || cargoQuitMessageMethod == null
                || cargoDisplayNameMethod == null) {
            synchronized (this) {
                if (cargoPlugin != current || cargoPermissionsMethod == null || cargoGroupMethod == null
                        || cargoJoinEnabledMethod == null || cargoQuitEnabledMethod == null
                        || cargoJoinMessageMethod == null || cargoQuitMessageMethod == null
                        || cargoDisplayNameMethod == null) {
                    try {
                        cargoPlugin = current;
                        cargoPermissionsMethod = current.getClass().getMethod("permissions");
                        Object permissions = cargoPermissionsMethod.invoke(current);
                        cargoGroupMethod = permissions.getClass().getMethod("getGroup", UUID.class);
                        cargoJoinEnabledMethod = current.getClass().getMethod("isJoinMessageEnabled", String.class);
                        cargoQuitEnabledMethod = current.getClass().getMethod("isQuitMessageEnabled", String.class);
                        cargoJoinMessageMethod = current.getClass().getMethod("getJoinMessage", String.class);
                        cargoQuitMessageMethod = current.getClass().getMethod("getQuitMessage", String.class);
                        cargoDisplayNameMethod = current.getClass().getMethod("getCargoDisplayName", String.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        cargoPermissionsMethod = null;
                        cargoGroupMethod = null;
                        cargoJoinEnabledMethod = null;
                        cargoQuitEnabledMethod = null;
                        cargoJoinMessageMethod = null;
                        cargoQuitMessageMethod = null;
                        cargoDisplayNameMethod = null;
                        return null;
                    }
                }
            }
        }
        return cargoPlugin;
    }

    private String formatMessage(String message, Player player, String group) {
        return colorize(message)
                .replace("%player%", player.getName())
                .replace("%cargo%", cargoDisplayName(group));
    }

    private String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
