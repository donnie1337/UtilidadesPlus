package com.sistemautil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Restringe a visibilidade do TAB aos comandos que o jogador realmente pode usar.
 * Namespaces internos (plugin:comando) ficam ocultos para jogadores comuns.
 */
public final class RestrictedCommandTabListener implements Listener {
    private static final String ADMIN_PERMISSION = "cargoplus.admin";

    /** Comandos publicos sem permission declarada que devem continuar visiveis. */
    private static final Set<String> PUBLIC_COMMANDS = Set.of(
            "login",
            "registro",
            "register",
            "cadastrar",
            "tpa",
            "tpaqui",
            "tpaccept",
            "tpaceitar",
            "tpdeny",
            "tpnegar",
            "tpacancel",
            "tpacancelar",
            "home",
            "homes",
            "sethome",
            "delhome"
    );

    private static final Set<String> CARGO_COMMANDS = Set.of(
            "promover",
            "setcargo",
            "removercargo",
            "cargo",
            "cargoplus"
    );

    private volatile Method commandMapMethod;

    public RestrictedCommandTabListener() {
        this.commandMapMethod = resolveCommandMapMethod();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(ADMIN_PERMISSION)) return;

        event.getCommands().removeIf(command -> !isVisible(player, command));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;
        if (player.hasPermission(ADMIN_PERMISSION)) return;

        String buffer = event.getBuffer();
        if (buffer == null || buffer.isBlank()) return;

        String root = normalizeRoot(buffer);
        if (root == null || !isRootVisible(player, root)) {
            event.setCompletions(Collections.emptyList());
        }
    }

    private boolean isVisible(Player player, String command) {
        String root = normalizeRoot(command);
        return root != null && isRootVisible(player, root);
    }

    private boolean isRootVisible(Player player, String root) {
        if (root == null || root.indexOf(':') >= 0) return false;
        if (PUBLIC_COMMANDS.contains(root)) return true;
        if (CARGO_COMMANDS.contains(root)) return false;
        if (root.equals("?") || root.equals("about")) return player.hasPermission(ADMIN_PERMISSION);

        Command registered = findCommand(root);
        if (registered == null) {
            // Falha de descoberta do CommandMap: para o TAB, adota fail-closed.
            // Apenas os comandos publicos explicitamente conhecidos continuam visiveis.
            return false;
        }

        return registered.testPermissionSilent(player);
    }

    private static String normalizeRoot(String value) {
        String command = value == null ? "" : value.trim();
        if (command.startsWith("/")) command = command.substring(1);
        int space = command.indexOf(' ');
        if (space >= 0) command = command.substring(0, space);
        if (command.isEmpty()) return null;
        return command.toLowerCase(Locale.ROOT);
    }

    private Command findCommand(String label) {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) return null;
        return commandMap.getCommand(label);
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
}
