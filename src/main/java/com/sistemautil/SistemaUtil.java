package com.sistemautil;

import com.sistemautil.motd.MotdCommand;
import com.sistemautil.motd.MotdListener;
import com.sistemautil.motd.MotdPlugin;
import com.sistemautil.motd.TPSMonitor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class SistemaUtil extends JavaPlugin {
    private final TPSMonitor tpsMonitor = new TPSMonitor();
    private MotdPlugin motd;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        motd = new MotdPlugin(this);
        motd.enable();

        getServer().getPluginManager().registerEvents(new MotdListener(this), this);
        tpsMonitor.start(this);

        MotdCommand motdCommand = new MotdCommand(this);
        if (getCommand("motdplus") != null) {
            getCommand("motdplus").setExecutor(motdCommand);
            getCommand("motdplus").setTabCompleter(motdCommand);
        }

        getLogger().info("SistemaUtil ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        tpsMonitor.stop();
    }

    public void reloadPlugin(CommandSender sender) {
        reloadConfig();
        motd.recarregar();
        sender.sendMessage(ChatColor.GREEN + "Configuração do SistemaUtil recarregada!");
    }

    public TPSMonitor getTpsMonitor() {
        return tpsMonitor;
    }

    public MotdPlugin getMotd() {
        return motd;
    }
}
