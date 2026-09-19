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
        String separador = plugin.getMotdConfig().getString("formato.separador", "|");
        if (separador == null || separador.isEmpty()) separador = "|";
        String[] partes = textoFinal.split(java.util.regex.Pattern.quote(separador), 2);
        String linha1 = plugin.getVisualText().format(partes[0].trim());
        String linha2 = partes.length > 1
                ? plugin.getVisualText().format(partes[1].trim())
                : "";

        StringBuilder motd = new StringBuilder(linha1);
        if (!linha2.isEmpty()) {
            motd.append("\\n").append(linha2);
        }

        String footer = construirFooter();
        if (!footer.isEmpty()) {
            motd.append("\\n").append(footer);
        }

        event.setMotd(motd.toString());
    }

    private String construirFooter() {
        if (!plugin.getMotdConfig().getBoolean("footer.ativado", false)) return "";

        List<?> riscos = plugin.getMotdConfig().getList("footer.riscos");
        if (riscos == null || riscos.size() < 3) return "";

        boolean animado = plugin.getMotdConfig().getBoolean("footer.animado", true);
        int intervalo = Math.max(1, plugin.getMotdConfig().getInt("footer.intervalo-segundos", 4));
        int indice = 0;

        if (animado) {
            long frame = (System.currentTimeMillis() / 1000L / intervalo) % 3;
            indice = (int) frame;
        }

        StringBuilder riscosTexto = new StringBuilder();
        String mensagem = "";

        for (int i = 0; i < 3; i++) {
            Object valor = riscos.get(i);
            if (!(valor instanceof java.util.Map<?, ?> mapa)) continue;

            String cor = String.valueOf(mapa.getOrDefault("cor", "&f"));
            String risco = String.valueOf(mapa.getOrDefault("risco", "_______"));
            String texto = String.valueOf(mapa.getOrDefault("mensagem", ""));

            if (i > 0) riscosTexto.append(" ");
            riscosTexto.append(cor).append(risco);

            if (i == indice) {
                mensagem = cor + texto;
            }
        }

        String prefixo = plugin.getMotdConfig().getString("footer.prefixo", "");
        String separador = plugin.getMotdConfig().getString("footer.separador", "\\n");

        String riscosFormatados = plugin.getVisualText().format(prefixo + riscosTexto);
        String mensagemFormatada = plugin.getVisualText().format(mensagem);

        if (mensagemFormatada.isEmpty()) return riscosFormatados;
        if ("\\n".equals(separador)) return riscosFormatados + "\\n" + mensagemFormatada;

        return riscosFormatados + plugin.getVisualText().format(separador) + mensagemFormatada;
    }

    private void aplicarMaxFicticio(ServerListPingEvent event) {
        if (!plugin.getMotdConfig().getBoolean("jogadores-ficticios.ativado", false)) return;
        event.setMaxPlayers(Math.max(0,
                plugin.getMotdConfig().getInt("jogadores-ficticios.max", 9999)));
    }
}
