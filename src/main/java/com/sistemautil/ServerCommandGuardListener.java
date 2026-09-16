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
 * Protege apenas comandos administrativos do servidor que não devem ser
 * expostos a jogadores comuns. O tratamento de permissões dos comandos dos
 * plugins fica centralizado no ChatPlus para evitar mensagens duplicadas.
 */
public final class ServerCommandGuardListener implements Listener {
    private static final String ADMIN_PERMISSION = "cargoplus.admin";
    private static final String PLUGINS_PERMISSION = "utilidadesplus.plugins";
    private static final String CHAT_PREFIX = "&e&lᴄʜᴀᴛ &8• &r";

    private volatile Method commandMapMethod;

    public ServerCommandGuardListener() {
        this.commandMapMethod = resolveCommandMapMethod();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2) return;

        String command = raw.substring(1).trim();
        if (command.isEmpty()) return;

        String label = command.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);

        if (isHelpDiscoveryCommand(label) && !hasCargoPermission(player, ADMIN_PERMISSION)) {
            deny(event, player);
            return;
        }

        if (isPluginsCommand(label) && !hasCargoPermission(player, PLUGINS_PERMISSION)) {
            deny(event, player);
            return;
        }

        if (isBukkitOrSpigotCommand(label) && !hasCargoPermission(player, ADMIN_PERMISSION)) {
            deny(event, player);
        }
    }

    private void deny(PlayerCommandPreprocessEvent event, Player player) {
        event.setCancelled(true);
        player.sendMessage(colorize(CHAT_PREFIX + "Comando não encontrado."));
    }

    private boolean hasCargoPermission(Player player, String permission) {
        if (!Bukkit.getPluginManager().isPluginEnabled("CargoPlus")) return false;
        try {
            var cargoPlus = Bukkit.getPluginManager().getPlugin("CargoPlus");
            if (cargoPlus == null) return false;
            Method permissionsMethod = cargoPlus.getClass().getMethod("permissions");
            permissionsMethod.setAccessible(true);
            Object permissions = permissionsMethod.invoke(cargoPlus);
            Method check = permissions.getClass().getMethod("hasCargoPermission", java.util.UUID.class, String.class);
            check.setAccessible(true);
            Object result = check.invoke(permissions, player.getUniqueId(), permission);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private Method resolveCommandMapMethod() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getCommandMap");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return null;
        }
    }

    private String colorize(String message) {
        return message.replace('&', '§');
    }

    private boolean isHelpDiscoveryCommand(String label) {
        return label.equals("?")
                || label.equals("help")
                || label.equals("about")
                || label.equals("bukkit:help")
                || label.equals("spigot:help")
                || label.equals("minecraft:help");
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
