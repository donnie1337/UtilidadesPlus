package com.sistemautil;

import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public final class DeathMessageListener implements Listener {

    public void disableFor(World world) {
        world.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        disableFor(event.getWorld());
    }
}
