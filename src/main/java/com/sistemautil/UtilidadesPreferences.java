package com.sistemautil;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UtilidadesPreferences {
    private final JavaPlugin plugin;
    private final File file;
    private final AtomicBoolean saveScheduled = new AtomicBoolean();
    private FileConfiguration config;

    public UtilidadesPreferences(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "utilidades.yml");
    }

    public void load() {
        synchronized (this) {
            config = YamlConfiguration.loadConfiguration(file);
        }
    }

    public boolean globallyReceivesJoin() {
        synchronized (this) { return config.getBoolean("receber.entrada", true); }
    }

    public boolean globallyReceivesQuit() {
        synchronized (this) { return config.getBoolean("receber.saida", true); }
    }

    public boolean receivesJoin(Player player) { return get(player, "entrada", globallyReceivesJoin()); }
    public boolean receivesQuit(Player player) { return get(player, "saida", globallyReceivesQuit()); }
    public void setReceivesJoin(Player player, boolean value) { set(player, "entrada", value); }
    public void setReceivesQuit(Player player, boolean value) { set(player, "saida", value); }

    public boolean broadcastsJoin(Player player) { return get(player, "mostrar-entrada", true); }
    public boolean broadcastsQuit(Player player) { return get(player, "mostrar-saida", true); }
    public void setBroadcastsJoin(Player player, boolean value) { set(player, "mostrar-entrada", value); }
    public void setBroadcastsQuit(Player player, boolean value) { set(player, "mostrar-saida", value); }

    public boolean broadcastsJoinQuit(Player player) { return broadcastsJoin(player) && broadcastsQuit(player); }

    public void setBroadcastsJoinQuit(Player player, boolean value) {
        set(player, "mostrar-entrada", value);
        set(player, "mostrar-saida", value);
    }

    public void save() {
        final String snapshot;
        synchronized (this) { snapshot = config.saveToString(); }
        saveSnapshot(snapshot);
    }

    private void scheduleAsyncSave() {
        if (!saveScheduled.compareAndSet(false, true)) return;
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                final String snapshot;
                synchronized (this) { snapshot = config.saveToString(); }
                saveSnapshot(snapshot);
            } finally {
                saveScheduled.set(false);
            }
        }, 1L);
    }

    private void saveSnapshot(String snapshot) {
        try {
            file.getParentFile().mkdirs();
            java.nio.file.Files.writeString(file.toPath(), snapshot, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não foi possível salvar as preferências de utilidades: " + ex.getMessage());
        }
    }

    private boolean get(Player player, String path, boolean defaultValue) {
        if (player == null) return defaultValue;
        UUID uuid = player.getUniqueId();
        synchronized (this) { return config.getBoolean("jogadores." + uuid + "." + path, defaultValue); }
    }

    private void set(Player player, String path, boolean value) {
        if (player == null) return;
        synchronized (this) { config.set("jogadores." + player.getUniqueId() + "." + path, value); }
        scheduleAsyncSave();
    }
}
