package io.github.linpatr.groundwork.core.structure;

import java.util.Locale;
import java.util.Optional;

/**
 * The horizontal direction a building's "forward" axis points in the world.
 *
 * <p>Layouts are authored in local space: {@code +x} is the builder's right, {@code +y} is up and
 * {@code +z} is forward, away from the builder. Block states inside a layout are authored as if the
 * building faces {@link #NORTH}; platform adapters rotate them by {@link #quarterTurns()} clockwise
 * turns.
 */
public enum Facing {
    NORTH(0, -1, 1, 0),
    EAST(1, 0, 0, 1),
    SOUTH(0, 1, -1, 0),
    WEST(-1, 0, 0, -1);

    private final int forwardX;
    private final int forwardZ;
    private final int rightX;
    private final int rightZ;

    Facing(int forwardX, int forwardZ, int rightX, int rightZ) {
        this.forwardX = forwardX;
        this.forwardZ = forwardZ;
        this.rightX = rightX;
        this.rightZ = rightZ;
    }

    /** Converts a local layout offset into a world offset for a building facing this way. */
    public GridPos toWorld(GridPos local) {
        return new GridPos(
                local.x() * rightX + local.z() * forwardX,
                local.y(),
                local.x() * rightZ + local.z() * forwardZ);
    }

    /** Number of clockwise quarter turns (viewed from above) from {@link #NORTH} to this facing. */
    public int quarterTurns() {
        return ordinal();
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<Facing> byName(String name) {
        for (Facing facing : values()) {
            if (facing.serializedName().equalsIgnoreCase(name)) {
                return Optional.of(facing);
            }
        }
        return Optional.empty();
    }
}
