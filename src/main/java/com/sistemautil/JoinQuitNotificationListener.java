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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class JoinQuitNotificationListener implements Listener {
    private static final String AUTH_PLUGIN_NAME = "LoginPlus";
    private final SistemaUtil plugin;
    private final UtilidadesPreferences preferences;
    private volatile Plugin authPlugin;
    private volatile Method authCheckMethod;
    private volatile Plugin cargoPlugin;
    private volatile Method cargoPermissionsMethod;
    private volatile Method cargoGroupMethod;
    private volatile Method cargoDisplayNameMethod;
    private volatile Method cargoColorMethod;

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
        if (!plugin.getUtilidadesConfig().getBoolean("mensagens-saida.ativado", false)) return;

        String group = cargoGroup(event.getPlayer());
        if (group.isBlank()) return;

        String message = plugin.getUtilidadesConfig().getString("mensagens-saida.mensagem", "");
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
        if (!plugin.getUtilidadesConfig().getBoolean("mensagens-entrada.ativado", true)) return;

        String group = cargoGroup(player);
        if (group.isBlank()) return;

        String message = joinMessage();
        if (message.isBlank()) return;
        message = cargoColor(group) + formatMessage(message, player, group);

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

    private String joinMessage() {
        List<String> configured = plugin.getUtilidadesConfig().getStringList("mensagens-entrada.mensagens");
        if (configured.isEmpty()) return "";
        return configured.get(ThreadLocalRandom.current().nextInt(configured.size()));
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

    private String cargoColor(String group) {
        Plugin cargo = cargoPlugin();
        if (cargo == null || cargoColorMethod == null) return ChatColor.WHITE.toString();
        try {
            Object result = cargoColorMethod.invoke(cargo, group);
            return result instanceof String value && !value.isBlank() ? value : ChatColor.WHITE.toString();
        } catch (ReflectiveOperationException | LinkageError ex) {
            return ChatColor.WHITE.toString();
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
                || cargoDisplayNameMethod == null || cargoColorMethod == null) {
            synchronized (this) {
                if (cargoPlugin != current || cargoPermissionsMethod == null || cargoGroupMethod == null
                || cargoDisplayNameMethod == null || cargoColorMethod == null) {
                    try {
                        cargoPlugin = current;
                        cargoPermissionsMethod = current.getClass().getMethod("permissions");
                        Object permissions = cargoPermissionsMethod.invoke(current);
                        cargoGroupMethod = permissions.getClass().getMethod("getGroup", UUID.class);
                        cargoDisplayNameMethod = current.getClass().getMethod("getCargoDisplayName", String.class);
                        cargoColorMethod = current.getClass().getMethod("getCargoColor", String.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        cargoPermissionsMethod = null;
                        cargoGroupMethod = null;
                        cargoDisplayNameMethod = null;
                        cargoColorMethod = null;
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
