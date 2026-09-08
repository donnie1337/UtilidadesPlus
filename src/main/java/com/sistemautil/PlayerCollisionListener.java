package com.sistemautil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class PlayerCollisionListener implements Listener {
    private static final String TEAM_PREFIX = "utilidadesplus_nc_";

    public void disableCollision(Player player) {
        if (player == null || !player.isOnline()) return;

        if (player.isCollidable()) {
            player.setCollidable(false);
        }

        // Para colisao entre jogadores, o Paper usa a Team do scoreboard do proprio jogador.
        // Usamos o scoreboard principal para garantir que todos os jogadores compartilhem a
        // mesma referencia de Teams e que outro scoreboard temporario nao quebre a regra.
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (player.getScoreboard() != scoreboard) {
            player.setScoreboard(scoreboard);
        }

        Team team = scoreboard.getEntryTeam(player.getName());
        if (team == null) {
            String teamName = TEAM_PREFIX + player.getUniqueId().toString().replace("-", "").substring(0, 13);
            team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.addEntry(player.getName());
        }

        if (team.getOption(Team.Option.COLLISION_RULE) != Team.OptionStatus.NEVER) {
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        disableCollision(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        disableCollision(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        disableCollision(event.getPlayer());
    }
}
