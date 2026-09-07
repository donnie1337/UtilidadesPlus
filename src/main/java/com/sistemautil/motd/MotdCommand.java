package com.sistemautil.motd;

import com.sistemautil.SistemaUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.*;

import java.util.Collections;
import java.util.List;

public final class MotdCommand implements CommandExecutor, TabCompleter {
    private final SistemaUtil plugin;

    public MotdCommand(SistemaUtil plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sistemautil.motd.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " reload");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin(sender);
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Subcomando desconhecido. Use /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sistemautil.motd.admin")) return Collections.emptyList();
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase())) return List.of("reload");
        return Collections.emptyList();
    }
}
