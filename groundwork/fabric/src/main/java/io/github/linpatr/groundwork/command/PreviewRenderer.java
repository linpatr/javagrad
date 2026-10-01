package io.github.linpatr.groundwork.command;

import io.github.linpatr.groundwork.core.construction.ConstructionPlan;
import io.github.linpatr.groundwork.core.structure.GridPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shows a building site to one player with particles: the bounding box outline, plus a red marker
 * on every position that blocks construction. Each player has at most one active preview.
 */
public final class PreviewRenderer {
    public static final int DURATION_TICKS = 200;
    private static final int INTERVAL_TICKS = 10;
    private static final double EDGE_STEP = 0.5;
    private static final ParticleOptions PROBLEM = new DustParticleOptions(new Vector3f(1.0f, 0.15f, 0.15f), 1.5f);

    private static final Map<UUID, Preview> ACTIVE = new HashMap<>();

    private PreviewRenderer() {
    }

    public static void show(ServerPlayer player, ConstructionPlan plan, List<GridPos> problems) {
        Preview preview = new Preview(plan.min(), plan.max(), List.copyOf(problems), DURATION_TICKS);
        ACTIVE.put(player.getUUID(), preview);
        render(player, preview);
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Preview>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Preview> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Preview preview = entry.getValue();
            preview.remainingTicks--;
            if (player == null || preview.remainingTicks <= 0) {
                iterator.remove();
            } else if (preview.remainingTicks % INTERVAL_TICKS == 0) {
                render(player, preview);
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static void render(ServerPlayer player, Preview preview) {
        double x0 = preview.min.x();
        double y0 = preview.min.y();
        double z0 = preview.min.z();
        double x1 = preview.max.x() + 1;
        double y1 = preview.max.y() + 1;
        double z1 = preview.max.z() + 1;
        for (double y : new double[] {y0, y1}) {
            edge(player, x0, y, z0, x1, y, z0);
            edge(player, x0, y, z1, x1, y, z1);
            edge(player, x0, y, z0, x0, y, z1);
            edge(player, x1, y, z0, x1, y, z1);
        }
        for (double x : new double[] {x0, x1}) {
            for (double z : new double[] {z0, z1}) {
                edge(player, x, y0, z, x, y1, z);
            }
        }
        for (GridPos pos : preview.problems) {
            player.serverLevel().sendParticles(player, PROBLEM, true,
                    pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, 3, 0.25, 0.25, 0.25, 0.0);
        }
    }

    private static void edge(ServerPlayer player, double xa, double ya, double za, double xb, double yb, double zb) {
        double length = Math.max(Math.abs(xb - xa), Math.max(Math.abs(yb - ya), Math.abs(zb - za)));
        int steps = Math.max(1, (int) Math.ceil(length / EDGE_STEP));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            player.serverLevel().sendParticles(player, ParticleTypes.END_ROD, true,
                    xa + (xb - xa) * t, ya + (yb - ya) * t, za + (zb - za) * t, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static final class Preview {
        final GridPos min;
        final GridPos max;
        final List<GridPos> problems;
        int remainingTicks;

        Preview(GridPos min, GridPos max, List<GridPos> problems, int remainingTicks) {
            this.min = min;
            this.max = max;
            this.problems = problems;
            this.remainingTicks = remainingTicks;
        }
    }
}
