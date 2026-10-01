package io.github.linpatr.groundwork.core.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FacingTest {
    private static final GridPos RIGHT = new GridPos(1, 0, 0);
    private static final GridPos FORWARD = new GridPos(0, 0, 1);

    @Test
    void forwardPointsInTheFacingDirection() {
        // Minecraft: north = -Z, east = +X, south = +Z, west = -X.
        assertEquals(new GridPos(0, 0, -1), Facing.NORTH.toWorld(FORWARD));
        assertEquals(new GridPos(1, 0, 0), Facing.EAST.toWorld(FORWARD));
        assertEquals(new GridPos(0, 0, 1), Facing.SOUTH.toWorld(FORWARD));
        assertEquals(new GridPos(-1, 0, 0), Facing.WEST.toWorld(FORWARD));
    }

    @Test
    void rightIsAQuarterTurnClockwiseFromForward() {
        assertEquals(new GridPos(1, 0, 0), Facing.NORTH.toWorld(RIGHT));
        assertEquals(new GridPos(0, 0, 1), Facing.EAST.toWorld(RIGHT));
        assertEquals(new GridPos(-1, 0, 0), Facing.SOUTH.toWorld(RIGHT));
        assertEquals(new GridPos(0, 0, -1), Facing.WEST.toWorld(RIGHT));
    }

    @Test
    void heightIsUnaffected() {
        for (Facing facing : Facing.values()) {
            assertEquals(new GridPos(0, 5, 0), facing.toWorld(new GridPos(0, 5, 0)));
        }
    }

    @Test
    void quarterTurnsAreClockwiseFromNorth() {
        assertEquals(0, Facing.NORTH.quarterTurns());
        assertEquals(1, Facing.EAST.quarterTurns());
        assertEquals(2, Facing.SOUTH.quarterTurns());
        assertEquals(3, Facing.WEST.quarterTurns());
    }

    @Test
    void looksUpByName() {
        assertEquals(Facing.EAST, Facing.byName("East").orElseThrow());
        assertEquals(true, Facing.byName("up").isEmpty());
    }
}
