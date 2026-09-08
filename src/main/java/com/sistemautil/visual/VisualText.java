package com.sistemautil.visual;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Centraliza small caps, RGB/gradientes e ícones do servidor. */
public final class VisualText {
    private static final Pattern GRADIENT = Pattern.compile("<gradient:([^>]+)>(.*?)</gradient>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HEX_TAG = Pattern.compile("<#([0-9a-fA-F]{6})>");
    private static final Pattern ICON = Pattern.compile("\\{icon:([a-zA-Z0-9_.-]+)}");

    private final FileConfiguration config;
    private final Map<String, String> icons = new HashMap<>();
    private String smallCapsUpper = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";
    private String smallCapsLower = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";
    private boolean enabled;
    private boolean smallCapsEnabled;
    private boolean gradientEnabled;
    private boolean iconsEnabled;

    public VisualText(FileConfiguration config) { this.config = config; reload(); }

    public void reload() {
        enabled = config.getBoolean("identidade-visual.ativado", true);
        smallCapsEnabled = config.getBoolean("identidade-visual.small-caps.ativado", true);
        gradientEnabled = config.getBoolean("identidade-visual.gradient.ativado", true);
        iconsEnabled = config.getBoolean("identidade-visual.icones.ativado", true);
        String upper = config.getString("identidade-visual.small-caps.mapa-maiusculas");
        String lower = config.getString("identidade-visual.small-caps.mapa-minusculas");
        if (upper != null && upper.length() >= 26) smallCapsUpper = upper.substring(0, 26);
        if (lower != null && lower.length() >= 26) smallCapsLower = lower.substring(0, 26);
        icons.clear();
        ConfigurationSection section = config.getConfigurationSection("identidade-visual.icones.lista");
        if (section != null) for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value != null) icons.put(key.toLowerCase(Locale.ROOT), value);
        }
    }

    public String format(String text) {
        if (text == null) return "";
        if (!enabled) return ChatColor.translateAlternateColorCodes('&', text);
        String result = replaceIcons(text);
        result = replaceGradients(result);
        result = replaceHexTags(result);
        result = smallCapsEnabled ? smallCaps(result) : result;
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    public String smallCaps(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder out = new StringBuilder(text.length());
        boolean tag = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<') tag = true;
            if (tag) {
                out.append(c);
                if (c == '>') tag = false;
                continue;
            }
            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(++i);
                out.append('§').append(code);
                if (code == 'x') {
                    for (int j = 0; j < 6; j++) {
                        if (i + 2 >= text.length() || text.charAt(i + 1) != '§') break;
                        out.append('§').append(text.charAt(i + 2));
                        i += 2;
                    }
                }
                continue;
            }
            if (c == '&' && i + 1 < text.length()) {
                out.append('&').append(text.charAt(++i));
                continue;
            }
            if (c >= 'A' && c <= 'Z') out.append(smallCapsUpper.charAt(c - 'A'));
            else if (c >= 'a' && c <= 'z') out.append(smallCapsLower.charAt(c - 'a'));
            else out.append(c);
        }
        return out.toString();
    }

    public String icon(String name) {
        if (!iconsEnabled || name == null) return "";
        return icons.getOrDefault(name.toLowerCase(Locale.ROOT), "");
    }

    public Map<String, String> icons() { return Map.copyOf(icons); }

    private String replaceIcons(String text) {
        Matcher matcher = ICON.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(result, Matcher.quoteReplacement(icon(matcher.group(1))));
        matcher.appendTail(result);
        return result.toString();
    }

    private String replaceHexTags(String text) {
        Matcher matcher = HEX_TAG.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(result, Matcher.quoteReplacement(hexCode(matcher.group(1))));
        matcher.appendTail(result);
        return result.toString();
    }

    private String replaceGradients(String text) {
        if (!gradientEnabled) return text;
        String result = text;
        for (int guard = 0; guard < 8; guard++) {
            Matcher matcher = GRADIENT.matcher(result);
            if (!matcher.find()) break;
            StringBuffer buffer = new StringBuffer();
            do {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(applyGradient(matcher.group(2), parseColors(matcher.group(1)))));
            } while (matcher.find());
            matcher.appendTail(buffer);
            result = buffer.toString();
        }
        return result;
    }

    private List<String> parseColors(String raw) {
        List<String> colors = new ArrayList<>();
        for (String token : raw.split(":")) {
            String value = token.trim();
            if (value.matches("#[0-9a-fA-F]{6}")) colors.add(value.substring(1));
            else if (value.matches("[0-9a-fA-F]{6}")) colors.add(value);
        }
        if (colors.isEmpty()) {
            colors.add(config.getString("identidade-visual.gradient.inicio", "7C3AED").replace("#", ""));
            colors.add(config.getString("identidade-visual.gradient.fim", "22D3EE").replace("#", ""));
        }
        return colors;
    }

    private String applyGradient(String text, List<String> colors) {
        if (text.isEmpty()) return text;
        int visible = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) { i++; continue; }
            if (c != '\n' && c != '\r') visible++;
        }
        if (visible == 0) return text;

        StringBuilder out = new StringBuilder(text.length() * 8);
        int index = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                out.append('&').append(text.charAt(++i));
                continue;
            }
            if (c == '\n' || c == '\r') { out.append(c); continue; }
            double progress = visible == 1 ? 0D : (double) index / (visible - 1);
            out.append(hexCode(interpolate(colors, progress))).append(c);
            index++;
        }
        return out.toString();
    }

    private String interpolate(List<String> colors, double progress) {
        if (colors.size() == 1) return colors.get(0);
        double scaled = progress * (colors.size() - 1);
        int index = Math.min(colors.size() - 2, (int) Math.floor(scaled));
        double local = scaled - index;
        int a = Integer.parseInt(colors.get(index), 16), b = Integer.parseInt(colors.get(index + 1), 16);
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) Math.round(ar + (br - ar) * local);
        int g = (int) Math.round(ag + (bg - ag) * local);
        int blue = (int) Math.round(ab + (bb - ab) * local);
        return String.format(Locale.ROOT, "%02X%02X%02X", r, g, blue);
    }

    private String hexCode(String hex) {
        String clean = hex.replace("#", "");
        if (!clean.matches("[0-9a-fA-F]{6}")) return "";
        StringBuilder out = new StringBuilder("§x");
        for (char c : clean.toCharArray()) out.append('§').append(c);
        return out.toString();
    }
}
