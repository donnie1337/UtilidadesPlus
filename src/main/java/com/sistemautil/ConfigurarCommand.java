package com.sistemautil;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ConfigurarCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "sistemautil.configurar";
    private final UtilidadesGui gui;

    public ConfigurarCommand(UtilidadesGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }
        if (!hasConfigPermission(player)) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /" + label);
            return true;
        }
        gui.open(player);
        return true;
    }

    private boolean hasConfigPermission(Player player) {
        if (player.hasPermission(PERMISSION)) return true;

        // O SistemaPerm é a fonte de cargos do servidor. Como a permissão
        // do comando fica com default=false, verificamos também o cargo
        // diretamente para que todos os cargos de staff tenham acesso,
        // inclusive em instalações que ainda possuem um config.yml antigo.
        Plugin cargoPlus = player.getServer().getPluginManager().getPlugin("CargoPlus");
        if (cargoPlus == null || !cargoPlus.isEnabled()) return false;

        try {
            Method getApi = cargoPlus.getClass().getMethod("api");
            Object api = getApi.invoke(cargoPlus);
            if (api == null) return false;

            Method getGroup = api.getClass().getMethod("getGroup", UUID.class);
            Object group = getGroup.invoke(api, player.getUniqueId());
            if (!(group instanceof String groupName)) return false;

            return !groupName.trim().equalsIgnoreCase("membro") && !groupName.trim().isEmpty();
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
