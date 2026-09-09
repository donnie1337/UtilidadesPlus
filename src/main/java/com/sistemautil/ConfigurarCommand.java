package com.sistemautil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class ConfigurarCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "utilidadesplus.configurar";
    private final UtilidadesGui gui;

    public ConfigurarCommand(UtilidadesGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c✖ §fEste comando só pode ser usado por jogadores.");
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§c✖ §fVocê não tem permissão para executar este comando.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage("§e➜ §fUso: /" + label);
            return true;
        }
        gui.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
