package com.sistemautil;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class UtilidadesPreferences {
    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public UtilidadesPreferences(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "utilidades.yml");
    }

    public void load() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean receivesJoin(Player player) {
        return get(player, "entrada");
    }

    public boolean receivesQuit(Player player) {
        return get(player, "saida");
    }

    public void setReceivesJoin(Player player, boolean value) {
        set(player, "entrada", value);
    }

    public void setReceivesQuit(Player player, boolean value) {
        set(player, "saida", value);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não foi possível salvar as preferências de utilidades: " + ex.getMessage());
        }
    }

    private boolean get(Player player, String path) {
        if (player == null) return true;
        UUID uuid = player.getUniqueId();
        return config.getBoolean("jogadores." + uuid + "." + path, config.getBoolean("receber." + path, true));
    }

    private void set(Player player, String path, boolean value) {
        config.set("jogadores." + player.getUniqueId() + "." + path, value);
        save();
    }
}
