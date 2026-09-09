package com.sistemautil;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;

/** Recalcula o TAB logo após comandos do CargoPlus que alteram o cargo. */
public final class CargoTabRefreshListener implements Listener {
    private final SistemaUtil plugin;

    public CargoTabRefreshListener(SistemaUtil plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isCargoChangeCommand(event.getMessage())) scheduleRefresh();
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        if (isCargoChangeCommand(event.getCommand())) scheduleRefresh();
    }

    private boolean isCargoChangeCommand(String command) {
        if (command == null) return false;
        String normalized = command.trim();
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.isBlank()) return false;
        String label = normalized.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return label.equals("promover") || label.equals("setcargo");
    }

    private void scheduleRefresh() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getTabManager() != null) plugin.getTabManager().updateAll();
        }, 1L);
    }
}
