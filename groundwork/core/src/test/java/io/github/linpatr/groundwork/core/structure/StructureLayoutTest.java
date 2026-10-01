package io.github.linpatr.groundwork.core.structure;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructureLayoutTest {
    @Test
    void computesSizeFromBlocks() {
        StructureLayout layout = new StructureLayout(List.of(
                new LayoutBlock(new GridPos(0, 0, 0), "a"),
                new LayoutBlock(new GridPos(2, 1, 3), "b")), GridPos.ORIGIN);
        assertEquals(new GridPos(3, 2, 4), layout.size());
    }

    @Test
    void placesRelativeToAnchorAndFacing() {
        StructureLayout layout = new StructureLayout(List.of(
                new LayoutBlock(new GridPos(0, 0, 0), "left"),
                new LayoutBlock(new GridPos(1, 0, 0), "anchor"),
                new LayoutBlock(new GridPos(1, 0, 1), "behind")), new GridPos(1, 0, 0));
        GridPos origin = new GridPos(10, 64, 10);

        List<StructureLayout.PlacedBlock> east = layout.place(origin, Facing.EAST);
        assertEquals(new GridPos(10, 64, 9), east.get(0).pos(), "left of an east-facing build is north");
        assertEquals(origin, east.get(1).pos(), "anchor lands on the origin");
        assertEquals(new GridPos(11, 64, 10), east.get(2).pos(), "forward of an east-facing build is +X");
    }

    @Test
    void rejectsEmptyLayouts() {
        assertThrows(IllegalArgumentException.class, () -> new StructureLayout(List.of(), GridPos.ORIGIN));
    }
}
