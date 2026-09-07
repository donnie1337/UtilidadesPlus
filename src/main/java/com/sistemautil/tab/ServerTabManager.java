package com.sistemautil.tab;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Controla a identidade visual do TAB e a tag acima da cabeça do jogador.
 * A tag e a cor do nome são obtidas do CargoPlus; /cor continua afetando
 * somente a cor da mensagem enviada no chat.
 */
public final class ServerTabManager {
    private final JavaPlugin plugin;
    private int taskId = -1;

    public ServerTabManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        updateAll();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAll, 20L, 20L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void updateAll() {
        int online = Bukkit.getOnlinePlayers().size();
        String address = plugin.getConfig().getString("tab.endereco-servidor", "play.seuservidor.com:25565");

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayer(player);

            int ping = Math.max(0, player.getPing());
            player.setPlayerListHeaderFooter(
                    colorize(plugin.getConfig().getString("tab.header",
                            "&8&m----------------------------------------\n&6&lMEU SERVIDOR\n&7Seja bem-vindo!\n")),
                    colorize(plugin.getConfig().getString("tab.footer",
                            "\n&8&m----------------------------------------\n&fJogadores online: &a%online% &8| &fPing: &a%ping%ms\n&fIP: &b%ip%\n"))
                            .replace("%online%", String.valueOf(online))
                            .replace("%ping%", String.valueOf(ping))
                            .replace("%ip%", address == null ? "" : address));
        }
    }

    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        applyPlayer(player);
    }

    private void applyPlayer(Player player) {
        CargoData cargo = getCargoData(player);
        if (!cargo.available()) return;

        String nameColor = colorize(cargo.nicknameColor());
        String prefix = colorize(cargo.prefix());
        String listName = prefix + nameColor + player.getName();
        player.setPlayerListName(listName);

        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team != null) {
            team.setPrefix(prefix);
            team.setSuffix("");
        }
    }

    private CargoData getCargoData(Player player) {
        try {
            var cargo = Bukkit.getPluginManager().getPlugin("CargoPlus");
            if (cargo == null || !cargo.isEnabled()) return CargoData.empty();

            Method permissionsMethod = cargo.getClass().getMethod("permissions");
            Object permissions = permissionsMethod.invoke(cargo);

            Method getPrefix = permissions.getClass().getMethod("getPrefix", java.util.UUID.class);
            Method getNicknameColor = permissions.getClass().getMethod("getNicknameColor", java.util.UUID.class);

            Object prefix = getPrefix.invoke(permissions, player.getUniqueId());
            Object nicknameColor = getNicknameColor.invoke(permissions, player.getUniqueId());

            return new CargoData(
                    true,
                    prefix instanceof String ? (String) prefix : "",
                    nicknameColor instanceof String ? (String) nicknameColor : "§f"
            );
        } catch (ReflectiveOperationException | LinkageError ex) {
            return CargoData.empty();
        }
    }

    private String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private record CargoData(boolean available, String prefix, String nicknameColor) {
        static CargoData empty() {
            return new CargoData(false, "", "§f");
        }
    }
}
