package com.sistemautil;

import com.sistemautil.motd.MotdCommand;
import com.sistemautil.motd.MotdListener;
import com.sistemautil.motd.MotdPlugin;
import com.sistemautil.motd.TPSMonitor;
import com.sistemautil.tab.ServerTabManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SistemaUtil extends JavaPlugin {
    private final TPSMonitor tpsMonitor = new TPSMonitor();
    private MotdPlugin motd;
    private ServerTabManager tabManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        motd = new MotdPlugin(this);
        motd.enable();

        getServer().getPluginManager().registerEvents(new MotdListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerCommandGuardListener(), this);
        tpsMonitor.start(this);

        tabManager = new ServerTabManager(this);
        tabManager.start();

        MotdCommand motdCommand = new MotdCommand(this);
        if (getCommand("motdplus") != null) {
            getCommand("motdplus").setExecutor(motdCommand);
            getCommand("motdplus").setTabCompleter(motdCommand);
        }

        getLogger().info("SistemaUtil ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        if (tabManager != null) tabManager.stop();
        tpsMonitor.stop();
    }

    public TPSMonitor getTpsMonitor() {
        return tpsMonitor;
    }

    public MotdPlugin getMotd() {
        return motd;
    }

    public ServerTabManager getTabManager() {
        return tabManager;
    }
}
