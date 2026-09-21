package com.sistemautil.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private final File dataFolder;
    private final File motdFile;
    private final File tabFile;
    private final File utilidadesFile;
    private final File utilidadesBackupFile;

    private FileConfiguration motd;
    private FileConfiguration tab;
    private FileConfiguration utilidades;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.motdFile = new File(dataFolder, "motd.yml");
        this.tabFile = new File(dataFolder, "tab.yml");
        this.utilidadesFile = new File(dataFolder, "utilidades.yml");
        this.utilidadesBackupFile = new File(dataFolder, "utilidades.yml.backup");
    }

    public void loadAll() {
        ensureDataFolder();
        motd = load("motd.yml", motdFile);
        tab = load("tab.yml", tabFile);
        utilidades = loadUtilidades();
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

    public FileConfiguration utilidades() {
        return utilidades;
    }

    private void ensureDataFolder() {
        if (dataFolder.exists()) return;

        if (!dataFolder.mkdirs() && !dataFolder.exists()) {
            plugin.getLogger().warning(
                    "Não foi possível criar a pasta de configurações: "
                            + dataFolder.getAbsolutePath()
            );
        }
    }

    private FileConfiguration load(String resource, File file) {
        if (!file.exists()) {
            try {
                plugin.saveResource(resource, false);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "O recurso padrão " + resource
                                + " não está presente no JAR do UtilidadesPlus.",
                        exception
                );
            }
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    private FileConfiguration loadUtilidades() {
        /*
         * utilidades.yml é uma configuração editável pelo administrador.
         * Nunca devemos substituir uma configuração personalizada pelo recurso
         * padrão empacotado no JAR.
         *
         * O backup existe como proteção adicional contra processos externos
         * que apagam/substituem a pasta do plugin durante uma atualização.
         */
        if (!utilidadesFile.exists()) {
            if (utilidadesBackupFile.exists()) {
                restoreBackup();
            } else {
                createResourceIfMissing("utilidades.yml", utilidadesFile);
            }
        } else if (utilidadesBackupFile.exists() && isIdenticalToResource(utilidadesFile, "utilidades.yml")) {
            restoreBackup();
        }

        FileConfiguration loaded = YamlConfiguration.loadConfiguration(utilidadesFile);

        if (utilidadesFile.exists() && !isIdenticalToResource(utilidadesFile, "utilidades.yml")) {
            backupUtilidades();
        }

        plugin.getLogger().info(
                "Configuração utilidades.yml carregada de: "
                        + utilidadesFile.getAbsolutePath()
        );

        return loaded;
    }

    private void createResourceIfMissing(String resource, File file) {
        try {
            plugin.saveResource(resource, false);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "O recurso padrão " + resource
                            + " não está presente no JAR do UtilidadesPlus.",
                    exception
            );
        }
    }

    private boolean isIdenticalToResource(File file, String resource) {
        try (InputStream stream = plugin.getResource(resource)) {
            if (stream == null || !file.exists()) return false;

            byte[] resourceBytes = stream.readAllBytes();
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            return java.util.Arrays.equals(resourceBytes, fileBytes);
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "Não foi possível comparar " + resource + " com a configuração do servidor.",
                    exception
            );
            return false;
        }
    }

    private void backupUtilidades() {
        try {
            Files.copy(
                    utilidadesFile.toPath(),
                    utilidadesBackupFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "Não foi possível criar o backup de utilidades.yml.",
                    exception
            );
        }
    }

    private void restoreBackup() {
        try {
            Files.copy(
                    utilidadesBackupFile.toPath(),
                    utilidadesFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            plugin.getLogger().info(
                    "utilidades.yml padrão detectado. Configuração personalizada restaurada do backup."
            );
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Não foi possível restaurar o backup de utilidades.yml.",
                    exception
            );
        }
    }
}
