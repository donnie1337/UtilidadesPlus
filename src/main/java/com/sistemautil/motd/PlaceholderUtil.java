package com.sistemautil.motd;

import com.sistemautil.SistemaUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerListPingEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class PlaceholderUtil {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PlaceholderUtil() {}

    public static String aplicar(String texto, SistemaUtil plugin, ServerListPingEvent event) {
        int online = event.getNumPlayers();
        int max = event.getMaxPlayers();
        int vagas = Math.max(0, max - online);
        LocalDateTime agora = LocalDateTime.now();

        return texto
                .replace("%online%", String.valueOf(online))
                .replace("%max%", String.valueOf(max))
                .replace("%vagas%", String.valueOf(vagas))
                .replace("%tps%", String.valueOf(plugin.getTpsMonitor().getTPS()))
                .replace("%hora%", agora.format(HORA))
                .replace("%data%", agora.format(DATA))
                .replace("%servidor%", Bukkit.getServer().getName())
                .replace("%versao%", Bukkit.getVersion());
    }
}
