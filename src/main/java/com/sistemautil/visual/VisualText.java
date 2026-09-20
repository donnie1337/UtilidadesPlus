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
    private static final Pattern SMALL = Pattern.compile("<small>(.*?)</small>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EFFECT = Pattern.compile("<(carregamento|brilho|pulso|piscar|onda|onda-reversa|arcoiris|arcoiris-pulso|fade|shake)(?::(\\d+))?>(.*?)</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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
        return formatAnimated(text, 0L);
    }

    public String formatAnimated(String text, long tick) {
        if (text == null) return "";
        if (!enabled) return ChatColor.translateAlternateColorCodes('&', text);
        String result = replaceIcons(text);
        result = replaceGradients(result);
        result = replaceHexTags(result);
        result = smallCapsEnabled ? replaceSmallCapsTags(result) : result.replaceAll("(?i)</?small>", "");
        result = replaceEffects(result, tick);
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String replaceEffects(String text, long tick) {
        String result = text;
        for (int guard = 0; guard < 8; guard++) {
            Matcher matcher = EFFECT.matcher(result);
            if (!matcher.find()) break;
            StringBuffer buffer = new StringBuffer();
            do {
                String effect = matcher.group(1).toLowerCase(Locale.ROOT);
                int parameter = matcher.group(2) == null ? 3 : Math.max(1, Integer.parseInt(matcher.group(2)));
                String replacement = applyEffect(effect, parameter, matcher.group(3), tick);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            } while (matcher.find());
            matcher.appendTail(buffer);
            result = buffer.toString();
        }
        return result;
    }

    private String applyEffect(String effect, int parameter, String text, long tick) {
        if (text == null || text.isEmpty()) return text;
        List<StyledChar> chars = styledChars(text);
        if (chars.isEmpty()) return text;
        return switch (effect) {
            case "carregamento" -> loadingEffect(chars, parameter, tick);
            case "brilho" -> shineEffect(chars, tick);
            case "pulso" -> pulseEffect(chars, tick);
            case "piscar" -> blinkEffect(chars, parameter, tick);
            case "onda" -> waveEffect(chars, false, tick);
            case "onda-reversa" -> waveEffect(chars, true, tick);
            case "arcoiris" -> rainbowEffect(chars, false, tick);
            case "arcoiris-pulso" -> rainbowEffect(chars, true, tick);
            case "fade" -> fadeEffect(chars, tick);
            case "shake" -> shakeEffect(chars, tick);
            default -> text;
        };
    }

    private String loadingEffect(List<StyledChar> chars, int blinks, long tick) {
        int travelTicks = Math.max(12, chars.size() * 3);
        int blinkTicks = Math.max(2, blinks * 4);
        int pauseTicks = 12;
        int phase = (int) (tick % (travelTicks + blinkTicks + pauseTicks));
        if (phase >= travelTicks + blinkTicks) return render(chars, -1, null);
        if (phase < travelTicks) {
            int highlighted = Math.min(chars.size() - 1, (phase * chars.size()) / travelTicks);
            return render(chars, highlighted, WHITE);
        }
        int blinkPhase = phase - travelTicks;
        return (blinkPhase / 2) % 2 == 0 ? render(chars, -1, WHITE) : render(chars, -1, null);
    }

    private String shineEffect(List<StyledChar> chars, long tick) {
        int length = chars.size();
        int position = (int) (tick % Math.max(1, length * 2));
        if (position >= length) position = length * 2 - position - 1;
        return render(chars, position, WHITE);
    }

    private String pulseEffect(List<StyledChar> chars, long tick) {
        return render(chars, -1, ((tick / 6) % 2 == 0) ? WHITE : null);
    }

    private String blinkEffect(List<StyledChar> chars, int blinks, long tick) {
        int cycle = Math.max(8, blinks * 8 + 8);
        int phase = (int) (tick % cycle);
        if (phase >= blinks * 8) return render(chars, -1, null);
        return ((phase / 2) % 2 == 0) ? render(chars, -1, null) : "";
    }

    private String waveEffect(List<StyledChar> chars, boolean reverse, long tick) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < chars.size(); i++) {
            int index = reverse ? chars.size() - 1 - i : i;
            int distance = (int) ((tick - index * 2) % Math.max(1, chars.size() * 2));
            boolean highlighted = distance >= 0 && distance < 4;
            StyledChar ch = chars.get(i);
            out.append(ch.controls()).append(highlighted ? WHITE : "").append(ch.character());
            if (highlighted && ch.restoreColor() != null) out.append(ch.restoreColor());
        }
        return out.toString();
    }

    private String rainbowEffect(List<StyledChar> chars, boolean pulse, long tick) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < chars.size(); i++) {
            StyledChar ch = chars.get(i);
            double hue = ((i * 360.0 / Math.max(1, chars.size())) + tick * 6.0) % 360.0;
            if (pulse) hue = (hue + Math.sin(tick / 8.0) * 30.0 + 360.0) % 360.0;
            out.append(ch.controls()).append(hexCode(hsvToHex(hue, 1.0, 1.0))).append(ch.character());
            if (ch.restoreColor() != null) out.append(ch.restoreColor());
        }
        return out.toString();
    }

    private String fadeEffect(List<StyledChar> chars, long tick) {
        double value = (Math.sin(tick / 8.0) + 1.0) / 2.0;
        return value > 0.18 ? render(chars, -1, value > 0.72 ? WHITE : null) : "";
    }

    private String shakeEffect(List<StyledChar> chars, long tick) {
        return " ".repeat((int) (tick % 4)) + render(chars, -1, null);
    }

    private String render(List<StyledChar> chars, int highlighted, String color) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < chars.size(); i++) {
            StyledChar ch = chars.get(i);
            boolean highlight = highlighted == -1 ? color != null : i == highlighted;
            out.append(ch.controls());
            if (highlight && color != null) out.append(color);
            out.append(ch.character());
            if (highlight && ch.restoreColor() != null) out.append(ch.restoreColor());
        }
        return out.toString();
    }

    private List<StyledChar> styledChars(String text) {
        List<StyledChar> chars = new ArrayList<>();
        String controls = "";
        String activeColor = "";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(++i);
                if (code == 'x' && i + 12 < text.length()) {
                    StringBuilder rgb = new StringBuilder("§x");
                    for (int j = 0; j < 6 && i + 2 < text.length() && text.charAt(i + 1) == '§'; j++) {
                        rgb.append('§').append(text.charAt(i + 2));
                        i += 2;
                    }
                    controls += rgb;
                    activeColor = rgb.toString();
                } else {
                    controls += "§" + code;
                    if (isColorCode(code)) activeColor = "§" + code;
                }
                continue;
            }
            if (c == '&' && i + 1 < text.length()) {
                char code = text.charAt(++i);
                controls += "§" + code;
                if (isColorCode(code)) activeColor = "§" + code;
                continue;
            }
            if (c == '\n' || c == '\r') {
                controls += String.valueOf(c);
                continue;
            }
            chars.add(new StyledChar(controls, String.valueOf(c), activeColor.isBlank() ? null : activeColor));
            controls = "";
        }
        return chars;
    }

    private boolean isColorCode(char code) {
        return "0123456789abcdefABCDEF".indexOf(code) >= 0 || code == 'r' || code == 'R';
    }

    private String hsvToHex(double hue, double saturation, double value) {
        double c = value * saturation;
        double x = c * (1 - Math.abs((hue / 60.0) % 2 - 1));
        double m = value - c;
        double r, g, b;
        if (hue < 60) { r = c; g = x; b = 0; }
        else if (hue < 120) { r = x; g = c; b = 0; }
        else if (hue < 180) { r = 0; g = c; b = x; }
        else if (hue < 240) { r = 0; g = x; b = c; }
        else if (hue < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        int red = (int) Math.round((r + m) * 255);
        int green = (int) Math.round((g + m) * 255);
        int blue = (int) Math.round((b + m) * 255);
        return String.format(Locale.ROOT, "%02X%02X%02X", red, green, blue);
    }

    private record StyledChar(String controls, String character, String restoreColor) {}

    private static final String WHITE = "§f";

    private String replaceSmallCapsTags(String text) {
        Matcher matcher = SMALL.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(result, Matcher.quoteReplacement(smallCaps(matcher.group(1))));
        matcher.appendTail(result);
        return result.toString();
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
