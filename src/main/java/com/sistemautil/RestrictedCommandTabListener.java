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
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Restringe a visibilidade do TAB aos comandos que o jogador realmente pode usar.
 * Comandos nativos do Bukkit/Paper ficam visiveis somente para o cargo DEV.
 */
public final class RestrictedCommandTabListener implements Listener {
    private static final String ADMIN_PERMISSION = "cargoplus.admin";
    private static final String COR_PERMISSION = "chatplus.cor";
    private static final String CONFIGURAR_PERMISSION = "utilidadesplus.configurar";
    private static final String TELL_PERMISSION = "essentialsplus.tell";

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
        if (root.equals("tell") || root.equals("r")) return player.hasPermission(TELL_PERMISSION);
        if (root.equals("?") || root.equals("about")) return player.hasPermission(ADMIN_PERMISSION);

        // /cor e /configurar usam permissões administradas pelo CargoPlus.
        // O check de permissão do Bukkit sozinho não representa essa camada,
        // então consultamos a API registrada pelo CargoPlus.
        if (root.equals("cor")) return hasCargoPermission(player, COR_PERMISSION);
        if (root.equals("configurar")) return hasCargoPermission(player, CONFIGURAR_PERMISSION);

        Command registered = findCommand(root);
        if (registered == null) {
            // Falha de descoberta do CommandMap: para o TAB, adota fail-closed.
            // Apenas os comandos publicos explicitamente conhecidos continuam visiveis.
            return false;
        }

        // Comandos encontrados no CommandMap e que nao foram declarados pelos
        // plugins deste projeto sao considerados nativos/internos e ficam
        // restritos ao cargo DEV.
        return false;
    }

    private boolean hasCargoPermission(Player player, String permission) {
        try {
            Class<?> apiClass = Class.forName("com.cargoplus.api.CargoPlusAPI");
            RegisteredServiceProvider<?> registration =
                    Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null || registration.getProvider() == null) return false;

            Object provider = registration.getProvider();
            Method method = apiClass.getMethod("hasCargoPermission", UUID.class, String.class);
            Object result = method.invoke(provider, player.getUniqueId(), permission);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
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
