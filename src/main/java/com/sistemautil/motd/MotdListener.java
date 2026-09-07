package com.sistemautil.motd;

import com.sistemautil.SistemaUtil;
import org.bukkit.ChatColor;
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
        aplicarMaxFicticio(event);
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
        String[] partes = textoFinal.split("\\|", 2);
        String linha1 = ChatColor.translateAlternateColorCodes('&', partes[0].trim());
        String linha2 = partes.length > 1
                ? ChatColor.translateAlternateColorCodes('&', partes[1].trim())
                : "";

        event.setMotd(linha2.isEmpty() ? linha1 : linha1 + "\n" + linha2);
    }

    private void aplicarMaxFicticio(ServerListPingEvent event) {
        if (!plugin.getMotdConfig().getBoolean("jogadores-ficticios.ativado", false)) return;
        event.setMaxPlayers(Math.max(0,
                plugin.getMotdConfig().getInt("jogadores-ficticios.max", 9999)));
    }
}
