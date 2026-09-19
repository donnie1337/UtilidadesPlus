package com.sistemautil;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public final class AdvancementMessageListener implements Listener {

    public void disableFor(World world) {
        world.setGameRule(GameRule.SHOW_ADVANCEMENT_MESSAGES, false);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        disableFor(event.getWorld());
    }
}
