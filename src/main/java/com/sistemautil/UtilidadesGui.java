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
import java.util.ArrayList;
import java.util.List;

public final class UtilidadesGui implements Listener {
    private static final String PERMISSION = "utilidadesplus.configurar";
    private static final String MAIN_TITLE = "§8Configurações";
    private static final String TELEPORT_TITLE = "§8Teletransporte e comunicação";
    private static final String PREFERENCES_TITLE = "§8Mensagens de entrada";
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

        Inventory inventory = Bukkit.createInventory(null, size(), MAIN_TITLE);
        inventory.setItem(slot("teletransporte", 11), item(
                Material.OAK_BOAT,
                "§b&lTeletransporte",
                "",
                "§7Controle solicitações de TPA",
                "§7e mensagens privadas.",
                "",
                "§eClique para abrir."
        ));
        inventory.setItem(slot("mensagens", 15), item(
                Material.COMPASS,
                "§e&lMensagens de entrada",
                "",
                "§7Configure suas mensagens,",
                "§7notificações e cor do chat.",
                "",
                "§eClique para abrir."
        ));

        player.openInventory(inventory);
    }

    private void openTeleport(Player player) {
        Inventory inventory = Bukkit.createInventory(null, size(), TELEPORT_TITLE);
        inventory.setItem(slot("receber-tpa", 11), toggleItem(
                Material.OAK_BOAT,
                "§b&lReceber TPA",
                preferences.receivesTpa(player),
                "§7Permite que outros jogadores",
                "§7enviem solicitações de TPA para você."
        ));
        inventory.setItem(slot("receber-tell", 15), toggleItem(
                Material.PAPER,
                "§e&lReceber /tell",
                preferences.receivesTell(player),
                "§7Permite que outros jogadores",
                "§7enviem mensagens privadas para você."
        ));
        inventory.setItem(22, item(Material.ARROW, "§fVoltar", "§7Voltar para configurações."));
        player.openInventory(inventory);
    }

    private void openPreferences(Player player) {
        Inventory inventory = Bukkit.createInventory(null, size(), PREFERENCES_TITLE);
        inventory.setItem(slot("entrada", 11), toggleItem(
                Material.OAK_DOOR,
                "§e&lMensagens de entrada/saída",
                preferences.broadcastsJoinQuit(player),
                "§7Controla se sua entrada e saída",
                "§7podem ser exibidas para os jogadores."
        ));
        inventory.setItem(slot("cor", 13), colorItem(player));
        inventory.setItem(slot("notificacoes", 15), toggleItem(
                Material.ENDER_EYE,
                "§b&lNotificações de entrada/saída",
                preferences.receivesJoin(player) && preferences.receivesQuit(player),
                "§7Controla se você recebe as",
                "§7mensagens de entrada e saída."
        ));
        inventory.setItem(22, item(Material.ARROW, "§fVoltar", "§7Voltar para configurações."));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!MAIN_TITLE.equals(title) && !TELEPORT_TITLE.equals(title) && !PREFERENCES_TITLE.equals(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int rawSlot = event.getRawSlot();

        if (MAIN_TITLE.equals(title)) {
            if (rawSlot == slot("teletransporte", 11)) {
                openTeleport(player);
            } else if (rawSlot == slot("mensagens", 15)) {
                openPreferences(player);
            }
            return;
        }

        if (TELEPORT_TITLE.equals(title)) {
            if (rawSlot == slot("receber-tpa", 11)) {
                boolean value = !preferences.receivesTpa(player);
                preferences.setReceivesTpa(player, value);
                player.sendMessage(value
                        ? "§b&lᴛᴘᴀ §8• §aVocê agora pode receber solicitações de TPA."
                        : "§b&lᴛᴘᴀ §8• §cVocê não receberá mais solicitações de TPA.");
                openTeleport(player);
                return;
            }
            if (rawSlot == slot("receber-tell", 15)) {
                boolean value = !preferences.receivesTell(player);
                preferences.setReceivesTell(player, value);
                player.sendMessage(value
                        ? "§e&lᴄʜᴀᴛ §8• §aVocê agora pode receber mensagens privadas."
                        : "§e&lᴄʜᴀᴛ §8• §cVocê não receberá mais mensagens privadas.");
                openTeleport(player);
                return;
            }
            if (rawSlot == 22) open(player);
            return;
        }

        if (rawSlot == slot("entrada", 11)) {
            boolean value = !preferences.broadcastsJoinQuit(player);
            preferences.setBroadcastsJoinQuit(player, value);
            player.sendMessage(value
                    ? "§aSuas mensagens de entrada/saída agora são visíveis para todos."
                    : "§cSuas mensagens de entrada/saída foram ocultadas dos outros jogadores.");
            openPreferences(player);
            return;
        }
        if (rawSlot == slot("notificacoes", 15)) {
            boolean value = !(preferences.receivesJoin(player) && preferences.receivesQuit(player));
            preferences.setReceivesJoin(player, value);
            preferences.setReceivesQuit(player, value);
            player.sendMessage(value
                    ? "§aNotificações de entrada/saída ativadas."
                    : "§cNotificações de entrada/saída desativadas.");
            openPreferences(player);
            return;
        }
        if (rawSlot == slot("cor", 13)) {
            player.closeInventory();
            player.performCommand("cor");
            return;
        }
        if (rawSlot == 22) open(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (MAIN_TITLE.equals(title) || TELEPORT_TITLE.equals(title) || PREFERENCES_TITLE.equals(title)) event.setCancelled(true);
    }

    private ItemStack toggleItem(Material material, String name, boolean enabled, String... description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String line : description) lore.add(ChatColor.translateAlternateColorCodes('&', line));
        lore.add("");
        lore.add(enabled ? "§a● ATIVADO" : "§c● DESATIVADO");
        lore.add("§8Clique para " + (enabled ? "desativar" : "ativar") + ".");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack item(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) lore.add(ChatColor.translateAlternateColorCodes('&', line));
        meta.setLore(lore);
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
                    "",
                    "§7Sua cor atual:",
                    "§f▸ " + color + currentChatColorName(player),
                    "",
                    "§7Escolha uma nova cor para",
                    "§7as suas mensagens no chat.",
                    "",
                    "§eClique para abrir as cores."
            ));
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

    private int size() {
        int size = plugin.getUtilidadesConfig().getInt("gui.tamanho", 27);
        return size >= 9 && size <= 54 && size % 9 == 0 ? size : 27;
    }

    private int slot(String key, int fallback) {
        return plugin.getUtilidadesConfig().getInt("gui.slots." + key, fallback);
    }
}
