package com.sistemautil;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public final class RestrictedCommandTabListener implements Listener {
    private static final String ADMIN_PERMISSION = "cargoplus.admin";

    private static final Set<String> MEMBER_COMMANDS = Set.of(
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(ADMIN_PERMISSION)) return;

        event.getCommands().removeIf(command -> !isAllowedForMember(command));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;
        if (player.hasPermission(ADMIN_PERMISSION)) return;

        String buffer = event.getBuffer();
        if (buffer == null || buffer.isEmpty()) return;

        String root = normalizeRoot(buffer);
        if (!MEMBER_COMMANDS.contains(root)) {
            event.setCompletions(Collections.emptyList());
        }
    }

    private static boolean isAllowedForMember(String command) {
        String root = normalizeRoot(command);
        return MEMBER_COMMANDS.contains(root);
    }

    private static String normalizeRoot(String value) {
        String command = value.trim();
        if (command.startsWith("/")) command = command.substring(1);
        int space = command.indexOf(' ');
        if (space >= 0) command = command.substring(0, space);
        int colon = command.indexOf(':');
        if (colon >= 0) command = command.substring(colon + 1);
        return command.toLowerCase(Locale.ROOT);
    }
}
