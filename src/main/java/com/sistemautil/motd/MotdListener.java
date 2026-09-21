package com.sistemautil.motd;

import com.sistemautil.SistemaUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;

public final class MotdListener implements Listener {
    private final SistemaUtil plugin;

    public MotdListener(SistemaUtil plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        aplicarMaxDinamico(event);
        aplicarMotd(event);
        if (plugin.getMotd().getIconeAtual() != null
                && plugin.getMotdConfig().getBoolean("icone.ativado", true)) {
            event.setServerIcon(plugin.getMotd().getIconeAtual());
        }
    }

    private void aplicarMotd(ServerListPingEvent event) {
        if (!plugin.getMotdConfig().getBoolean("ativado", true)) return;

        List<String> linhas = plugin.getMotdConfig().getStringList("linhas");
        if (linhas.isEmpty()) return;

        String linhaEscolhida;
        boolean animado = plugin.getMotdConfig().getBoolean("animado", true);
        if (animado && linhas.size() > 1) {
            int intervalo = Math.max(1,
                    plugin.getMotdConfig().getInt("intervalo-segundos", 4));
            long frame = (System.currentTimeMillis() / 1000L / intervalo) % linhas.size();
            linhaEscolhida = linhas.get((int) frame);
        } else {
            linhaEscolhida = linhas.get(0);
        }

        String textoFinal = PlaceholderUtil.aplicar(linhaEscolhida, plugin, event);
        String separador = plugin.getMotdConfig().getString("formato.separador", "|");
        if (separador == null || separador.isEmpty()) separador = "|";
        String[] partes = textoFinal.split(java.util.regex.Pattern.quote(separador), 2);
        String linha1 = plugin.getVisualText().format(partes[0].trim());
        String linha2 = partes.length > 1
                ? plugin.getVisualText().format(partes[1].trim())
                : "";

        StringBuilder motd = new StringBuilder(linha1);
        if (!linha2.isEmpty()) {
            motd.append("\n").append(linha2);
        }

        event.setMotd(motd.toString());
    }

    private void aplicarMaxDinamico(ServerListPingEvent event) {
        // O MOTD sempre exibe o total de jogadores online + 1 como limite.
        // Ex.: 1/2, 10/11, 100/101.
        event.setMaxPlayers(event.getNumPlayers() + 1);
    }
}
