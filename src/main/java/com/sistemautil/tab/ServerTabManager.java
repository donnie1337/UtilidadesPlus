package com.sistemautil.tab;

import com.sistemautil.SistemaUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
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
    private final SistemaUtil plugin;
    private final CargoBridge cargo = new CargoBridge();
    private final ClanBridge clan = new ClanBridge();
    private final VanishBridge vanish = new VanishBridge();
    private final PlaceholderBridge placeholders = new PlaceholderBridge();
    private int taskId = -1;
    private long lastFooterFrame = Long.MIN_VALUE;
    private String lastFooterText = null;
    private final Map<UUID, String> headTeams = new HashMap<>();

    public ServerTabManager(SistemaUtil plugin) { this.plugin = plugin; }
    public void start() { stop(); updateAll(); taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAll, 2L, 2L); }
    public void stop() { if (taskId != -1) { Bukkit.getScheduler().cancelTask(taskId); taskId = -1; } clearHeadTeams(); }
    private void clearHeadTeams() { Scoreboard scoreboard = Bukkit.getScoreboardManager() == null ? null : Bukkit.getScoreboardManager().getMainScoreboard(); if (scoreboard == null) { headTeams.clear(); return; } for (String teamName : new ArrayList<>(headTeams.values())) { Team team = scoreboard.getTeam(teamName); if (team != null) team.unregister(); } headTeams.clear(); }

    private String formatFooter(int online, int max, int ping, String address, Player player) {
        String fallback = plugin.getTabConfig().getString("footer", "");
        if (!plugin.getTabConfig().getBoolean("footer-animado.ativado", false)) {
            return formatTabText(fallback, online, max, ping, address, player);
        }
        String efeito = plugin.getTabConfig().getString("footer-animado.efeito", "personalizado");
        if (!"personalizado".equalsIgnoreCase(efeito)) {
            String preset = buildFooterPreset(efeito, online, max, ping, address, player);
            if (preset != null) return preset;
        }
        List<Map<?, ?>> estados = plugin.getTabConfig().getMapList("footer-animado.estados");
        if (estados.isEmpty()) return formatTabText(fallback, online, max, ping, address, player);
        int intervalo = Math.max(1, plugin.getTabConfig().getInt("footer-animado.intervalo-segundos", 3));
        int frame = (int) ((System.currentTimeMillis() / 1000L / intervalo) % estados.size());
        Map<?, ?> estado = estados.get(frame);
        String riscos = stringValue(estado.get("riscos"), "");
        String mensagem = stringValue(estado.get("mensagem"), "");
        String espacos = stringValue(estado.get("espacos-centralizacao"), "                    ");
        return formatTabText(espacos + riscos + "\\n" + espacos + mensagem, online, max, ping, address, player);
    }

    private String buildFooterPreset(String efeito, int online, int max, int ping, String address, Player player) {
        int intervalo = Math.max(1, plugin.getTabConfig().getInt("footer-animado.intervalo-segundos", 3));
        long tick = System.currentTimeMillis() / 50L;
        long frame = tick / (intervalo * 20L);
        String espacos = plugin.getTabConfig().getString("footer-animado.predefinicoes." + efeito + ".espacos-centralizacao", "                    ");
        String barra = plugin.getTabConfig().getString("footer-animado.predefinicoes." + efeito + ".barra", "&c━━━━━━━━━━━━━━━━━━━━━━━━");
        String mensagem = plugin.getTabConfig().getString("footer-animado.predefinicoes." + efeito + ".mensagem", "&cServidor online!");
        if ("alternancia".equalsIgnoreCase(efeito)) {
            List<?> cores = plugin.getTabConfig().getList("footer-animado.predefinicoes.alternancia.cores");
            if (cores != null && !cores.isEmpty()) {
                String cor = String.valueOf(cores.get((int) (frame % cores.size())));
                barra = cor + "━━━━━━━━━━━━━━━━━━━━━━━━";
                mensagem = cor + plugin.getTabConfig().getString("footer-animado.predefinicoes.alternancia.texto", "Servidor online!");
            }
        } else if ("pulso".equalsIgnoreCase(efeito)) {
            String corA = plugin.getTabConfig().getString("footer-animado.predefinicoes.pulso.cor-inicial", "&c");
            String corB = plugin.getTabConfig().getString("footer-animado.predefinicoes.pulso.cor-final", "&f");
            boolean invertido = frame % 2 == 1;
            String cor = invertido ? corB : corA;
            barra = cor + "━━━━━━━━━━━━━━━━━━━━━━━━";
            mensagem = cor + plugin.getTabConfig().getString("footer-animado.predefinicoes.pulso.texto", "Servidor online!");
        } else if ("arco-iris".equalsIgnoreCase(efeito)) {
            List<?> cores = plugin.getTabConfig().getList("footer-animado.predefinicoes.arco-iris.cores");
            if (cores != null && !cores.isEmpty()) {
                String cor = String.valueOf(cores.get((int) (frame % cores.size())));
                barra = cor + "━━━━━━━━━━━━━━━━━━━━━━━━";
                mensagem = cor + plugin.getTabConfig().getString("footer-animado.predefinicoes.arco-iris.texto", "Servidor online!");
            }
        } else if ("deslizante".equalsIgnoreCase(efeito)) {
            String base = plugin.getTabConfig().getString("footer-animado.predefinicoes.deslizante.texto", "SEU SERVIDOR");
            String cor = plugin.getTabConfig().getString("footer-animado.predefinicoes.deslizante.cor", "&b");
            int largura = Math.max(1, plugin.getTabConfig().getInt("footer-animado.predefinicoes.deslizante.largura", 24));
            int pos = (int) (frame % (base.length() + largura));
            String texto = " ".repeat(Math.max(0, Math.min(pos, largura))) + base;
            barra = cor + "━".repeat(largura);
            mensagem = cor + texto;
        }
        return formatTabText(espacos + barra + "\\n" + espacos + mensagem, online, max, ping, address, player);
    }
    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    public void updateAll() {
        if (!plugin.getTabConfig().getBoolean("ativado", true)) return;
        cargo.refresh(); clan.refresh(); vanish.refresh(); placeholders.refresh();
        int online = Bukkit.getOnlinePlayers().size(); int max = Bukkit.getMaxPlayers();
        String address = plugin.getTabConfig().getString("endereco-servidor", "play.seuservidor.com:25565");
        String header = formatTabText(plugin.getTabConfig().getString("header", "&6&lMEU SERVIDOR\n&7Seja bem-vindo!"), online, max, 0, address, null);
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers()); Map<UUID, TabState> states = new HashMap<>(); 
        for (Player player : players) states.put(player.getUniqueId(), new TabState(cargo.getGroup(player), cargo.getPriority(cargo.getGroup(player))));
        players.sort(buildComparator(states));
        for (Player player : players) applyPlayer(player, states.get(player.getUniqueId()));
        updateVanishVisibility(players);
        for (int index = 0; index < players.size(); index++) {
            Player player = players.get(index); player.setPlayerListOrder(index); int ping = Math.max(0, player.getPing());
            String footer = formatFooter(online, max, ping, address, player);
            String configuredHeader = plugin.getTabConfig().getBoolean("header-ativado", true) ? header : "";
            String configuredFooter = plugin.getTabConfig().getBoolean("footer-ativado", true) ? footer : "";
            player.setPlayerListHeaderFooter(configuredHeader, configuredFooter);
        }
    }
    private void updateVanishVisibility(List<Player> players) {
        for (Player viewer : players) {
            if (viewer == null || !viewer.isOnline()) continue;
            boolean canSeeVanish = viewer.hasPermission("essentialsplus.vanish");
            for (Player target : players) {
                if (target == null || !target.isOnline() || viewer.equals(target)) continue;
                boolean targetVanished = vanish.isVanished(target);
                if (targetVanished && !canSeeVanish) {
                    viewer.hidePlayer(plugin, target);
                } else {
                    viewer.showPlayer(plugin, target);
                }
            }
        }
    }

    public void updatePlayer(Player player) { if (player == null || !player.isOnline()) return; updateAll(); player.updateCommands(); }

    private Comparator<Player> buildComparator(Map<UUID, TabState> states) {
        List<Map<?, ?>> rules = plugin.getTabConfig().getMapList("sorting.rules"); boolean defaultCaseSensitive = plugin.getTabConfig().getBoolean("sorting.case-sensitive", false);
        Comparator<Player> nameComparator = Comparator.comparing(Player::getName, stringComparator(defaultCaseSensitive));
        Comparator<Player> chain = Comparator.comparingInt(player -> { TabState state = states.get(player.getUniqueId()); return state == null ? Integer.MAX_VALUE : state.priority(); });
        if (plugin.getTabConfig().getBoolean("sorting.enabled", true)) for (Map<?, ?> raw : rules) {
            String type = string(raw.get("type")).toLowerCase(Locale.ROOT); if ("primary-group".equals(type)) continue;
            boolean caseSensitive = raw.containsKey("case-sensitive") ? Boolean.parseBoolean(String.valueOf(raw.get("case-sensitive"))) : defaultCaseSensitive; String order = string(raw.get("order")).toLowerCase(Locale.ROOT);
            Comparator<Player> rule = switch (type) {
                case "permission" -> Comparator.comparingInt(player -> hasPermissionNode(player, string(raw.get("node"))) ? 1 : 0);
                case "numeric-placeholder" -> Comparator.comparingDouble(player -> numericPlaceholder(player, string(raw.get("placeholder"))));
                case "placeholder" -> Comparator.comparing(player -> placeholder(player, string(raw.get("placeholder"))), stringComparator(caseSensitive));
                case "predefined-values" -> Comparator.comparingInt(player -> predefinedValueIndex(player, string(raw.get("placeholder")), raw.get("values"), caseSensitive));
                default -> null;
            };
            if (rule == null) continue; if ("descending".equals(order) || "desc".equals(order)) rule = rule.reversed(); chain = chain.thenComparing(rule);
        }
        return chain.thenComparing(nameComparator);
    }
    private boolean hasPermissionNode(Player player, String node) { return !node.isBlank() && player.hasPermission(node); }
    private double numericPlaceholder(Player player, String token) { try { return Double.parseDouble(placeholder(player, token).trim().replace(',', '.')); } catch (NumberFormatException ex) { return Double.POSITIVE_INFINITY; } }
    private int predefinedValueIndex(Player player, String token, Object values, boolean caseSensitive) { if (!(values instanceof List<?> list)) return Integer.MAX_VALUE; String value = placeholder(player, token); for (int i = 0; i < list.size(); i++) { String candidate = String.valueOf(list.get(i)); if (caseSensitive ? candidate.equals(value) : candidate.equalsIgnoreCase(value)) return i; } return Integer.MAX_VALUE; }
    private String placeholder(Player player, String token) { if (token == null) return ""; return placeholders.resolve(player, token.replace("%player_name%", player.getName()).replace("%player_ping%", String.valueOf(Math.max(0, player.getPing()))).replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())).replace("%player_world%", player.getWorld().getName()).replace("%player_group%", cargo.getGroup(player))); }
    private Comparator<String> stringComparator(boolean caseSensitive) { return caseSensitive ? Comparator.naturalOrder() : String.CASE_INSENSITIVE_ORDER; }
    private String string(Object value) { return value == null ? "" : String.valueOf(value); }

    private void applyPlayer(Player player, TabState state) {
        if (!plugin.getTabConfig().getBoolean("tag.ativada", true)) {
            player.setPlayerListName(formatConfiguredPlayerName(player, "", ""));
            return;
        }
        CargoData data = cargo.getData(player); if (!data.available()) return;
        String prefix = cargo.getAnimatedPrefix(player); if (prefix == null || prefix.isBlank()) prefix = data.prefix() == null ? "" : data.prefix();
        String cargoColor = data.nicknameColor();
        if (cargoColor == null || cargoColor.isBlank()) cargoColor = "§f";
        String clanTag = ensureClanColor(clan.getTag(player));
        // O nome recebe a cor do cargo, mas a tag do clan não deve herdar essa cor.
        // Quando o ClanPlus não fornece nenhuma cor, a tag usa cinza claro (&7).
        String tagPart = clanTag.isBlank() ? "" : " §r" + clanTag;
        String configured = plugin.getTabConfig().getString("jogadores.formato", "%prefix%%name_color%%player_name%%clan_tag%");
        String name = configured.replace("%prefix%", colorize(prefix))
                .replace("%name_color%", cargoColor)
                .replace("%player_name%", player.getName())
                .replace("%clan_tag%", tagPart)
                .replace("%group%", cargo.getGroup(player));
        if (!plugin.getTabConfig().getBoolean("tag.mostrar-no-tab", true)) name = name.replace(tagPart, "");
        player.setPlayerListName(colorize(name));
        applyAboveHead(player, prefix, cargoColor, player.getName(), clanTag, vanish.getSuffix(player));
    }

    private String ensureClanColor(String clanTag) {
        if (clanTag == null || clanTag.isBlank()) return "";
        if (hasExplicitClanColor(clanTag)) return clanTag;
        return "§7" + clanTag;
    }

    private boolean hasExplicitClanColor(String text) {
        if (text == null || text.isBlank()) return false;
        for (int i = 0; i + 1 < text.length(); i++) {
            char marker = text.charAt(i);
            char code = text.charAt(i + 1);
            if ((marker == '&' || marker == '§') && ((code >= '0' && code <= '9')
                    || (code >= 'a' && code <= 'f')
                    || (code >= 'A' && code <= 'F'))) {
                return true;
            }
            if ((marker == '&' || marker == '§') && (code == 'x' || code == 'X')) {
                return true;
            }
            if (marker == '<' && code == '#') {
                int end = text.indexOf('>', i + 2);
                if (end > i + 2) return true;
            }
        }
        return false;
    }

    private void applyAboveHead(Player player, String prefix, String nameColor, String playerName, String clanTag, String vanishSuffix) {
        if (!plugin.getTabConfig().getBoolean("tag.cabeca.ativada", true)) return;

        String configuredPrefix = plugin.getTabConfig().getString("tag.cabeca.prefixo.formato", "%prefix%");
        String configuredName = plugin.getTabConfig().getString("tag.cabeca.nome.formato", "%name_color%%player_name%");
        String configuredClan = plugin.getTabConfig().getString("tag.cabeca.clan.formato", " &r%clan_tag%");
        String configuredVanish = plugin.getTabConfig().getString("tag.cabeca.invisivel.formato", " &r%vanish_suffix%");

        String vanishText = plugin.getTabConfig().getString("tag.cabeca.invisivel.texto", "[INVISIVEL]");
        String vanishColor = plugin.getTabConfig().getString("tag.cabeca.invisivel.cor", "&c");
        boolean vanishEnabled = plugin.getTabConfig().getBoolean("tag.cabeca.invisivel.ativado", true);

        // Cada bloco visual mantém sua própria cor. O clan é encerrado antes da tag de invisibilidade.
        String prefixPart = replaceHeadPlaceholders(configuredPrefix, prefix, nameColor, playerName, clanTag, "");
        String namePart = replaceHeadPlaceholders(configuredName, prefix, nameColor, playerName, clanTag, "");
        String clanPart = clanTag == null || clanTag.isBlank()
                ? ""
                : replaceHeadPlaceholders(configuredClan, prefix, nameColor, playerName, clanTag, "");
        String invisPart = vanishEnabled && vanishSuffix != null && !vanishSuffix.isBlank()
                ? replaceHeadPlaceholders(configuredVanish, prefix, nameColor, playerName, clanTag, vanishColor + vanishText)
                : "";

        boolean vanishBelowName = plugin.getTabConfig().getBoolean("tag.cabeca.invisivel.abaixo-do-nome", true);
        if (vanishBelowName && !invisPart.isBlank()) {
            invisPart = "\n" + centerVanishUnderName(invisPart, namePart);
        }

        String format = plugin.getTabConfig().getString("tag.cabeca.formato", "%prefixo%%name_color%%player_name%%clan%%invisivel%");
        String result = format
                .replace("%prefixo%", prefixPart)
                .replace("%nome%", namePart)
                .replace("%name%", namePart)
                .replace("%clan%", clanPart)
                .replace("%invisivel%", invisPart)
                .replace("%prefix%", prefix == null ? "" : prefix)
                .replace("%name_color%", nameColor == null ? "§f" : nameColor)
                .replace("%player_name%", playerName == null ? player.getName() : playerName)
                .replace("%clan_tag%", clanTag == null ? "" : clanTag)
                .replace("%vanish_suffix%", vanishColor + vanishText)
                + (invisPart.isBlank() ? "" : "§r");

        ScoreboardManagerPlaceholder.apply(plugin, player, headTeams, prefixPart, nameColor, clanPart, invisPart, namePart);
    }

    private String centerVanishUnderName(String invisPart, String namePart) {
        boolean automatic = plugin.getTabConfig().getBoolean("tag.cabeca.invisivel.centralizar-automaticamente", true);
        int extraSpaces = Math.max(0, plugin.getTabConfig().getInt("tag.cabeca.invisivel.espacos-extra", 0));
        if (!automatic) {
            String configuredSpaces = plugin.getTabConfig().getString("tag.cabeca.invisivel.espacos-centralizacao", "       ");
            return configuredSpaces + invisPart;
        }

        String cleanName = plugin.getVisualText().format(namePart)
                .replaceAll("§[0-9A-FK-ORXx]", "")
                .replaceAll("<[^>]+>", "");
        String cleanInvis = plugin.getVisualText().format(invisPart)
                .replaceAll("§[0-9A-FK-ORXx]", "")
                .replaceAll("<[^>]+>", "");

        int padding = Math.max(0, (cleanName.length() - cleanInvis.length()) / 2) + extraSpaces;
        return " ".repeat(padding) + invisPart;
    }

    private String replaceHeadPlaceholders(String text, String prefix, String nameColor, String playerName, String clanTag, String vanishText) {
        return (text == null ? "" : text)
                .replace("%prefix%", prefix == null ? "" : prefix)
                .replace("%name_color%", nameColor == null ? "§f" : nameColor)
                .replace("%player_name%", playerName == null ? "" : playerName)
                .replace("%clan_tag%", clanTag == null ? "" : clanTag)
                .replace("%vanish_suffix%", vanishText == null ? "" : vanishText);
    }

    private static final class ScoreboardManagerPlaceholder {
        private static void apply(SistemaUtil plugin, Player player, Map<UUID, String> headTeams,
                                  String prefixPart, String nameColor, String clanPart, String invisPart, String namePart) {
            if (Bukkit.getScoreboardManager() == null) return;

            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = headTeams.computeIfAbsent(player.getUniqueId(),
                    uuid -> "util_" + uuid.toString().replace("-", "").substring(0, 11));

            Team team = scoreboard.getTeam(teamName);
            if (team == null) team = scoreboard.registerNewTeam(teamName);

            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }

            /*
             * A Team prefix is prepended to the real player-name entry.
             * The previous implementation put the complete nickname inside
             * the prefix, which made Minecraft render the nickname twice.
             *
             * Keep the real player name as the entry and put only the cargo
             * visual + nickname color in the prefix. This makes the nametag
             * use exactly the same CargoPlus prefix/effect and nickname color
             * as TAB without creating a second nickname.
             */
            String formattedPrefix = plugin.getVisualText().format(
                    (prefixPart == null ? "" : prefixPart)
                            + (nameColor == null || nameColor.isBlank() ? "§f" : nameColor)
            );

            String formattedSuffix = plugin.getVisualText().format(
                    clanPart == null ? "" : clanPart
            );

            // Do not rewrite the scoreboard packet every 2 ticks when nothing changed.
            if (!formattedPrefix.equals(team.getPrefix())) {
                team.setPrefix(formattedPrefix);
            }
            if (!formattedSuffix.equals(team.getSuffix())) {
                team.setSuffix(formattedSuffix);
            }
        }
    }

    private String formatConfiguredPlayerName(Player player, String prefix, String clanTag) {
        String configured = plugin.getTabConfig().getString("jogadores.formato", "%prefix%%name_color%%player_name%%clan_tag%");
        return colorize(configured.replace("%prefix%", prefix).replace("%name_color%", "§f").replace("%player_name%", player.getName()).replace("%clan_tag%", clanTag));
    }

    /* Small Caps is controlled by <small> tags in the configured TAB text. */
    private String toSmallCapsPreservingColors(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if ((character == '&' || character == '§') && i + 1 < text.length()) {
                char code = text.charAt(++i);
                result.append('§').append(code);
                continue;
            }

            result.append(toSmallCaps(character));
        }
        return result.toString();
    }

    private char toSmallCaps(char character) {
        return switch (character) {
            case 'A', 'a' -> 'ᴀ'; case 'B', 'b' -> 'ʙ'; case 'C', 'c' -> 'ᴄ'; case 'D', 'd' -> 'ᴅ';
            case 'E', 'e' -> 'ᴇ'; case 'F', 'f' -> 'ꜰ'; case 'G', 'g' -> 'ɢ'; case 'H', 'h' -> 'ʜ';
            case 'I', 'i' -> 'ɪ'; case 'J', 'j' -> 'ᴊ'; case 'K', 'k' -> 'ᴋ'; case 'L', 'l' -> 'ʟ';
            case 'M', 'm' -> 'ᴍ'; case 'N', 'n' -> 'ɴ'; case 'O', 'o' -> 'ᴏ'; case 'P', 'p' -> 'ᴘ';
            case 'Q', 'q' -> 'ǫ'; case 'R', 'r' -> 'ʀ'; case 'S', 's' -> 's'; case 'T', 't' -> 'ᴛ';
            case 'U', 'u' -> 'ᴜ'; case 'V', 'v' -> 'ᴠ'; case 'W', 'w' -> 'ᴡ'; case 'X', 'x' -> 'x';
            case 'Y', 'y' -> 'ʏ'; case 'Z', 'z' -> 'ᴢ'; default -> character;
        };
    }
    private String toSmallCaps(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder result = new StringBuilder(text.length());
        for (char character : text.toCharArray()) {
            result.append(switch (character) {
                case 'A', 'a' -> 'ᴀ'; case 'B', 'b' -> 'ʙ'; case 'C', 'c' -> 'ᴄ'; case 'D', 'd' -> 'ᴅ';
                case 'E', 'e' -> 'ᴇ'; case 'F', 'f' -> 'ꜰ'; case 'G', 'g' -> 'ɢ'; case 'H', 'h' -> 'ʜ';
                case 'I', 'i' -> 'ɪ'; case 'J', 'j' -> 'ᴊ'; case 'K', 'k' -> 'ᴋ'; case 'L', 'l' -> 'ʟ';
                case 'M', 'm' -> 'ᴍ'; case 'N', 'n' -> 'ɴ'; case 'O', 'o' -> 'ᴏ'; case 'P', 'p' -> 'ᴘ';
                case 'Q', 'q' -> 'ǫ'; case 'R', 'r' -> 'ʀ'; case 'S', 's' -> 's'; case 'T', 't' -> 'ᴛ';
                case 'U', 'u' -> 'ᴜ'; case 'V', 'v' -> 'ᴠ'; case 'W', 'w' -> 'ᴡ'; case 'X', 'x' -> 'x';
                case 'Y', 'y' -> 'ʏ'; case 'Z', 'z' -> 'ᴢ'; default -> character;
            });
        }
        return result.toString();
    }
    private String formatTabText(String text, int online, int max, int ping, String address, Player player) { String result = (text == null ? "" : text).replace("%online%", String.valueOf(online)).replace("%max%", String.valueOf(max)).replace("%ping%", String.valueOf(ping)).replace("%ip%", address == null ? "" : address); if (player != null) result = placeholders.resolve(player, result.replace("%player_name%", player.getName()).replace("%player_group%", cargo.getGroup(player)).replace("%player_world%", player.getWorld().getName())); return plugin.getVisualText().formatAnimated(result, System.currentTimeMillis() / 50L); }
    private String colorize(String text) { return plugin.getVisualText().format(text == null ? "" : text); }
    private record TabState(String group, int priority) { }

    private static final class CargoBridge {
        private Plugin plugin; private Object api; private Object groups; private Method apiMethod, getGroupMethod, getPrefixMethod, getAnimatedPrefixMethod, getNicknameColorMethod, groupsMethod, indexOfMethod;
        void refresh() { Plugin current = Bukkit.getPluginManager().getPlugin("CargoPlus"); if (current == null || !current.isEnabled()) { clear(); return; } try { plugin = current; apiMethod = current.getClass().getMethod("api"); Object currentApi = apiMethod.invoke(current); if (currentApi == null) { clear(); return; } api = currentApi; getGroupMethod = api.getClass().getMethod("getGroup", UUID.class); getPrefixMethod = api.getClass().getMethod("getPrefix", UUID.class); getAnimatedPrefixMethod = api.getClass().getMethod("getAnimatedPrefix", UUID.class); getNicknameColorMethod = api.getClass().getMethod("getNicknameColor", UUID.class); groupsMethod = api.getClass().getMethod("groups"); groups = groupsMethod.invoke(api); indexOfMethod = groups == null ? null : groups.getClass().getMethod("indexOf", String.class); } catch (ReflectiveOperationException | LinkageError ex) { clear(); } }
        private void clear() { plugin = null; api = null; groups = null; apiMethod = null; getGroupMethod = null; getPrefixMethod = null; getAnimatedPrefixMethod = null; getNicknameColorMethod = null; groupsMethod = null; indexOfMethod = null; }
        String getGroup(Player player) { return invokeString(getGroupMethod, player.getUniqueId()); }
        String getAnimatedPrefix(Player player) { return invokeString(getAnimatedPrefixMethod, player.getUniqueId()); }
        int getPriority(Player player) { return getPriority(getGroup(player)); }
        int getPriority(String rawGroup) { String group = normalizeGroup(rawGroup); return switch (group) { case "dev", "developer", "desenvolvedor" -> 0; case "gerente", "manager" -> 1; case "admin", "administrador", "administrator" -> 2; case "moderador", "moderator", "mod" -> 3; case "ajudante", "helper" -> 4; case "membro", "member", "default" -> 5; default -> priorityFromCargoHierarchy(group); }; }
        private int priorityFromCargoHierarchy(String group) { if (groups == null || indexOfMethod == null || group.isBlank()) return Integer.MAX_VALUE; try { Object value = indexOfMethod.invoke(groups, group); if (value instanceof Number number) return 100 - number.intValue(); } catch (ReflectiveOperationException | LinkageError ignored) {} return Integer.MAX_VALUE; }
        private String normalizeGroup(String group) { if (group == null) return ""; return group.replace('\u00A7', '&').replaceAll("(?i)&[0-9A-FK-ORX]", "").trim().toLowerCase(Locale.ROOT); }
        CargoData getData(Player player) { String prefix = invokeString(getPrefixMethod, player.getUniqueId()); String color = invokeString(getNicknameColorMethod, player.getUniqueId()); return api == null ? CargoData.empty() : new CargoData(true, prefix, color.isBlank() ? "&f" : color); }
        private String invokeString(Method method, Object arg) { if (method == null || api == null) return ""; try { Object value = method.invoke(api, arg); return value == null ? "" : String.valueOf(value); } catch (ReflectiveOperationException | LinkageError ex) { return ""; }
        }
    }

    private static final class ClanBridge {
        private Plugin plugin; private Object api; private Object manager; private Method apiMethod; private Method clansMethod; private Method byPlayerMethod; private Method tagMethod;
        void refresh() { Plugin current = Bukkit.getPluginManager().getPlugin("ClanPlus"); if (current == null || !current.isEnabled()) { clear(); return; } try { plugin = current; apiMethod = current.getClass().getMethod("clans"); manager = apiMethod.invoke(current); if (manager == null) { clear(); return; } clansMethod = apiMethod; byPlayerMethod = manager.getClass().getMethod("byPlayer", UUID.class); Object sampleClan = null; for (Object ignored : (Iterable<?>) manager.getClass().getMethod("all").invoke(manager)) { sampleClan = ignored; break; } if (sampleClan != null) tagMethod = sampleClan.getClass().getMethod("tag"); else tagMethod = null; } catch (ReflectiveOperationException | LinkageError ex) { clear(); } }
        String getTag(Player player) { if (manager == null || byPlayerMethod == null) return ""; try { Object clan = byPlayerMethod.invoke(manager, player.getUniqueId()); if (clan == null) return ""; Method method = tagMethod; if (method == null) method = clan.getClass().getMethod("tag"); Object value = method.invoke(clan); return value == null ? "" : String.valueOf(value); } catch (ReflectiveOperationException | LinkageError ex) { return ""; } }
        private void clear() { plugin = null; api = null; manager = null; apiMethod = null; clansMethod = null; byPlayerMethod = null; tagMethod = null; }
    }

    private static final class VanishBridge {
        private Plugin plugin;
        private Method method;

        void refresh() {
            Plugin current = Bukkit.getPluginManager().getPlugin("EssentialsPlus");
            if (current == plugin) return;
            plugin = current;
            method = null;
            if (current == null || !current.isEnabled()) return;
            try {
                method = current.getClass().getMethod("isVanished", Player.class);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                method = null;
            }
        }

        boolean isVanished(Player player) {
            if (method == null || plugin == null) return false;
            try {
                Object value = method.invoke(plugin, player);
                return Boolean.TRUE.equals(value);
            } catch (ReflectiveOperationException | LinkageError ex) {
                return false;
            }
        }

        String getSuffix(Player player) {
            return isVanished(player) ? "[INVISIVEL]" : "";
        }
    }

    private static final class PlaceholderBridge {
        private Plugin plugin; private Method method;
        void refresh() { Plugin current = Bukkit.getPluginManager().getPlugin("PlaceholderAPI"); if (current == plugin) return; plugin = current; method = null; if (current != null && current.isEnabled()) try { Class<?> type = Class.forName("me.clip.placeholderapi.PlaceholderAPI", true, current.getClass().getClassLoader()); method = type.getMethod("setPlaceholders", Player.class, String.class); } catch (ReflectiveOperationException | LinkageError ignored) { method = null; } }
        String resolve(Player player, String value) { if (method == null || plugin == null) return value; try { Object result = method.invoke(null, player, value); return result instanceof String s ? s : value; } catch (ReflectiveOperationException | LinkageError ex) { return value; } }
    }
    private record CargoData(boolean available, String prefix, String nicknameColor) { static CargoData empty() { return new CargoData(false, "", "&f"); } }
}
