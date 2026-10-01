package io.github.linpatr.groundwork.core.structure;

/** An integer block position, either relative to a layout or absolute in a world. */
public record GridPos(int x, int y, int z) {
    public static final GridPos ORIGIN = new GridPos(0, 0, 0);

    public GridPos offset(int dx, int dy, int dz) {
        return new GridPos(x + dx, y + dy, z + dz);
    }

    public GridPos offset(GridPos other) {
        return offset(other.x, other.y, other.z);
    }

    public GridPos subtract(GridPos other) {
        return offset(-other.x, -other.y, -other.z);
    }
}
