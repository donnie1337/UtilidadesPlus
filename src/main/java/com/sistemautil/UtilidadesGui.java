package com.sistemautil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;

public final class UtilidadesGui implements Listener {
    private static final String PERMISSION = "sistemautil.configurar";
    private final SistemaUtil plugin;
    private final UtilidadesPreferences preferences;

    public UtilidadesGui(SistemaUtil plugin, UtilidadesPreferences preferences) {
        this.plugin = plugin;
        this.preferences = preferences;
    }

    public void open(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§cVocê não tem permissão para usar este menu.");
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, size(), title());
        inventory.setItem(slot("entrada", 11), toggleItem(Material.LEVER, "Mensagem de entrada", preferences.receivesJoin(player)));
        inventory.setItem(slot("saida", 15), toggleItem(Material.SLIME_PISTON, "Mensagem de saída", preferences.receivesQuit(player)));
        if (player.hasPermission("cargoplus.cor")) {
            inventory.setItem(slot("cor", 13), colorItem(player));
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!title().equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot == slot("entrada", 11)) {
            boolean value = !preferences.receivesJoin(player);
            preferences.setReceivesJoin(player, value);
            player.sendMessage(value ? "§aMensagens de entrada ativadas." : "§cMensagens de entrada desativadas.");
            open(player);
            return;
        }
        if (rawSlot == slot("saida", 15)) {
            boolean value = !preferences.receivesQuit(player);
            preferences.setReceivesQuit(player, value);
            player.sendMessage(value ? "§aMensagens de saída ativadas." : "§cMensagens de saída desativadas.");
            open(player);
            return;
        }
        if (rawSlot == slot("cor", 13) && player.hasPermission("cargoplus.cor")) {
            player.closeInventory();
            player.performCommand("cor");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (title().equals(event.getView().getTitle())) event.setCancelled(true);
    }

    private ItemStack toggleItem(Material material, String name, boolean enabled) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§e" + name);
        meta.setLore(List.of(
                "§7Clique para " + (enabled ? "desativar" : "ativar") + ".",
                enabled ? "§a● ATIVADO" : "§c● DESATIVADO"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack colorItem(Player player) {
        ChatColor color = parseColor(currentChatColor(player));
        ItemStack item = new ItemStack(dyeFor(color));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "Cor da mensagem");
            meta.setLore(List.of(
                    "§7Cor atual: " + color + currentChatColorName(player),
                    "§eClique para abrir as lãs de cores."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String currentChatColor(Player player) {
        Plugin chat = Bukkit.getPluginManager().getPlugin("ChatPlus");
        if (chat == null || !chat.isEnabled()) return "§f";
        try {
            Method method = chat.getClass().getMethod("getCurrentChatColor", Player.class);
            Object result = method.invoke(chat, player);
            return result instanceof String value ? value : "§f";
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "§f";
        }
    }

    private String currentChatColorName(Player player) {
        Plugin chat = Bukkit.getPluginManager().getPlugin("ChatPlus");
        if (chat == null || !chat.isEnabled()) return "branco";
        try {
            Method method = chat.getClass().getMethod("getCurrentChatColorName", Player.class);
            Object result = method.invoke(chat, player);
            return result instanceof String value ? value : "branco";
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "branco";
        }
    }

    private ChatColor parseColor(String code) {
        if (code == null) return ChatColor.WHITE;
        String translated = ChatColor.translateAlternateColorCodes('&', code.trim());
        if (translated.length() == 2 && translated.charAt(0) == ChatColor.COLOR_CHAR) {
            ChatColor color = ChatColor.getByChar(translated.charAt(1));
            if (color != null) return color;
        }
        return ChatColor.WHITE;
    }

    private Material dyeFor(ChatColor color) {
        return switch (color) {
            case WHITE -> Material.WHITE_DYE;
            case GRAY, DARK_GRAY -> Material.GRAY_DYE;
            case DARK_RED, RED -> Material.RED_DYE;
            case GREEN -> Material.LIME_DYE;
            case DARK_GREEN -> Material.GREEN_DYE;
            case BLUE, DARK_BLUE -> Material.BLUE_DYE;
            case AQUA, DARK_AQUA -> Material.CYAN_DYE;
            case YELLOW -> Material.YELLOW_DYE;
            case GOLD -> Material.ORANGE_DYE;
            case LIGHT_PURPLE -> Material.PINK_DYE;
            case DARK_PURPLE -> Material.PURPLE_DYE;
            default -> Material.WHITE_DYE;
        };
    }

    private String title() {
        String raw = plugin.getUtilidadesConfig().getString("gui.titulo", "&8Utilidades do jogador");
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    private int size() {
        int size = plugin.getUtilidadesConfig().getInt("gui.tamanho", 27);
        return size >= 9 && size <= 54 && size % 9 == 0 ? size : 27;
    }

    private int slot(String key, int fallback) {
        return plugin.getUtilidadesConfig().getInt("gui.slots." + key, fallback);
    }
}
