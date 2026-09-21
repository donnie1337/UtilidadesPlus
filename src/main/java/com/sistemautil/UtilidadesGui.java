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
    private static final String MAIN_PATH = "gui.telas.principal";
    private static final String TELEPORT_PATH = "gui.telas.teletransporte";
    private static final String PREFERENCES_PATH = "gui.telas.mensagens";
    private final SistemaUtil plugin;
    private final UtilidadesPreferences preferences;

    public UtilidadesGui(SistemaUtil plugin, UtilidadesPreferences preferences) {
        this.plugin = plugin;
        this.preferences = preferences;
    }

    public void open(Player player) {
        if (!plugin.getUtilidadesConfig().getBoolean("gui.ativado", true)) return;
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§cVocê não tem permissão para usar este menu.");
            return;
        }

        Inventory inventory = createInventory(MAIN_PATH, "&8Utilidades do jogador");
        inventory.setItem(slot(MAIN_PATH, "teletransporte", 11), configuredItem(
                MAIN_PATH + ".itens.teletransporte", Material.OAK_BOAT,
                "&b&lTeletransporte", "", "&7Controle solicitações de TPA",
                "&7e mensagens privadas.", "", "&eClique para abrir."
        ));
        inventory.setItem(slot(MAIN_PATH, "mensagens", 15), configuredItem(
                MAIN_PATH + ".itens.mensagens", Material.COMPASS,
                "&e&lMensagens de entrada", "", "&7Configure suas mensagens,",
                "&7notificações e cor do chat.", "", "&eClique para abrir."
        ));
        player.openInventory(inventory);
    }

    private void openTeleport(Player player) {
        Inventory inventory = createInventory(TELEPORT_PATH, "&8Teletransporte e comunicação");
        inventory.setItem(slot(TELEPORT_PATH, "receber-tpa", 11), toggleItem(
                TELEPORT_PATH + ".itens.receber-tpa", Material.OAK_BOAT,
                "&b&lReceber TPA", preferences.receivesTpa(player),
                "&7Permite que outros jogadores", "&7enviem solicitações de TPA para você."
        ));
        inventory.setItem(slot(TELEPORT_PATH, "receber-tell", 15), toggleItem(
                TELEPORT_PATH + ".itens.receber-tell", Material.PAPER,
                "&e&lReceber /tell", preferences.receivesTell(player),
                "&7Permite que outros jogadores", "&7enviem mensagens privadas para você."
        ));
        inventory.setItem(backSlot(TELEPORT_PATH), configuredItem(
                TELEPORT_PATH + ".itens.voltar", Material.ARROW,
                "&fVoltar", "&7Voltar para configurações."
        ));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openPreferences(Player player) {
        Inventory inventory = createInventory(PREFERENCES_PATH, "&8Mensagens de entrada");
        inventory.setItem(slot(PREFERENCES_PATH, "entrada", 11), toggleItem(
                PREFERENCES_PATH + ".itens.entrada", Material.OAK_DOOR,
                "&e&lMensagens de entrada/saída",
                preferences.broadcastsJoinQuit(player),
                "&7Controla se sua entrada e saída",
                "&7podem ser exibidas para os jogadores."
        ));
        inventory.setItem(slot(PREFERENCES_PATH, "cor", 13), colorItem(player));
        inventory.setItem(slot(PREFERENCES_PATH, "notificacoes", 15), toggleItem(
                PREFERENCES_PATH + ".itens.notificacoes", Material.ENDER_EYE,
                "&b&lNotificações de entrada/saída",
                preferences.receivesJoin(player) && preferences.receivesQuit(player),
                "&7Controla se você recebe as",
                "&7mensagens de entrada e saída."
        ));
        inventory.setItem(backSlot(PREFERENCES_PATH), configuredItem(
                PREFERENCES_PATH + ".itens.voltar", Material.ARROW,
                "&fVoltar", "&7Voltar para configurações."
        ));
        fill(inventory);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        String mainTitle = title(MAIN_PATH, "&8Utilidades do jogador");
        String teleportTitle = title(TELEPORT_PATH, "&8Teletransporte e comunicação");
        String preferencesTitle = title(PREFERENCES_PATH, "&8Mensagens de entrada");
        if (!mainTitle.equals(title) && !teleportTitle.equals(title) && !preferencesTitle.equals(title)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int rawSlot = event.getRawSlot();

        if (mainTitle.equals(title)) {
            if (rawSlot == slot(MAIN_PATH, "teletransporte", 11)) openTeleport(player);
            else if (rawSlot == slot(MAIN_PATH, "mensagens", 15)) openPreferences(player);
            return;
        }

        if (teleportTitle.equals(title)) {
            if (rawSlot == slot(TELEPORT_PATH, "receber-tpa", 11)) {
                boolean value = !preferences.receivesTpa(player);
                preferences.setReceivesTpa(player, value);
                sendConfiguredMessage(player, value ? "mensagem-tpa-ativado" : "mensagem-tpa-desativado",
                        value ? "§b&lᴛᴘᴀ §8• §aVocê agora pode receber solicitações de TPA."
                                : "§b&lᴛᴘᴀ §8• §cVocê não receberá mais solicitações de TPA.");
                openTeleport(player);
                return;
            }
            if (rawSlot == slot(TELEPORT_PATH, "receber-tell", 15)) {
                boolean value = !preferences.receivesTell(player);
                preferences.setReceivesTell(player, value);
                sendConfiguredMessage(player, value ? "mensagem-tell-ativado" : "mensagem-tell-desativado",
                        value ? "§e&lᴄʜᴀᴛ §8• §aVocê agora pode receber mensagens privadas."
                                : "§e&lᴄʜᴀᴛ §8• §cVocê não receberá mais mensagens privadas.");
                openTeleport(player);
                return;
            }
            if (rawSlot == backSlot(TELEPORT_PATH)) open(player);
            return;
        }

        if (rawSlot == slot(PREFERENCES_PATH, "entrada", 11)) {
            boolean value = !preferences.broadcastsJoinQuit(player);
            preferences.setBroadcastsJoinQuit(player, value);
            sendConfiguredMessage(player, value ? "mensagem-entrada-ativada" : "mensagem-entrada-desativada",
                    value ? "§aSuas mensagens de entrada/saída agora são visíveis para todos."
                            : "§cSuas mensagens de entrada/saída foram ocultadas dos outros jogadores.");
            openPreferences(player);
            return;
        }
        if (rawSlot == slot(PREFERENCES_PATH, "notificacoes", 15)) {
            boolean value = !(preferences.receivesJoin(player) && preferences.receivesQuit(player));
            preferences.setReceivesJoin(player, value);
            preferences.setReceivesQuit(player, value);
            sendConfiguredMessage(player, value ? "mensagem-notificacoes-ativadas" : "mensagem-notificacoes-desativadas",
                    value ? "§aNotificações de entrada/saída ativadas."
                            : "§cNotificações de entrada/saída desativadas.");
            openPreferences(player);
            return;
        }
        if (rawSlot == slot(PREFERENCES_PATH, "cor", 13)) {
            player.closeInventory();
            player.performCommand("cor");
            return;
        }
        if (rawSlot == backSlot(PREFERENCES_PATH)) open(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(title(MAIN_PATH, "&8Utilidades do jogador"))
                || title.equals(title(TELEPORT_PATH, "&8Teletransporte e comunicação"))
                || title.equals(title(PREFERENCES_PATH, "&8Mensagens de entrada"))) {
            event.setCancelled(true);
        }
    }

    private Inventory createInventory(String path, String fallbackTitle) {
        int size = plugin.getUtilidadesConfig().getInt(path + ".tamanho", 27);
        if (size < 9 || size > 54 || size % 9 != 0) size = 27;
        return Bukkit.createInventory(null, size, title(path, fallbackTitle));
    }

    private String title(String path, String fallback) {
        return color(plugin.getUtilidadesConfig().getString(path + ".titulo", fallback));
    }

    private ItemStack configuredItem(String path, Material fallbackMaterial, String fallbackName, String... fallbackLore) {
        Material material = material(path + ".material", fallbackMaterial);
        String name = plugin.getUtilidadesConfig().getString(path + ".nome", fallbackName);
        List<String> lore = plugin.getUtilidadesConfig().getStringList(path + ".lore");
        if (lore.isEmpty()) lore = List.of(fallbackLore);
        return item(material, name, lore);
    }

    private ItemStack toggleItem(String path, Material fallbackMaterial, String fallbackName,
                                 boolean enabled, String... fallbackLore) {
        Material material = material(path + ".material", fallbackMaterial);
        String name = plugin.getUtilidadesConfig().getString(path + ".nome", fallbackName);
        List<String> lore = plugin.getUtilidadesConfig().getStringList(path + ".lore");
        if (lore.isEmpty()) lore = List.of(fallbackLore);

        String status = enabled
                ? plugin.getUtilidadesConfig().getString("gui.textos.status-ativado", "&a● ATIVADO")
                : plugin.getUtilidadesConfig().getString("gui.textos.status-desativado", "&c● DESATIVADO");
        String action = enabled
                ? plugin.getUtilidadesConfig().getString("gui.textos.acao-desativar", "&8Clique para desativar.")
                : plugin.getUtilidadesConfig().getString("gui.textos.acao-ativar", "&8Clique para ativar.");

        List<String> parsed = new ArrayList<>();
        for (String line : lore) parsed.add(apply(line, status, action, null, null));
        return item(material, apply(name, status, action, null, null), parsed);
    }

    private ItemStack colorItem(Player player) {
        String path = PREFERENCES_PATH + ".itens.cor";
        ChatColor color = parseColor(currentChatColor(player));
        String colorCode = color.toString();
        String colorName = currentChatColorName(player);
        String name = plugin.getUtilidadesConfig().getString(path + ".nome", "&fCor da mensagem");
        String materialName = plugin.getUtilidadesConfig().getString(path + ".material", "");
        Material material = materialName.isBlank() ? dyeFor(color) : material(path + ".material", dyeFor(color));

        List<String> lore = plugin.getUtilidadesConfig().getStringList(path + ".lore");
        if (lore.isEmpty()) {
            lore = List.of("", "&7Sua cor atual:", "&f▸ {cor}{nome-cor}", "",
                    "&7Escolha uma nova cor para", "&7as suas mensagens no chat.", "",
                    "&eClique para abrir as cores.");
        }

        List<String> parsed = new ArrayList<>();
        for (String line : lore) parsed.add(apply(line, null, null, colorCode, colorName));
        return item(material, apply(name, null, null, colorCode, colorName), parsed);
    }

    private String apply(String text, String status, String action, String color, String colorName) {
        if (text == null) return "";
        return color(text.replace("{status}", status == null ? "" : status)
                .replace("{acao}", action == null ? "" : action)
                .replace("{cor}", color == null ? "§f" : color)
                .replace("{nome-cor}", colorName == null ? "branco" : colorName));
    }

    private void sendConfiguredMessage(Player player, String key, String fallback) {
        player.sendMessage(color(plugin.getUtilidadesConfig().getString("gui.mensagens." + key, fallback)));
    }

    private ItemStack item(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(color(name));
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) lore.add(color(line));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private Material material(String path, Material fallback) {
        String value = plugin.getUtilidadesConfig().getString(path);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Material.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private int slot(String path, String key, int fallback) {
        return plugin.getUtilidadesConfig().getInt(path + ".itens." + key + ".slot", fallback);
    }

    private int backSlot(String path) {
        return slot(path, "voltar", 22);
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
}
