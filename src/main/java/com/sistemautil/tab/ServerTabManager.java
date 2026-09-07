package com.sistemautil.tab;

import com.sistemautil.SistemaUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;

/**
 * Controla a identidade visual do TAB e a tag acima da cabeça do jogador.
 * A tag e a cor do nome vêm do CargoPlus. /cor afeta somente a mensagem do chat.
 */
public final class ServerTabManager {
    private final SistemaUtil plugin;
    private int taskId = -1;

    public ServerTabManager(SistemaUtil plugin) {
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
        if (!plugin.getTabConfig().getBoolean("ativado", true)) return;

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        String address = plugin.getTabConfig().getString("endereco-servidor", "play.seuservidor.com:25565");

        String header = formatTabText(plugin.getTabConfig().getString("header",
                "&6&lMEU SERVIDOR\n&7Seja bem-vindo!"), online, max, 0, address);

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayer(player);
            int ping = Math.max(0, player.getPing());
            String footer = formatTabText(plugin.getTabConfig().getString("footer",
                    "&8&m----------------------------------------\n&fJogadores online: &a%online%/%max%\n&fSeu ping: &a%ping%ms\n&fIP: &b%ip%"),
                    online, max, ping, address);
            player.setPlayerListHeaderFooter(header, footer);
        }
    }

    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        applyPlayer(player);
    }

    private void applyPlayer(Player player) {
        CargoData cargo = getCargoData(player);
        if (!cargo.available()) return;

        String prefix = plugin.getTabConfig().getBoolean("tag.ativada", true)
                && plugin.getTabConfig().getBoolean("tag.mostrar-no-tab", true)
                ? colorize(cargo.prefix()) : "";
        String nameColor = colorize(cargo.nicknameColor());
        player.setPlayerListName(prefix + nameColor + player.getName());

        if (plugin.getTabConfig().getBoolean("tag.ativada", true)
                && plugin.getTabConfig().getBoolean("tag.mostrar-na-cabeca", true)) {
            Team team = player.getScoreboard().getEntryTeam(player.getName());
            if (team != null) {
                team.setPrefix(prefix);
                team.setSuffix("");
            }
        }
    }

    private String formatTabText(String text, int online, int max, int ping, String address) {
        return colorize(text == null ? "" : text)
                .replace("%online%", String.valueOf(online))
                .replace("%max%", String.valueOf(max))
                .replace("%ping%", String.valueOf(ping))
                .replace("%ip%", address == null ? "" : address);
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
                    nicknameColor instanceof String ? (String) nicknameColor : "&f"
            );
        } catch (ReflectiveOperationException | LinkageError ex) {
            return CargoData.empty();
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private record CargoData(boolean available, String prefix, String nicknameColor) {
        static CargoData empty() {
            return new CargoData(false, "", "&f");
        }
    }
}
