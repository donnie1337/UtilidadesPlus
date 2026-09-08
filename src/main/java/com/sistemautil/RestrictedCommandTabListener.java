package com.sistemautil;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Locale;

public final class RestrictedCommandTabListener implements Listener {
    private static final String ADMIN_PERMISSION = "utilidadesplus.admin";

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(ADMIN_PERMISSION)) return;

        event.getCommands().removeIf(command -> {
            String root = command.toLowerCase(Locale.ROOT);
            return root.equals("spigot") || root.equals("bukkit");
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;
        if (player.hasPermission(ADMIN_PERMISSION)) return;

        String buffer = event.getBuffer();
        if (buffer == null || buffer.isEmpty()) return;

        String command = buffer.startsWith("/") ? buffer.substring(1) : buffer;
        int space = command.indexOf(' ');
        String root = (space >= 0 ? command.substring(0, space) : command)
                .toLowerCase(Locale.ROOT);

        if (root.equals("spigot") || root.equals("bukkit")) {
            event.setCompletions(java.util.Collections.emptyList());
        }
    }
}
