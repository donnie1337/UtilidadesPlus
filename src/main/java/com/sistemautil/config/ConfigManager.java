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
 * Centraliza os arquivos de configuração independentes do SistemaUtil.
 */
public final class ConfigManager {
    private final JavaPlugin plugin;
    private final File motdFile;
    private final File tabFile;
    private final File corFile;

    private FileConfiguration motd;
    private FileConfiguration tab;
    private FileConfiguration cor;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.motdFile = new File(plugin.getDataFolder(), "motd.yml");
        this.tabFile = new File(plugin.getDataFolder(), "tab.yml");
        this.corFile = new File(plugin.getDataFolder(), "cor.yml");
    }

    public void loadAll() {
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

    private FileConfiguration load(String resource, File file) {
        if (!file.exists()) {
            plugin.saveResource(resource, false);
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
