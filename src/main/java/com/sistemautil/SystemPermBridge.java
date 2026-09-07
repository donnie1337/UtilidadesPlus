package com.sistemautil;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public final class SystemPermBridge {
    private final SistemaUtil plugin;
    private Plugin systemPerm;

    public SystemPermBridge(SistemaUtil plugin) {
        this.plugin = plugin;
        refresh();
    }

    public void refresh() {
        systemPerm = plugin.getServer().getPluginManager().getPlugin("CargoPlus");
        if (systemPerm == null) {
            systemPerm = plugin.getServer().getPluginManager().getPlugin("SistemaPerm");
        }
        if (systemPerm != null && !systemPerm.isEnabled()) systemPerm = null;
    }

    public boolean isAvailable() { return systemPerm != null && systemPerm.isEnabled(); }

    public boolean setChatColor(Player player, String color) {
        if (!isAvailable()) return false;
        try {
            Method method = systemPerm.getClass().getMethod("setChatColor", Player.class, String.class);
            Object result = method.invoke(systemPerm, player, color);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | LinkageError ex) {
            plugin.getLogger().warning("Não foi possível integrar /cor com o SistemaPerm: " + ex.getMessage());
            return false;
        }
    }

    public String getChatColorCode(Player player) {
        if (!isAvailable()) return null;
        try {
            Method method = systemPerm.getClass().getMethod("getChatColor", Player.class);
            Object result = method.invoke(systemPerm, player);
            return result instanceof String ? (String) result : null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return null;
        }
    }
}
