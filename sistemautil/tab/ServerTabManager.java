package com.sistemautil.tab;

import com.sistemautil.SistemaUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Controla a identidade visual, ordenação e informações do TAB. */
public final class ServerTabManager {
    private static final long FALLBACK_REFRESH_TICKS = 100L;

    private final SistemaUtil plugin;
    private final CargoBridge cargo = new CargoBridge();
    private final PlaceholderBridge placeholders = new PlaceholderBridge();
    private int taskId = -1;

    public ServerTabManager(SistemaUtil plugin) { this.plugin = plugin; }

    public void start() {
        stop();
        updateAll();
        // O TAB é atualizado por eventos quando possível. Este refresh periódico é apenas
        // um fallback para alterações feitas por plugins externos ou APIs que não emitam evento.
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAll,
                FALLBACK_REFRESH_TICKS, FALLBACK_REFRESH_TICKS);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void updateAll() {
        if (!plugin.getTabConfig().getBoolean("ativado", true)) return;

        cargo.refresh();
        placeholders.refresh();

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        String address = plugin.getTabConfig().getString("endereco-servidor", "play.seuservidor.com:25565");
        String header = formatTabText(
                plugin.getTabConfig().getString("header", "&6&lMEU SERVIDOR\n&7Seja bem-vindo!"),
                online, max, 0, address, null
        );

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        // Capture o cargo e a prioridade uma única vez por ciclo.
        // Assim, a ordenação e o número enviado ao Bukkit usam exatamente o mesmo snapshot.
        Map<UUID, TabState> states = new HashMap<>();
        for (Player player : players) {
            String group = cargo.getGroup(player);
            int priority = cargo.getPriority(group);
            states.put(player.getUniqueId(), new TabState(group, priority));
        }

        players.sort(buildComparator(states));

        // Primeiro atualiza a identidade visual de todos.
        for (Player player : players) {
            applyPlayer(player, states.get(player.getUniqueId()));
        }

        // Depois aplica a ordem em uma segunda passada. Isso evita que a atualização
        // visual interfira no momento em que a ordem da lista é enviada ao Bukkit.
        for (int index = 0; index < players.size(); index++) {
            Player player = players.get(index);
            player.setPlayerListOrder(index);

            int ping = Math.max(0, player.getPing());
            String footer = formatTabText(
                    plugin.getTabConfig().getString("footer", "&8&m----------------------------------------\n&fJogadores online: &a%online%/%max%\n&fSeu ping: &a%ping%ms\n&fIP: &b%ip%"),
                    online, max, ping, address, player
            );
            player.setPlayerListHeaderFooter(header, footer);
        }
    }

    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        updateAll();
        player.updateCommands();
    }

    private Comparator<Player> buildComparator(Map<UUID, TabState> states) {
        List<Map<?, ?>> rules = plugin.getTabConfig().getMapList("sorting.rules");
        boolean defaultCaseSensitive = plugin.getTabConfig().getBoolean("sorting.case-sensitive", false);
        Comparator<Player> nameComparator = Comparator.comparing(Player::getName, stringComparator(defaultCaseSensitive));
        Comparator<Player> chain = Comparator.comparingInt(player -> {
            TabState state = states.get(player.getUniqueId());
            return state == null ? Integer.MAX_VALUE : state.priority();
        });

        boolean enabled = plugin.getTabConfig().getBoolean("sorting.enabled", true);
        if (enabled) {
            for (Map<?, ?> raw : rules) {
                String type = string(raw.get("type")).toLowerCase(Locale.ROOT);
                // A hierarquia de cargos é obrigatória e sempre vem primeiro.
                if ("primary-group".equals(type)) continue;

                boolean caseSensitive = raw.containsKey("case-sensitive")
                        ? Boolean.parseBoolean(String.valueOf(raw.get("case-sensitive")))
                        : defaultCaseSensitive;
                String order = string(raw.get("order")).toLowerCase(Locale.ROOT);

                Comparator<Player> rule = switch (type) {
                    case "permission" -> Comparator.comparingInt(player -> hasPermissionNode(player, string(raw.get("node"))) ? 1 : 0);
                    case "numeric-placeholder" -> Comparator.comparingDouble(player -> numericPlaceholder(player, string(raw.get("placeholder"))));
                    case "placeholder" -> Comparator.comparing(player -> placeholder(player, string(raw.get("placeholder"))), stringComparator(caseSensitive));
                    case "predefined-values" -> Comparator.comparingInt(player -> predefinedValueIndex(player, string(raw.get("placeholder")), raw.get("values"), caseSensitive));
                    default -> null;
                };

                if (rule == null) continue;
                if ("descending".equals(order) || "desc".equals(order)) rule = rule.reversed();
                chain = chain.thenComparing(rule);
            }
        }

        return chain.thenComparing(nameComparator);
    }

    private boolean hasPermissionNode(Player player, String node) {
        return !node.isBlank() && player.hasPermission(node);
    }

    private double numericPlaceholder(Player player, String token) {
        try {
            return Double.parseDouble(placeholder(player, token).trim().replace(',', '.'));
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
        return placeholders.resolve(player, token
                .replace("%player_name%", player.getName())
                .replace("%player_ping%", String.valueOf(Math.max(0, player.getPing())))
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%player_world%", player.getWorld().getName())
                .replace("%player_group%", cargo.getGroup(player)));
    }

    private Comparator<String> stringComparator(boolean caseSensitive) {
        return caseSensitive ? Comparator.naturalOrder() : String.CASE_INSENSITIVE_ORDER;
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void applyPlayer(Player player, TabState state) {
        CargoData data = cargo.getData(player);
        if (!data.available()) return;

        boolean tagEnabled = plugin.getTabConfig().getBoolean("tag.ativada", true);
        boolean showTag = tagEnabled && plugin.getTabConfig().getBoolean("tag.mostrar-no-tab", true);
        String prefix = showTag ? colorize(data.prefix()) : "";
        String nameColor = colorize(data.nicknameColor());
        String group = state == null ? cargo.getGroup(player) : state.group();
        String format = plugin.getTabConfig().getString("jogadores.formato", "%prefix%%name_color%%player_name%");

        player.setPlayerListName(colorize(format
                .replace("%prefix%", prefix)
                .replace("%name_color%", nameColor)
                .replace("%player_name%", player.getName())
                .replace("%player_group%", group)));

        if (tagEnabled && plugin.getTabConfig().getBoolean("tag.mostrar-na-cabeca", true)) {
            Team team = player.getScoreboard().getEntryTeam(player.getName());
            if (team != null) {
                team.setPrefix(prefix);
                team.setSuffix("");
            }
        }
    }

    private String formatTabText(String text, int online, int max, int ping, String address, Player player) {
        String result = (text == null ? "" : text)
                .replace("%online%", String.valueOf(online))
                .replace("%max%", String.valueOf(max))
                .replace("%ping%", String.valueOf(ping))
                .replace("%ip%", address == null ? "" : address);

        if (player != null) {
            result = placeholders.resolve(player, result
                    .replace("%player_name%", player.getName())
                    .replace("%player_group%", cargo.getGroup(player))
                    .replace("%player_world%", player.getWorld().getName()));
        }

        return colorize(result);
    }

    private String colorize(String text) {
        return plugin.getVisualText().format(text == null ? "" : text);
    }

    private record TabState(String group, int priority) { }

    private static final class CargoBridge {
        private Plugin plugin;
        private Object api;
        private Object groups;
        private Method apiMethod, getGroupMethod, getPrefixMethod, getNicknameColorMethod, groupsMethod, indexOfMethod;

        void refresh() {
            Plugin current = Bukkit.getPluginManager().getPlugin("CargoPlus");
            if (current == null || !current.isEnabled()) {
                clear();
                return;
            }

            try {
                plugin = current;
                apiMethod = current.getClass().getMethod("api");
                Object currentApi = apiMethod.invoke(current);
                if (currentApi == null) {
                    clear();
                    return;
                }

                api = currentApi;
                getGroupMethod = api.getClass().getMethod("getGroup", UUID.class);
                getPrefixMethod = api.getClass().getMethod("getPrefix", UUID.class);
                getNicknameColorMethod = api.getClass().getMethod("getNicknameColor", UUID.class);
                groupsMethod = api.getClass().getMethod("groups");
                groups = groupsMethod.invoke(api);
                indexOfMethod = groups == null ? null : groups.getClass().getMethod("indexOf", String.class);
            } catch (ReflectiveOperationException | LinkageError ex) {
                clear();
            }
        }

        private void clear() {
            plugin = null;
            api = null;
            groups = null;
            apiMethod = null;
            getGroupMethod = null;
            getPrefixMethod = null;
            getNicknameColorMethod = null;
            groupsMethod = null;
            indexOfMethod = null;
        }

        String getGroup(Player player) {
            return invokeString(getGroupMethod, player.getUniqueId());
        }

        int getPriority(Player player) {
            return getPriority(getGroup(player));
        }

        int getPriority(String rawGroup) {
            String group = normalizeGroup(rawGroup);
            return switch (group) {
                case "dev", "developer", "desenvolvedor" -> 0;
                case "gerente", "manager" -> 1;
                case "admin", "administrador", "administrator" -> 2;
                case "moderador", "moderator", "mod" -> 3;
                case "ajudante", "helper" -> 4;
                case "membro", "member", "default" -> 5;
                default -> priorityFromCargoHierarchy(group);
            };
        }

        private int priorityFromCargoHierarchy(String group) {
            if (groups == null || indexOfMethod == null || group.isBlank()) return Integer.MAX_VALUE;

            try {
                Object value = indexOfMethod.invoke(groups, group);
                if (value instanceof Number number) {
                    // CargoPlus mantém a hierarquia do menor para o maior cargo.
                    // Invertendo o índice, o maior cargo recebe a menor prioridade TAB.
                    return 100 - number.intValue();
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }

            return Integer.MAX_VALUE;
        }

        private String normalizeGroup(String group) {
            if (group == null) return "";
            return group.replace('\u00A7', '&')
                    .replaceAll("(?i)&[0-9A-FK-ORX]", "")
                    .trim()
                    .toLowerCase(Locale.ROOT);
        }

        CargoData getData(Player player) {
            String prefix = invokeString(getPrefixMethod, player.getUniqueId());
            String color = invokeString(getNicknameColorMethod, player.getUniqueId());
            return api == null
                    ? CargoData.empty()
                    : new CargoData(true, prefix, color.isBlank() ? "&f" : color);
        }

        private String invokeString(Method method, Object arg) {
            if (method == null || api == null) return "";
            try {
                Object value = method.invoke(api, arg);
                return value == null ? "" : String.valueOf(value);
            } catch (ReflectiveOperationException | LinkageError ex) {
                return "";
            }
        }
    }

    private static final class PlaceholderBridge {
        private Plugin plugin;
        private Method method;

        void refresh() {
            Plugin current = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
            if (current == plugin) return;
            plugin = current;
            method = null;

            if (current != null && current.isEnabled()) {
                try {
                    Class<?> type = Class.forName(
                            "me.clip.placeholderapi.PlaceholderAPI",
                            true,
                            current.getClass().getClassLoader()
                    );
                    method = type.getMethod("setPlaceholders", Player.class, String.class);
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    method = null;
                }
            }
        }

        String resolve(Player player, String value) {
            if (method == null || plugin == null) return value;
            try {
                Object result = method.invoke(null, player, value);
                return result instanceof String s ? s : value;
            } catch (ReflectiveOperationException | LinkageError ex) {
                return value;
            }
        }
    }

    private record CargoData(boolean available, String prefix, String nicknameColor) {
        static CargoData empty() {
            return new CargoData(false, "", "&f");
        }
    }
}
