package com.sistemautil;

import com.sistemautil.config.ConfigManager;
import com.sistemautil.motd.MotdCommand;
import com.sistemautil.motd.MotdListener;
import com.sistemautil.motd.MotdPlugin;
import com.sistemautil.motd.TPSMonitor;
import com.sistemautil.tab.ServerTabManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SistemaUtil extends JavaPlugin {
    private final TPSMonitor tpsMonitor = new TPSMonitor();
    private final ConfigManager configManager = new ConfigManager(this);
    private MotdPlugin motd;
    private ServerTabManager tabManager;
    private UtilidadesPreferences utilidadesPreferences;
    private UtilidadesGui utilidadesGui;

    @Override
    public void onEnable() {
        configManager.loadAll();
        utilidadesPreferences = new UtilidadesPreferences(this);
        utilidadesPreferences.load();
        utilidadesGui = new UtilidadesGui(this, utilidadesPreferences);

        motd = new MotdPlugin(this);
        motd.enable();

        getServer().getPluginManager().registerEvents(new MotdListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerCommandGuardListener(), this);
        getServer().getPluginManager().registerEvents(utilidadesGui, this);
        getServer().getPluginManager().registerEvents(
                new JoinQuitNotificationListener(this, utilidadesPreferences), this);
        tpsMonitor.start(this);

        tabManager = new ServerTabManager(this);
        tabManager.start();

        MotdCommand motdCommand = new MotdCommand(this);
        if (getCommand("motdplus") != null) {
            getCommand("motdplus").setExecutor(motdCommand);
            getCommand("motdplus").setTabCompleter(motdCommand);
        }

        ConfigurarCommand configurarCommand = new ConfigurarCommand(utilidadesGui);
        if (getCommand("configurar") != null) {
            getCommand("configurar").setExecutor(configurarCommand);
            getCommand("configurar").setTabCompleter(configurarCommand);
        }

        getLogger().info("SistemaUtil ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        if (tabManager != null) tabManager.stop();
        if (utilidadesPreferences != null) utilidadesPreferences.save();
        tpsMonitor.stop();
    }

    public TPSMonitor getTpsMonitor() { return tpsMonitor; }
    public MotdPlugin getMotd() { return motd; }
    public ServerTabManager getTabManager() { return tabManager; }
    public FileConfiguration getMotdConfig() { return configManager.motd(); }
    public FileConfiguration getTabConfig() { return configManager.tab(); }
    public FileConfiguration getCorConfig() { return configManager.cor(); }
    public FileConfiguration getUtilidadesConfig() { return configManager.utilidades(); }

    public void reloadConfigs() {
        configManager.reloadAll();
        if (utilidadesPreferences != null) utilidadesPreferences.load();
        if (motd != null) motd.recarregar();
        if (tabManager != null) tabManager.start();
    }
}
