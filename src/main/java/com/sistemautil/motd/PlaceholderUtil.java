package com.sistemautil.motd;

import com.sistemautil.SistemaUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerListPingEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class PlaceholderUtil {
    

    private PlaceholderUtil() {}

    public static String aplicar(String texto, SistemaUtil plugin, ServerListPingEvent event) {
        int online = event.getNumPlayers();
        int max = event.getMaxPlayers();
        int vagas = Math.max(0, max - online);
        LocalDateTime agora = LocalDateTime.now();
        String formatoHora = plugin.getMotdConfig().getString("placeholders.hora.formato", "HH:mm");
        String formatoData = plugin.getMotdConfig().getString("placeholders.data.formato", "dd/MM/yyyy");
        String hora = agora.format(DateTimeFormatter.ofPattern(formatoHora));
        String data = agora.format(DateTimeFormatter.ofPattern(formatoData));

        return texto
                .replace("%online%", String.valueOf(online))
                .replace("%max%", String.valueOf(max))
                .replace("%vagas%", String.valueOf(vagas))
                .replace("%tps%", String.valueOf(plugin.getTpsMonitor().getTPS()))
                .replace("%hora%", hora)
                .replace("%data%", data)
                .replace("%servidor%", Bukkit.getServer().getName())
                .replace("%versao%", Bukkit.getVersion());
    }
}
