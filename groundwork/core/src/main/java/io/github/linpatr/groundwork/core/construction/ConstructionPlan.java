package io.github.linpatr.groundwork.core.construction;

import io.github.linpatr.groundwork.core.building.BuildingDefinition;
import io.github.linpatr.groundwork.core.structure.Facing;
import io.github.linpatr.groundwork.core.structure.GridPos;
import io.github.linpatr.groundwork.core.structure.StructureLayout.PlacedBlock;

import java.util.List;

/** A building resolved to concrete world positions, ready to be checked or built. */
public record ConstructionPlan(BuildingDefinition building, GridPos origin, Facing facing, List<PlacedBlock> blocks) {
    public ConstructionPlan {
        blocks = List.copyOf(blocks);
    }

    public static ConstructionPlan of(BuildingDefinition building, GridPos origin, Facing facing) {
        return new ConstructionPlan(building, origin, facing, building.layout().place(origin, facing));
    }

    /** Smallest corner of the world-space bounding box. */
    public GridPos min() {
        int x = Integer.MAX_VALUE;
        int y = Integer.MAX_VALUE;
        int z = Integer.MAX_VALUE;
        for (PlacedBlock block : blocks) {
            x = Math.min(x, block.pos().x());
            y = Math.min(y, block.pos().y());
            z = Math.min(z, block.pos().z());
        }
        return new GridPos(x, y, z);
    }

    /** Largest corner of the world-space bounding box (inclusive). */
    public GridPos max() {
        int x = Integer.MIN_VALUE;
        int y = Integer.MIN_VALUE;
        int z = Integer.MIN_VALUE;
        for (PlacedBlock block : blocks) {
            x = Math.max(x, block.pos().x());
            y = Math.max(y, block.pos().y());
            z = Math.max(z, block.pos().z());
        }
        return new GridPos(x, y, z);
    }
}
