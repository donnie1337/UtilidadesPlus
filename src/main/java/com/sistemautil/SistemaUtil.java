package com.sistemautil;

import com.sistemautil.chat.ChatColorGui;
import com.sistemautil.motd.MotdCommand;
import com.sistemautil.motd.MotdListener;
import com.sistemautil.motd.TPSMonitor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SistemaUtil extends JavaPlugin {
    private final TPSMonitor tpsMonitor = new TPSMonitor();
    private ChatColorGui chatColorGui;
    private SystemPermBridge systemPermBridge;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        systemPermBridge = new SystemPermBridge(this);
        chatColorGui = new ChatColorGui(this);

        getServer().getPluginManager().registerEvents(new MotdListener(this), this);
        getServer().getPluginManager().registerEvents(chatColorGui, this);
        tpsMonitor.start(this);

        MotdCommand motd = new MotdCommand(this);
        if (getCommand("motdplus") != null) {
            getCommand("motdplus").setExecutor(motd);
            getCommand("motdplus").setTabCompleter(motd);
        }

        ChatColorCommand cor = new ChatColorCommand(this);
        if (getCommand("cor") != null) {
            getCommand("cor").setExecutor(cor);
            getCommand("cor").setTabCompleter(cor);
        }

        getLogger().info("SistemaUtil ativado com sucesso.");
        if (systemPermBridge.isAvailable()) {
            getLogger().info("Integração com SistemaPerm detectada.");
        } else {
            getLogger().info("SistemaPerm não detectado. Recursos independentes continuam ativos.");
        }
    }

    @Override
    public void onDisable() {
        tpsMonitor.stop();
    }

    public void reloadPlugin(CommandSender sender) {
        reloadConfig();
        systemPermBridge.refresh();
        sender.sendMessage(ChatColor.GREEN + "Configuração do SistemaUtil recarregada!");
    }

    public TPSMonitor getTpsMonitor() { return tpsMonitor; }
    public ChatColorGui getChatColorGui() { return chatColorGui; }
    public SystemPermBridge getSystemPermBridge() { return systemPermBridge; }

    public Map<String, String> chatColors() {
        Map<String, String> result = new LinkedHashMap<>();
        var section = getConfig().getConfigurationSection("chat.colors");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String value = section.getString(key);
                if (value != null && parse(value) != null) result.put(key.toLowerCase(Locale.ROOT), value);
            }
        }
        return result;
    }

    public ChatColor resolveChatColor(String name) {
        String value = chatColors().get(name == null ? "" : name.toLowerCase(Locale.ROOT));
        ChatColor color = parse(value);
        return color == null ? ChatColor.WHITE : color;
    }

    public String defaultChatColorName() {
        String configured = getConfig().getString("chat.default-color", "branco");
        return chatColors().containsKey(configured.toLowerCase(Locale.ROOT)) ? configured.toLowerCase(Locale.ROOT) : "branco";
    }

    public String getCurrentChatColorName(Player player) {
        if (player == null) return defaultChatColorName();
        if (systemPermBridge.isAvailable()) {
            String code = systemPermBridge.getChatColorCode(player);
            String matched = findColorName(code);
            if (matched != null) return matched;
        }
        return defaultChatColorName();
    }

    public boolean setChatColor(Player player, String color) {
        if (player == null || !player.hasPermission("cargoplus.cor")) return false;
        String normalized = color == null ? "" : color.toLowerCase(Locale.ROOT);
        if (!chatColors().containsKey(normalized)) return false;

        if (systemPermBridge.isAvailable()) {
            return systemPermBridge.setChatColor(player, normalized);
        }
        return false;
    }

    private String findColorName(String code) {
        if (code == null) return null;
        ChatColor target = parse(code);
        if (target == null) return null;
        for (Map.Entry<String, String> entry : chatColors().entrySet()) {
            if (parse(entry.getValue()) == target) return entry.getKey();
        }
        return null;
    }

    private static ChatColor parse(String value) {
        String translated = ChatColor.translateAlternateColorCodes('&', value == null ? "" : value.trim());
        if (translated.length() != 2 || translated.charAt(0) != ChatColor.COLOR_CHAR) return null;
        return ChatColor.getByChar(translated.charAt(1));
    }
}
