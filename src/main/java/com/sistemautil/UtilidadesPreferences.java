package com.sistemautil;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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

    // Controle global do que os jogadores conseguem visualizar.
    public boolean globallyReceivesJoin() {
        return config.getBoolean("receber.entrada", true);
    }

    public boolean globallyReceivesQuit() {
        return config.getBoolean("receber.saida", true);
    }

    // Controle individual do que o jogador consegue visualizar.
    public boolean receivesJoin(Player player) { return get(player, "entrada", globallyReceivesJoin()); }
    public boolean receivesQuit(Player player) { return get(player, "saida", globallyReceivesQuit()); }
    public void setReceivesJoin(Player player, boolean value) { set(player, "entrada", value); }
    public void setReceivesQuit(Player player, boolean value) { set(player, "saida", value); }

    // Controle individual de quem pode gerar mensagens de entrada/saida.
    // Um jogador desligado nao aparece para os demais.
    public boolean broadcastsJoin(Player player) { return get(player, "mostrar-entrada", true); }
    public boolean broadcastsQuit(Player player) { return get(player, "mostrar-saida", true); }
    public void setBroadcastsJoin(Player player, boolean value) { set(player, "mostrar-entrada", value); }
    public void setBroadcastsQuit(Player player, boolean value) { set(player, "mostrar-saida", value); }

    // O toggle da alavanca controla entrada + saida juntos.
    public boolean broadcastsJoinQuit(Player player) {
        return broadcastsJoin(player) && broadcastsQuit(player);
    }

    public void setBroadcastsJoinQuit(Player player, boolean value) {
        set(player, "mostrar-entrada", value);
        set(player, "mostrar-saida", value);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não foi possível salvar as preferências de utilidades: " + ex.getMessage());
        }
    }

    private boolean get(Player player, String path, boolean defaultValue) {
        if (player == null) return defaultValue;
        UUID uuid = player.getUniqueId();
        return config.getBoolean("jogadores." + uuid + "." + path, defaultValue);
    }

    private void set(Player player, String path, boolean value) {
        config.set("jogadores." + player.getUniqueId() + "." + path, value);
        save();
    }
}
