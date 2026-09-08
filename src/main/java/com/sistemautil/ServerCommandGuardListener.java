package com.sistemautil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Restringe comandos de descoberta/admin do Bukkit/Spigot para jogadores.
 * Somente o cargo dono, via CargoPlus, pode executar esses comandos.
 */
public final class ServerCommandGuardListener implements Listener {
    private static final String ADMIN_PERMISSION = "cargoplus.admin";
    private static final String PLUGINS_PERMISSION = "utilidadesplus.plugins";

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2) return;

        String command = raw.substring(1).trim();
        if (command.isEmpty()) return;

        String label = command.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);

        if (isPluginsCommand(label)) {
            if (!hasCargoPermission(player, PLUGINS_PERMISSION)) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não tem permissão para ver os plugins do servidor.");
            }
            return;
        }

        if (isBukkitOrSpigotCommand(label) && !hasCargoPermission(player, ADMIN_PERMISSION)) {
            event.setCancelled(true);
            player.sendMessage("§cEsse comando está desativado para jogadores.");
        }
    }

    private boolean hasCargoPermission(Player player, String permission) {
        if (!Bukkit.getPluginManager().isPluginEnabled("CargoPlus")) return false;
        try {
            var cargoPlus = Bukkit.getPluginManager().getPlugin("CargoPlus");
            if (cargoPlus == null) return false;
            Method permissionsMethod = cargoPlus.getClass().getMethod("permissions");
            Object permissions = permissionsMethod.invoke(cargoPlus);
            Method check = permissions.getClass().getMethod("hasCargoPermission", java.util.UUID.class, String.class);
            Object result = check.invoke(permissions, player.getUniqueId(), permission);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private boolean isPluginsCommand(String label) {
        return label.equals("plugins")
                || label.equals("pl")
                || label.equals("bukkit:plugins")
                || label.equals("bukkit:pl")
                || label.equals("spigot:plugins")
                || label.equals("spigot:pl");
    }

    private boolean isBukkitOrSpigotCommand(String label) {
        return label.equals("bukkit")
                || label.equals("spigot")
                || label.startsWith("bukkit:")
                || label.startsWith("spigot:");
    }
}
