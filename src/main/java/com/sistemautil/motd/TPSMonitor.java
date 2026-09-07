package com.sistemautil.motd;

import org.bukkit.scheduler.BukkitRunnable;
import com.sistemautil.SistemaUtil;

public final class TPSMonitor {
    private BukkitRunnable tarefa;
    private long ultimoTick = System.nanoTime();
    private double tps = 20.0;

    public void start(SistemaUtil plugin) {
        stop();
        ultimoTick = System.nanoTime();
        tarefa = new BukkitRunnable() {
            @Override
            public void run() {
                long agora = System.nanoTime();
                long decorrido = agora - ultimoTick;
                ultimoTick = agora;
                if (decorrido <= 0) return;
                double instantaneo = Math.min(20.0, Math.max(0.0, 1_000_000_000.0 / decorrido));
                tps = tps * 0.95 + instantaneo * 0.05;
            }
        };
        tarefa.runTaskTimer(plugin, 1L, 1L);
    }

    public void stop() {
        if (tarefa != null) {
            tarefa.cancel();
            tarefa = null;
        }
    }

    public double getTPS() { return Math.round(tps * 100.0) / 100.0; }
}
