package com.sistemautil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JoinQuitNotificationListener implements Listener {
    private static final String AUTH_PLUGIN_NAME = "LoginPlus";
    private static final Pattern GRADIENT = Pattern.compile("<gradient:#([0-9a-fA-F]{6}):#([0-9a-fA-F]{6})>(.*?)</gradient>", Pattern.DOTALL);
    private final SistemaUtil plugin;
    private final UtilidadesPreferences preferences;
    private volatile Plugin authPlugin;
    private volatile Method authCheckMethod;
    private volatile Plugin cargoPlugin;
    private volatile Method cargoPermissionsMethod;
    private volatile Method cargoGroupMethod;
    private volatile Method cargoDisplayNameMethod;
    private volatile Method cargoPrefixMethod;
    private volatile Method cargoReceivesMessageMethod;

    public JoinQuitNotificationListener(SistemaUtil plugin, UtilidadesPreferences preferences) { this.plugin = plugin; this.preferences = preferences; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        checkJoin(event.getPlayer(), 0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        if (!preferences.globallyReceivesQuit() || !preferences.broadcastsQuit(event.getPlayer())) return;
        if (!plugin.getUtilidadesConfig().getBoolean("mensagens-saida.ativado", false)) return;
        String group = cargoGroup(event.getPlayer());
        if (group.isBlank() || !cargoReceivesMessage(group)) return;
        String message = plugin.getUtilidadesConfig().getString("mensagens-saida.mensagem", "");
        if (message.isBlank()) return;
        message = formatMessage(message, event.getPlayer(), group);
        for (Player viewer : Bukkit.getOnlinePlayers()) if (preferences.receivesQuit(viewer)) viewer.sendMessage(message);
    }

    private void checkJoin(Player player, int attempt) {
        if (!player.isOnline() || attempt >= 60) return;
        if (!isAuthenticated(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkJoin(player, attempt + 1), 20L);
            return;
        }
        if (!preferences.broadcastsJoin(player)) return;
        if (!plugin.getUtilidadesConfig().getBoolean("mensagens-entrada.ativado", true)) return;
        String group = cargoGroup(player);
        if (group.isBlank() || !cargoReceivesMessage(group)) return;
        String message = joinMessage();
        if (message.isBlank()) return;
        message = formatMessage(message, player, group);
        for (Player viewer : Bukkit.getOnlinePlayers()) if (viewer.equals(player) || preferences.receivesJoin(viewer)) viewer.sendMessage(message);
    }

    private boolean isAuthenticated(Player player) {
        if (player == null || !player.isOnline()) return false;
        Plugin current = Bukkit.getPluginManager().getPlugin(AUTH_PLUGIN_NAME);
        if (current == null || !current.isEnabled()) return true;
        Method method = authCheckMethod;
        if (authPlugin != current || method == null) {
            synchronized (this) {
                if (authPlugin != current || authCheckMethod == null) {
                    authPlugin = current;
                    try { authCheckMethod = current.getClass().getMethod("isAuthenticated", Player.class); }
                    catch (ReflectiveOperationException | LinkageError ex) { authCheckMethod = null; }
                    method = authCheckMethod;
                }
            }
        }
        if (method == null) return false;
        try { Object result = method.invoke(current, player); return result instanceof Boolean value && value; }
        catch (ReflectiveOperationException | LinkageError ex) { return false; }
    }

    private String cargoGroup(Player player) {
        Object permissions = cargoPermissions();
        if (permissions == null || cargoGroupMethod == null) return "";
        try { Object result = cargoGroupMethod.invoke(permissions, player.getUniqueId()); return result instanceof String value ? value.trim().toLowerCase(Locale.ROOT) : ""; }
        catch (ReflectiveOperationException | LinkageError ex) { return ""; }
    }

    private boolean cargoReceivesMessage(String group) {
        Plugin cargo = cargoPlugin();
        if (cargo == null || cargoReceivesMessageMethod == null) return false;
        try { Object result = cargoReceivesMessageMethod.invoke(cargo, group); return result instanceof Boolean value && value; }
        catch (ReflectiveOperationException | LinkageError ex) { return false; }
    }

    private String joinMessage() {
        List<String> configured = plugin.getUtilidadesConfig().getStringList("mensagens-entrada.mensagens");
        if (configured.isEmpty()) return "";
        return configured.get(ThreadLocalRandom.current().nextInt(configured.size()));
    }

    private String cargoDisplayName(String group) {
        Plugin cargo = cargoPlugin();
        if (cargo == null || cargoDisplayNameMethod == null) return group;
        try { Object result = cargoDisplayNameMethod.invoke(cargo, group); return result instanceof String value && !value.isBlank() ? value : group; }
        catch (ReflectiveOperationException | LinkageError ex) { return group; }
    }

    private String cargoPrefix(Player player) {
        Plugin cargo = cargoPlugin();
        if (cargo == null || cargoPrefixMethod == null) return "";
        try {
            Object api = cargo.getClass().getMethod("api").invoke(cargo);
            if (api == null) return "";
            Method prefix = api.getClass().getMethod("getPrefix", UUID.class);
            Object result = prefix.invoke(api, player.getUniqueId());
            return result instanceof String value ? value : "";
        } catch (ReflectiveOperationException | LinkageError ex) { return ""; }
    }

    private Object cargoPermissions() {
        Plugin cargo = cargoPlugin();
        if (cargo == null || cargoPermissionsMethod == null) return null;
        try { return cargoPermissionsMethod.invoke(cargo); }
        catch (ReflectiveOperationException | LinkageError ex) { return null; }
    }

    private Plugin cargoPlugin() {
        Plugin current = Bukkit.getPluginManager().getPlugin("CargoPlus");
        if (current == null || !current.isEnabled()) return null;
        if (cargoPlugin != current || cargoPermissionsMethod == null || cargoGroupMethod == null || cargoDisplayNameMethod == null || cargoPrefixMethod == null || cargoReceivesMessageMethod == null) {
            synchronized (this) {
                if (cargoPlugin != current || cargoPermissionsMethod == null || cargoGroupMethod == null || cargoDisplayNameMethod == null || cargoPrefixMethod == null || cargoReceivesMessageMethod == null) {
                    try {
                        cargoPlugin = current;
                        cargoPermissionsMethod = current.getClass().getMethod("permissions");
                        Object permissions = cargoPermissionsMethod.invoke(current);
                        cargoGroupMethod = permissions.getClass().getMethod("getGroup", UUID.class);
                        cargoDisplayNameMethod = current.getClass().getMethod("getCargoDisplayName", String.class);
                        cargoPrefixMethod = current.getClass().getMethod("api");
                        cargoReceivesMessageMethod = current.getClass().getMethod("receivesJoinQuitMessage", String.class);
                    } catch (ReflectiveOperationException | LinkageError ex) {
                        cargoPermissionsMethod = null;
                        cargoGroupMethod = null;
                        cargoDisplayNameMethod = null;
                        cargoPrefixMethod = null;
                        cargoReceivesMessageMethod = null;
                        return null;
                    }
                }
            }
        }
        return cargoPlugin;
    }

    private String formatMessage(String message, Player player, String group) {
        String cargo = cargoPrefix(player);
        if (cargo.isBlank()) cargo = cargoDisplayName(group);
        return colorize(message
                .replace("%player%", player.getName())
                .replace("%cargo%", cargo));
    }

    private String colorize(String value) {
        String text = value == null ? "" : value;
        Matcher matcher = GRADIENT.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            int start = Integer.parseInt(matcher.group(1), 16);
            int end = Integer.parseInt(matcher.group(2), 16);
            String content = matcher.group(3);
            StringBuilder gradient = new StringBuilder();
            int length = content.codePointCount(0, content.length());
            if (length == 0) { matcher.appendReplacement(result, ""); continue; }
            int index = 0;
            for (int offset = 0; offset < content.length();) {
                int codePoint = content.codePointAt(offset);
                double ratio = length <= 1 ? 0 : (double) index / (length - 1);
                int r = (int) Math.round(((start >> 16) & 0xFF) + (((end >> 16) & 0xFF) - ((start >> 16) & 0xFF)) * ratio);
                int g = (int) Math.round(((start >> 8) & 0xFF) + (((end >> 8) & 0xFF) - ((start >> 8) & 0xFF)) * ratio);
                int b = (int) Math.round((start & 0xFF) + ((end & 0xFF) - (start & 0xFF)) * ratio);
                gradient.append(String.format("§x§%x§%x§%x§%x§%x§%x", (r >> 4) & 0xF, r & 0xF, (g >> 4) & 0xF, g & 0xF, (b >> 4) & 0xF, b & 0xF)).appendCodePoint(codePoint);
                offset += Character.charCount(codePoint);
                index++;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(gradient.toString()));
        }
        matcher.appendTail(result);
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }
}
