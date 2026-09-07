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
    private final SistemaUtil plugin;
    private final UtilidadesPreferences preferences;

    public JoinQuitNotificationListener(SistemaUtil plugin, UtilidadesPreferences preferences) {
        this.plugin = plugin;
        this.preferences = preferences;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        announceLater(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        if (!hasStaffCargo(event.getPlayer())) return;
        String message = buildMessage(event.getPlayer(), false);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (preferences.receivesQuit(viewer)) viewer.sendMessage(message);
        }
    }

    private void announceLater(Player player, boolean join) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !hasStaffCargo(player)) return;
            String message = buildMessage(player, join);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (join ? preferences.receivesJoin(viewer) : preferences.receivesQuit(viewer)) {
                    viewer.sendMessage(message);
                }
            }
        }, 1L);
    }

    private boolean hasStaffCargo(Player player) {
        String prefix = cargoValue(player, "getPrefix");
        return prefix != null && !ChatColor.stripColor(colorize(prefix)).isBlank();
    }

    private String buildMessage(Player player, boolean join) {
        String template = plugin.getUtilidadesConfig().getString(
                "mensagens." + (join ? "entrada" : "saida"),
                join ? "&8[&a+&8] %prefix%%nome-color%%nome% &7entrou no servidor!"
                        : "&8[&c-&8] %prefix%%nome-color%%nome% &7saiu do servidor!");
        String prefix = colorize(cargoValue(player, "getPrefix"));
        String nameColor = colorize(cargoValue(player, "getNicknameColor"));
        return colorize(template)
                .replace("%prefix%", prefix)
                .replace("%nome-color%", nameColor)
                .replace("%nome%", player.getName());
    }

    private String cargoValue(Player player, String methodName) {
        Plugin cargo = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (cargo == null || !cargo.isEnabled()) return "";
        try {
            Method permissionsMethod = cargo.getClass().getMethod("permissions");
            Object permissions = permissionsMethod.invoke(cargo);
            Method method = permissions.getClass().getMethod(methodName, UUID.class);
            Object result = method.invoke(permissions, player.getUniqueId());
            return result instanceof String value ? value : "";
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "";
        }
    }

    private String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
