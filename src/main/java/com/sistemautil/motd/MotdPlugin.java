package com.sistemautil.motd;

import com.sistemautil.SistemaUtil;
import org.bukkit.Bukkit;
import org.bukkit.util.CachedServerIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

/** Núcleo do antigo SistemaMotd incorporado ao SistemaUtil. */
public final class MotdPlugin {
    private final SistemaUtil plugin;
    private CachedServerIcon iconeAtual;

    public MotdPlugin(SistemaUtil plugin) {
        this.plugin = plugin;
    }

    public void enable() { carregarIcone(); }
    public void recarregar() { carregarIcone(); }

    private void carregarIcone() {
        if (!plugin.getConfig().getBoolean("icone.ativado", true)) {
            iconeAtual = null;
            return;
        }
        String nomeArquivo = plugin.getConfig().getString("icone.arquivo", "icon.png");
        if (nomeArquivo == null || nomeArquivo.isBlank()) nomeArquivo = "icon.png";
        String formato = plugin.getConfig().getString("icone.formato", "png");
        if (formato == null || formato.isBlank()) formato = "png";
        formato = formato.toLowerCase(Locale.ROOT).trim();
        if (!formato.equals("png") && !formato.equals("jpeg")) {
            plugin.getLogger().warning("Formato de icone invalido: '" + formato + "'. Use png ou jpeg.");
            iconeAtual = null;
            return;
        }
        if (!nomeArquivo.toLowerCase(Locale.ROOT).endsWith("." + formato)) {
            plugin.getLogger().warning("O arquivo de icone nao corresponde ao formato configurado.");
            iconeAtual = null;
            return;
        }
        File arquivo = new File(plugin.getDataFolder(), nomeArquivo);
        if (!arquivo.exists() || !arquivo.isFile()) {
            iconeAtual = null;
            return;
        }
        try {
            BufferedImage imagem = ImageIO.read(arquivo);
            if (imagem == null || imagem.getWidth() != 64 || imagem.getHeight() != 64) {
                throw new IllegalArgumentException("a imagem precisa ser valida e 64x64");
            }
            iconeAtual = Bukkit.loadServerIcon(imagem);
        } catch (Exception ex) {
            plugin.getLogger().warning("Nao foi possivel carregar o icone: " + ex.getMessage());
            iconeAtual = null;
        }
    }

    public CachedServerIcon getIconeAtual() { return iconeAtual; }
}
