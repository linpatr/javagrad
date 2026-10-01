package io.github.linpatr.groundwork.core.structure;

import java.util.Objects;

/**
 * One block of a layout. {@code blockState} uses Minecraft's block state syntax, e.g.
 * {@code minecraft:blast_furnace[facing=south]}, authored for a north-facing building.
 */
public record LayoutBlock(GridPos local, String blockState) {
    public LayoutBlock {
        Objects.requireNonNull(local, "local");
        Objects.requireNonNull(blockState, "blockState");
    }
}
