package com.sistemautil.tab;

import com.sistemautil.SistemaUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controla a identidade visual, ordenação e informações do TAB.
 */
public final class ServerTabManager {
    private final SistemaUtil plugin;
    private int taskId = -1;

    public ServerTabManager(SistemaUtil plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        updateAll();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAll, 20L, 20L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void updateAll() {
        if (!plugin.getTabConfig().getBoolean("ativado", true)) return;

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        String address = plugin.getTabConfig().getString("endereco-servidor", "play.seuservidor.com:25565");
        String header = formatTabText(plugin.getTabConfig().getString("header", "&6&lMEU SERVIDOR\n&7Seja bem-vindo!"), online, max, 0, address);

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(buildComparator());

        for (int index = 0; index < players.size(); index++) {
            Player player = players.get(index);
            applyPlayer(player);
            player.setPlayerListOrder(index);
            int ping = Math.max(0, player.getPing());
            String footer = formatTabText(plugin.getTabConfig().getString("footer",
                    "&8&m----------------------------------------\n&fJogadores online: &a%online%/%max%\n&fSeu ping: &a%ping%ms\n&fIP: &b%ip%"),
                    online, max, ping, address);
            player.setPlayerListHeaderFooter(header, footer);
            player.updateCommands();
        }
    }

    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        applyPlayer(player);
        player.updateCommands();
    }

    private Comparator<Player> buildComparator() {
        List<Map<?, ?>> rules = plugin.getTabConfig().getMapList("sorting.rules");
        boolean enabled = plugin.getTabConfig().getBoolean("sorting.enabled", true);
        boolean defaultCaseSensitive = plugin.getTabConfig().getBoolean("sorting.case-sensitive", false);
        Comparator<Player> fallback = Comparator.comparing(Player::getName, stringComparator(defaultCaseSensitive));
        if (!enabled || rules.isEmpty()) return fallback;

        Comparator<Player> chain = null;
        for (Map<?, ?> raw : rules) {
            String type = string(raw.get("type")).toLowerCase(Locale.ROOT);
            boolean caseSensitive = raw.containsKey("case-sensitive")
                    ? Boolean.parseBoolean(String.valueOf(raw.get("case-sensitive")))
                    : defaultCaseSensitive;
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
        String group = cargoString(player, "getGroup");
        if (group.isBlank()) return Integer.MIN_VALUE;
        try {
            Plugin cargo = Bukkit.getPluginManager().getPlugin("CargoPlus");
            if (cargo == null || !cargo.isEnabled()) return Integer.MIN_VALUE;
            Method apiMethod = cargo.getClass().getMethod("api");
            Object api = apiMethod.invoke(cargo);
            Method groupsMethod = api.getClass().getMethod("groups");
            Object groups = groupsMethod.invoke(api);
            Method indexOf = groups.getClass().getMethod("indexOf", String.class);
            Object value = indexOf.invoke(groups, group);
            return value instanceof Integer i ? i : Integer.MIN_VALUE;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return Integer.MIN_VALUE;
        }
    }

    private boolean hasPermissionNode(Player player, String node) {
        return !node.isBlank() && player.hasPermission(node);
    }

    private double numericPlaceholder(Player player, String token) {
        String value = placeholder(player, token).trim();
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            return Double.POSITIVE_INFINITY;
        }
    }

    private int predefinedValueIndex(Player player, String token, Object values, boolean caseSensitive) {
        if (!(values instanceof List<?> list)) return Integer.MAX_VALUE;
        String value = placeholder(player, token);
        for (int i = 0; i < list.size(); i++) {
            String candidate = String.valueOf(list.get(i));
            if (caseSensitive ? candidate.equals(value) : candidate.equalsIgnoreCase(value)) return i;
        }
        return Integer.MAX_VALUE;
    }

    private String placeholder(Player player, String token) {
        if (token == null) return "";
        String value = token;
        value = value.replace("%player_name%", player.getName())
                .replace("%player_ping%", String.valueOf(Math.max(0, player.getPing())))
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%player_world%", player.getWorld().getName())
                .replace("%player_group%", cargoString(player, "getGroup"));

        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled()) {
            try {
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI", true, papi.getClass().getClassLoader());
                Method setPlaceholders = papiClass.getMethod("setPlaceholders", Player.class, String.class);
                Object result = setPlaceholders.invoke(null, player, value);
                if (result instanceof String resolved) value = resolved;
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        return value;
    }

    private String cargoString(Player player, String methodName) {
        try {
            Plugin cargo = Bukkit.getPluginManager().getPlugin("CargoPlus");
            if (cargo == null || !cargo.isEnabled()) return "";
            Method permissionsMethod = cargo.getClass().getMethod("permissions");
            Object permissions = permissionsMethod.invoke(cargo);
            Method method = permissions.getClass().getMethod(methodName, java.util.UUID.class);
            Object value = method.invoke(permissions, player.getUniqueId());
            return value instanceof String s ? s : "";
        } catch (ReflectiveOperationException | LinkageError ex) {
            return "";
        }
    }

    private Comparator<String> stringComparator(boolean caseSensitive) {
        return caseSensitive ? Comparator.naturalOrder() : String.CASE_INSENSITIVE_ORDER;
    }

    private String string(Object value) { return value == null ? "" : String.valueOf(value); }

    private void applyPlayer(Player player) {
        CargoData cargo = getCargoData(player);
        if (!cargo.available()) return;

        boolean tagEnabled = plugin.getTabConfig().getBoolean("tag.ativada", true);
        String prefix = tagEnabled && plugin.getTabConfig().getBoolean("tag.mostrar-no-tab", true)
                ? colorize(cargo.prefix()) : "";
        String nameColor = colorize(cargo.nicknameColor());
        player.setPlayerListName(prefix + nameColor + player.getName());

        if (tagEnabled && plugin.getTabConfig().getBoolean("tag.mostrar-na-cabeca", true)) {
            Team team = player.getScoreboard().getEntryTeam(player.getName());
            if (team != null) {
                team.setPrefix(prefix);
                team.setSuffix("");
            }
        }
    }

    private String formatTabText(String text, int online, int max, int ping, String address) {
        return colorize(text == null ? "" : text)
                .replace("%online%", String.valueOf(online))
                .replace("%max%", String.valueOf(max))
                .replace("%ping%", String.valueOf(ping))
                .replace("%ip%", address == null ? "" : address);
    }

    private CargoData getCargoData(Player player) {
        try {
            Plugin cargo = Bukkit.getPluginManager().getPlugin("CargoPlus");
            if (cargo == null || !cargo.isEnabled()) return CargoData.empty();

            Method permissionsMethod = cargo.getClass().getMethod("permissions");
            Object permissions = permissionsMethod.invoke(cargo);
            Method getPrefix = permissions.getClass().getMethod("getPrefix", java.util.UUID.class);
            Method getNicknameColor = permissions.getClass().getMethod("getNicknameColor", java.util.UUID.class);

            Object prefix = getPrefix.invoke(permissions, player.getUniqueId());
            Object nicknameColor = getNicknameColor.invoke(permissions, player.getUniqueId());

            return new CargoData(true,
                    prefix instanceof String ? (String) prefix : "",
                    nicknameColor instanceof String ? (String) nicknameColor : "&f");
        } catch (ReflectiveOperationException | LinkageError ex) {
            return CargoData.empty();
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private record CargoData(boolean available, String prefix, String nicknameColor) {
        static CargoData empty() { return new CargoData(false, "", "&f"); }
    }
}
