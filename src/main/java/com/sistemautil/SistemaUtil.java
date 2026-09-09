package com.sistemautil;

import com.sistemautil.config.ConfigManager;
import com.sistemautil.motd.MotdCommand;
import com.sistemautil.motd.MotdListener;
import com.sistemautil.motd.MotdPlugin;
import com.sistemautil.motd.TPSMonitor;
import com.sistemautil.tab.ServerTabManager;
import com.sistemautil.visual.VisualText;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class SistemaUtil extends JavaPlugin {
    private final TPSMonitor tpsMonitor = new TPSMonitor();
    private final ConfigManager configManager = new ConfigManager(this);
    private MotdPlugin motd;
    private ServerTabManager tabManager;
    private UtilidadesPreferences utilidadesPreferences;
    private UtilidadesGui utilidadesGui;
    private VisualText visualText;
    private BukkitTask collisionTask;

    @Override
    public void onEnable() {
        configManager.loadAll();
        visualText = new VisualText(configManager.utilidades());
        utilidadesPreferences = new UtilidadesPreferences(this);
        utilidadesPreferences.load();
        utilidadesGui = new UtilidadesGui(this, utilidadesPreferences);

        motd = new MotdPlugin(this);
        motd.enable();

        getServer().getPluginManager().registerEvents(new MotdListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerCommandGuardListener(), this);
        getServer().getPluginManager().registerEvents(new RestrictedCommandTabListener(), this);
        getServer().getPluginManager().registerEvents(new CargoTabRefreshListener(this), this);
        getServer().getPluginManager().registerEvents(utilidadesGui, this);
        getServer().getPluginManager().registerEvents(
                new JoinQuitNotificationListener(this, utilidadesPreferences), this);

        PlayerCollisionListener collisionListener = new PlayerCollisionListener();
        getServer().getPluginManager().registerEvents(collisionListener, this);
        for (Player player : Bukkit.getOnlinePlayers()) collisionListener.disableCollision(player);

        collisionTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) collisionListener.disableCollision(player);
        }, 100L, 100L);

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

        getLogger().info("UtilidadesPlus ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        if (collisionTask != null) collisionTask.cancel();
        if (tabManager != null) tabManager.stop();
        if (utilidadesPreferences != null) utilidadesPreferences.save();
        tpsMonitor.stop();
    }

    public TPSMonitor getTpsMonitor() { return tpsMonitor; }
    public MotdPlugin getMotd() { return motd; }
    public ServerTabManager getTabManager() { return tabManager; }
    public VisualText getVisualText() { return visualText; }
    public org.bukkit.configuration.file.FileConfiguration getMotdConfig() { return configManager.motd(); }
    public org.bukkit.configuration.file.FileConfiguration getTabConfig() { return configManager.tab(); }
    public org.bukkit.configuration.file.FileConfiguration getUtilidadesConfig() { return configManager.utilidades(); }

    public void reloadConfigs() {
        configManager.reloadAll();
        if (visualText != null) visualText.reload();
        if (utilidadesPreferences != null) utilidadesPreferences.load();
        if (motd != null) motd.recarregar();
        if (tabManager != null) tabManager.start();
    }
}
