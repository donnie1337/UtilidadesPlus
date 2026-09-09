package com.sistemautil.tab;

import com.sistemautil.SistemaUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Controla a identidade visual, ordenação e informações do TAB. */
public final class ServerTabManager {
    private final SistemaUtil plugin;
    private final CargoBridge cargo = new CargoBridge();
    private final PlaceholderBridge placeholders = new PlaceholderBridge();
    private int taskId = -1;

    public ServerTabManager(SistemaUtil plugin) { this.plugin = plugin; }
    public void start() { stop(); updateAll(); taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAll, 20L, 20L); }
    public void stop() { if (taskId != -1) { Bukkit.getScheduler().cancelTask(taskId); taskId = -1; } }

    public void updateAll() {
        if (!plugin.getTabConfig().getBoolean("ativado", true)) return;
        cargo.refresh(); placeholders.refresh();
        int online = Bukkit.getOnlinePlayers().size(), max = Bukkit.getMaxPlayers();
        String address = plugin.getTabConfig().getString("endereco-servidor", "play.seuservidor.com:25565");
        String header = formatTabText(plugin.getTabConfig().getString("header", "&6&lMEU SERVIDOR\n&7Seja bem-vindo!"), online, max, 0, address, null);
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(buildComparator());
        for (int index = 0; index < players.size(); index++) {
            Player player = players.get(index);
            applyPlayer(player);
            // Neste servidor, menor prioridade numérica aparece primeiro no TAB.
            player.setPlayerListOrder(index + 1);
            int ping = Math.max(0, player.getPing());
            String footer = formatTabText(plugin.getTabConfig().getString("footer", "&8&m----------------------------------------\n&fJogadores online: &a%online%/%max%\n&fSeu ping: &a%ping%ms\n&fIP: &b%ip%"), online, max, ping, address, player);
            player.setPlayerListHeaderFooter(header, footer);
        }
    }

    public void updatePlayer(Player player) { if (player == null || !player.isOnline()) return; cargo.refresh(); placeholders.refresh(); applyPlayer(player); player.updateCommands(); }

    private Comparator<Player> buildComparator() {
        List<Map<?, ?>> rules = plugin.getTabConfig().getMapList("sorting.rules");
        boolean enabled = plugin.getTabConfig().getBoolean("sorting.enabled", true);
        boolean defaultCaseSensitive = plugin.getTabConfig().getBoolean("sorting.case-sensitive", false);
        Comparator<Player> fallback = Comparator.comparing(Player::getName, stringComparator(defaultCaseSensitive));
        if (!enabled || rules.isEmpty()) return fallback;
        Comparator<Player> chain = null;
        for (Map<?, ?> raw : rules) {
            String type = string(raw.get("type")).toLowerCase(Locale.ROOT);
            boolean caseSensitive = raw.containsKey("case-sensitive") ? Boolean.parseBoolean(String.valueOf(raw.get("case-sensitive"))) : defaultCaseSensitive;
            String order = string(raw.get("order")).toLowerCase(Locale.ROOT);
            Comparator<Player> rule = switch (type) {
                case "primary-group" -> Comparator.comparingInt(this::groupPriority);
                case "permission" -> Comparator.comparingInt(player -> hasPermissionNode(player, string(raw.get("node"))) ? 1 : 0);
                case "numeric-placeholder" -> Comparator.comparingDouble(player -> numericPlaceholder(player, string(raw.get("placeholder"))));
                case "placeholder" -> Comparator.comparing(player -> placeholder(player, string(raw.get("placeholder"))), stringComparator(caseSensitive));
                case "predefined-values" -> Comparator.comparingInt(player -> predefinedValueIndex(player, string(raw.get("placeholder")), raw.get("values"), caseSensitive));
                default -> null;
            };
            if (rule == null) continue;
            if ("descending".equals(order) || "desc".equals(order)) rule = rule.reversed();
            chain = chain == null ? rule : chain.thenComparing(rule);
        }
        return chain == null ? fallback : chain.thenComparing(Player::getName, stringComparator(defaultCaseSensitive));
    }

    private int groupPriority(Player player) {
        String group = cargo.getGroup(player).trim().toLowerCase(Locale.ROOT);
        return switch (group) {
            case "dev", "developer", "desenvolvedor" -> 0;
            case "gerente", "manager" -> 1;
            case "admin", "administrador", "administrator" -> 2;
            case "moderador", "moderator", "mod" -> 3;
            case "ajudante", "helper" -> 4;
            case "membro", "member", "default" -> 5;
            default -> 6;
        };
    }

    private boolean hasPermissionNode(Player player, String node) { return !node.isBlank() && player.hasPermission(node); }
    private double numericPlaceholder(Player player, String token) { try { return Double.parseDouble(placeholder(player, token).trim().replace(',', '.')); } catch (NumberFormatException ex) { return Double.POSITIVE_INFINITY; } }
    private int predefinedValueIndex(Player player, String token, Object values, boolean caseSensitive) {
        if (!(values instanceof List<?> list)) return Integer.MAX_VALUE;
        String value = placeholder(player, token);
        for (int i = 0; i < list.size(); i++) { String candidate = String.valueOf(list.get(i)); if (caseSensitive ? candidate.equals(value) : candidate.equalsIgnoreCase(value)) return i; }
        return Integer.MAX_VALUE;
    }
    private String placeholder(Player player, String token) {
        if (token == null) return "";
        return placeholders.resolve(player, token.replace("%player_name%", player.getName()).replace("%player_ping%", String.valueOf(Math.max(0, player.getPing()))).replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())).replace("%player_world%", player.getWorld().getName()).replace("%player_group%", cargo.getGroup(player)));
    }
    private Comparator<String> stringComparator(boolean caseSensitive) { return caseSensitive ? Comparator.naturalOrder() : String.CASE_INSENSITIVE_ORDER; }
    private String string(Object value) { return value == null ? "" : String.valueOf(value); }

    private void applyPlayer(Player player) {
        CargoData data = cargo.getData(player); if (!data.available()) return;
        boolean tagEnabled = plugin.getTabConfig().getBoolean("tag.ativada", true), showTag = tagEnabled && plugin.getTabConfig().getBoolean("tag.mostrar-no-tab", true);
        String prefix = showTag ? colorize(data.prefix()) : "", nameColor = colorize(data.nicknameColor());
        String format = plugin.getTabConfig().getString("jogadores.formato", "%prefix%%name_color%%player_name%");
        player.setPlayerListName(colorize(format.replace("%prefix%", prefix).replace("%name_color%", nameColor).replace("%player_name%", player.getName()).replace("%player_group%", cargo.getGroup(player))));
        if (tagEnabled && plugin.getTabConfig().getBoolean("tag.mostrar-na-cabeca", true)) { Team team = player.getScoreboard().getEntryTeam(player.getName()); if (team != null) { team.setPrefix(prefix); team.setSuffix(""); } }
    }
    private String formatTabText(String text, int online, int max, int ping, String address, Player player) {
        String result = (text == null ? "" : text).replace("%online%", String.valueOf(online)).replace("%max%", String.valueOf(max)).replace("%ping%", String.valueOf(ping)).replace("%ip%", address == null ? "" : address);
        if (player != null) result = placeholders.resolve(player, result.replace("%player_name%", player.getName()).replace("%player_group%", cargo.getGroup(player)).replace("%player_world%", player.getWorld().getName()));
        return colorize(result);
    }
    private String colorize(String text) { return plugin.getVisualText().format(text == null ? "" : text); }

    private static final class CargoBridge {
        private Plugin plugin; private Object api; private Object groups; private Method apiMethod, getGroupMethod, getPrefixMethod, getNicknameColorMethod, groupsMethod, indexOfMethod;
        void refresh() { Plugin current = Bukkit.getPluginManager().getPlugin("CargoPlus"); if (current == null || !current.isEnabled()) { clear(); return; } if (current == plugin && api != null) return; try { plugin = current; apiMethod = current.getClass().getMethod("api"); api = apiMethod.invoke(current); getGroupMethod = api.getClass().getMethod("getGroup", java.util.UUID.class); getPrefixMethod = api.getClass().getMethod("getPrefix", java.util.UUID.class); getNicknameColorMethod = api.getClass().getMethod("getNicknameColor", java.util.UUID.class); groupsMethod = api.getClass().getMethod("groups"); groups = groupsMethod.invoke(api); indexOfMethod = groups.getClass().getMethod("indexOf", String.class); } catch (ReflectiveOperationException | LinkageError ex) { clear(); } }
        private void clear() { plugin = null; api = null; groups = null; apiMethod = null; getGroupMethod = null; getPrefixMethod = null; getNicknameColorMethod = null; groupsMethod = null; indexOfMethod = null; }
        String getGroup(Player player) { return invokeString(getGroupMethod, player.getUniqueId()); }
        CargoData getData(Player player) { String prefix = invokeString(getPrefixMethod, player.getUniqueId()), color = invokeString(getNicknameColorMethod, player.getUniqueId()); return api == null ? CargoData.empty() : new CargoData(true, prefix, color.isBlank() ? "&f" : color); }
        private String invokeString(Method method, Object arg) { if (method == null || api == null) return ""; try { Object value = method.invoke(api, arg); return value == null ? "" : String.valueOf(value); } catch (ReflectiveOperationException | LinkageError ex) { return ""; } }
    }
    private static final class PlaceholderBridge {
        private Plugin plugin; private Method method;
        void refresh() { Plugin current = Bukkit.getPluginManager().getPlugin("PlaceholderAPI"); if (current == plugin) return; plugin = current; method = null; if (current != null && current.isEnabled()) try { Class<?> type = Class.forName("me.clip.placeholderapi.PlaceholderAPI", true, current.getClass().getClassLoader()); method = type.getMethod("setPlaceholders", Player.class, String.class); } catch (ReflectiveOperationException | LinkageError ignored) { method = null; } }
        String resolve(Player player, String value) { if (method == null || plugin == null) return value; try { Object result = method.invoke(null, player, value); return result instanceof String s ? s : value; } catch (ReflectiveOperationException | LinkageError ex) { return value; } }
    }
    private record CargoData(boolean available, String prefix, String nicknameColor) { static CargoData empty() { return new CargoData(false, "", "&f"); } }
}
