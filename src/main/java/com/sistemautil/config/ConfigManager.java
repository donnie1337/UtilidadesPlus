package com.sistemautil.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Centraliza os arquivos de configuração externos do SistemaUtil.
 *
 * Os arquivos ficam em plugins/SistemaUtil/ e são criados automaticamente
 * na primeira inicialização do plugin. Alterações feitas pelo administrador
 * são preservadas em reinicializações e recargas.
 */
public final class ConfigManager {
    private final JavaPlugin plugin;
    private final File dataFolder;
    private final File motdFile;
    private final File tabFile;
    private final File corFile;

    private FileConfiguration motd;
    private FileConfiguration tab;
    private FileConfiguration cor;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.motdFile = new File(dataFolder, "motd.yml");
        this.tabFile = new File(dataFolder, "tab.yml");
        this.corFile = new File(dataFolder, "cor.yml");
    }

    public void loadAll() {
        ensureDataFolder();
        motd = load("motd.yml", motdFile);
        tab = load("tab.yml", tabFile);
        cor = load("cor.yml", corFile);
    }

    public void reloadAll() {
        loadAll();
    }

    public FileConfiguration motd() {
        return motd;
    }

    public FileConfiguration tab() {
        return tab;
    }

    public FileConfiguration cor() {
        return cor;
    }

    private void ensureDataFolder() {
        if (dataFolder.exists()) {
            return;
        }

        if (!dataFolder.mkdirs() && !dataFolder.exists()) {
            plugin.getLogger().warning(
                    "Não foi possível criar a pasta de configurações: " + dataFolder.getAbsolutePath());
        }
    }

    private FileConfiguration load(String resource, File file) {
        if (!file.exists()) {
            try {
                plugin.saveResource(resource, false);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().log(Level.SEVERE,
                        "O recurso padrão " + resource + " não está presente no JAR do SistemaUtil.",
                        exception);
            }
        }

        FileConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        try (InputStream stream = plugin.getResource(resource)) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                loaded.setDefaults(defaults);
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Não foi possível carregar os valores padrão de " + resource + ".",
                    exception);
        }
        return loaded;
    }
}
