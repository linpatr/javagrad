package io.github.linpatr.groundwork.core.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The blocks that make up a building, in local coordinates (see {@link Facing}), together with the
 * anchor: the local position that lands on the target block when the building is placed.
 */
public final class StructureLayout {
    private final List<LayoutBlock> blocks;
    private final GridPos size;
    private final GridPos anchor;

    public StructureLayout(List<LayoutBlock> blocks, GridPos anchor) {
        Objects.requireNonNull(anchor, "anchor");
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("A layout needs at least one block");
        }
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        for (LayoutBlock block : blocks) {
            GridPos p = block.local();
            if (p.x() < 0 || p.y() < 0 || p.z() < 0) {
                throw new IllegalArgumentException("Layout positions must be non-negative, got " + p);
            }
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
            maxZ = Math.max(maxZ, p.z());
        }
        this.blocks = List.copyOf(blocks);
        this.size = new GridPos(maxX + 1, maxY + 1, maxZ + 1);
        this.anchor = anchor;
    }

    public List<LayoutBlock> blocks() {
        return blocks;
    }

    /** Width ({@code x}), height ({@code y}) and depth ({@code z}) of the layout's bounding box. */
    public GridPos size() {
        return size;
    }

    public GridPos anchor() {
        return anchor;
    }

    /** Resolves every block to its world position for a building anchored at {@code origin}. */
    public List<PlacedBlock> place(GridPos origin, Facing facing) {
        List<PlacedBlock> placed = new ArrayList<>(blocks.size());
        for (LayoutBlock block : blocks) {
            GridPos world = origin.offset(facing.toWorld(block.local().subtract(anchor)));
            placed.add(new PlacedBlock(world, block.blockState()));
        }
        return placed;
    }

    /** A layout block resolved to an absolute world position. */
    public record PlacedBlock(GridPos pos, String blockState) {
    }
}
