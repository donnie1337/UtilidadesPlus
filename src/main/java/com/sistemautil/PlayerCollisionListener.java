package com.sistemautil;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerCollisionListener implements Listener {

    public void disableCollision(Player player) {
        if (player != null && player.isOnline()) {
            player.setCollidable(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        disableCollision(event.getPlayer());
    }
}
