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
    private volatile Method cargoPrefixMethod;
    private volatile Method cargoNicknameColorMethod;

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
        if (!preferences.globallyReceivesQuit()) return;
        if (!hasStaffCargo(event.getPlayer()) || !preferences.broadcastsQuit(event.getPlayer())) return;

        String message = buildMessage(event.getPlayer(), false);
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
        if (!hasStaffCargo(player) || !preferences.broadcastsJoin(player)) return;

        String message = buildMessage(player, true);
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

    private boolean hasStaffCargo(Player player) {
        String prefix = cargoValue(player, "prefix");
        return prefix != null && !ChatColor.stripColor(colorize(prefix)).isBlank();
    }

    private String buildMessage(Player player, boolean join) {
        String template = plugin.getUtilidadesConfig().getString(
                "mensagens." + (join ? "entrada" : "saida"),
                join ? "&8[&a+&8] %prefix%%nome-color%%nome% &7entrou no servidor!"
                        : "&8[&c-&8] %prefix%%nome-color%%nome% &7saiu do servidor!");
        String prefix = colorize(cargoValue(player, "prefix"));
        String nameColor = colorize(cargoValue(player, "nickname-color"));
        return colorize(template)
                .replace("%prefix%", prefix)
                .replace("%nome-color%", nameColor)
                .replace("%nome%", player.getName());
    }

    private String cargoValue(Player player, String type) {
        Plugin cargo = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (cargo == null || !cargo.isEnabled()) return "";
        try {
            Method permissionsMethod = cargoPermissionsMethod;
            if (cargoPlugin != cargo || permissionsMethod == null || cargoPrefixMethod == null || cargoNicknameColorMethod == null) {
                synchronized (this) {
                    if (cargoPlugin != cargo || cargoPermissionsMethod == null || cargoPrefixMethod == null || cargoNicknameColorMethod == null) {
                        cargoPlugin = cargo;
                        cargoPermissionsMethod = cargo.getClass().getMethod("permissions");
                        Object permissions = cargoPermissionsMethod.invoke(cargo);
                        cargoPrefixMethod = permissions.getClass().getMethod("getPrefix", UUID.class);
                        cargoNicknameColorMethod = permissions.getClass().getMethod("getNicknameColor", UUID.class);
                    }
                    permissionsMethod = cargoPermissionsMethod;
                }
            }
            Object permissions = permissionsMethod.invoke(cargo);
            Method valueMethod = "prefix".equals(type) ? cargoPrefixMethod : cargoNicknameColorMethod;
            Object result = valueMethod.invoke(permissions, player.getUniqueId());
            return result instanceof String value ? value : "";
        } catch (ReflectiveOperationException | LinkageError ex) {
            cargoPrefixMethod = null;
            cargoNicknameColorMethod = null;
            return "";
        }
    }

    private String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
