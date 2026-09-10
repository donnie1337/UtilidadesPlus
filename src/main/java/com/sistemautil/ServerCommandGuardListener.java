package com.sistemautil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Protege comandos administrativos e oculta comandos sem permissão.
 * Jogadores sem a permissão necessária recebem a mesma mensagem de um
 * comando inexistente, evitando revelar a existência do comando.
 */
public final class ServerCommandGuardListener implements Listener {
    private static final String ADMIN_PERMISSION = "cargoplus.admin";
    private static final String PLUGINS_PERMISSION = "utilidadesplus.plugins";
    private static final String CHAT_PREFIX = "&e&lᴄʜᴀᴛ &8• &r";

    private volatile Method commandMapMethod;

    public ServerCommandGuardListener() {
        this.commandMapMethod = resolveCommandMapMethod();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
            return;
        }

        Command registered = findCommand(label);
        if (registered == null) return;

        // O Bukkit/Paper já conhece a permissão declarada no plugin.yml.
        // Interceptamos antes da execução para que a mensagem padrão de
        // falta de permissão nunca seja exposta ao jogador.
        if (!registered.testPermissionSilent(player)) {
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
            Object permissions = permissionsMethod.invoke(cargoPlus);
            Method check = permissions.getClass().getMethod("hasCargoPermission", java.util.UUID.class, String.class);
            Object result = check.invoke(permissions, player.getUniqueId(), permission);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    private Command findCommand(String label) {
        CommandMap commandMap = getCommandMap();
        return commandMap == null ? null : commandMap.getCommand(label);
    }

    private CommandMap getCommandMap() {
        Method method = commandMapMethod;
        if (method == null) {
            method = resolveCommandMapMethod();
            commandMapMethod = method;
        }
        if (method == null) return null;
        try {
            Object result = method.invoke(Bukkit.getServer());
            return result instanceof CommandMap map ? map : null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            commandMapMethod = null;
            return null;
        }
    }

    private Method resolveCommandMapMethod() {
        try {
            return Bukkit.getServer().getClass().getMethod("getCommandMap");
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
