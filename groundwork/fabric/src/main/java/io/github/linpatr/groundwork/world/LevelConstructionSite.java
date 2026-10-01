package io.github.linpatr.groundwork.world;

import io.github.linpatr.groundwork.content.BuildingRegistry;
import io.github.linpatr.groundwork.core.construction.ConstructionPlan;
import io.github.linpatr.groundwork.core.construction.ConstructionSite;
import io.github.linpatr.groundwork.core.construction.SiteCondition;
import io.github.linpatr.groundwork.core.structure.GridPos;
import io.github.linpatr.groundwork.core.structure.StructureLayout.PlacedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** A {@link ConstructionSite} backed by a server level. */
public final class LevelConstructionSite implements ConstructionSite {
    /** Send to clients but skip shape updates; shapes are settled in a second pass once every block exists. */
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private final ServerLevel level;

    public LevelConstructionSite(ServerLevel level) {
        this.level = level;
    }

    @Override
    public SiteCondition inspect(GridPos gridPos) {
        BlockPos pos = Conversions.toBlockPos(gridPos);
        if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return SiteCondition.OUT_OF_BOUNDS;
        }
        if (!level.isLoaded(pos)) {
            return SiteCondition.UNLOADED;
        }
        return level.getBlockState(pos).canBeReplaced() ? SiteCondition.CLEAR : SiteCondition.OBSTRUCTED;
    }

    @Override
    public void build(ConstructionPlan plan) {
        Rotation rotation = Conversions.toRotation(plan.facing());
        List<BlockPos> placed = new ArrayList<>(plan.blocks().size());

        // Layout blocks are ordered bottom-up, so supporting blocks exist before what rests on them.
        for (PlacedBlock block : plan.blocks()) {
            BlockPos pos = Conversions.toBlockPos(block.pos());
            BlockState state = BuildingRegistry.blockState(block.blockState()).rotate(rotation);
            level.setBlock(pos, state, PLACE_FLAGS);
            placed.add(pos);
        }

        // Let connecting blocks (walls, fences, panes...) join up with their new neighbours.
        for (BlockPos pos : placed) {
            BlockState current = level.getBlockState(pos);
            BlockState shaped = Block.updateFromNeighbourShapes(current, level, pos);
            if (shaped != current) {
                level.setBlock(pos, shaped, PLACE_FLAGS);
            }
        }

        // Finally notify neighbours so redstone, hoppers and the surrounding world react.
        for (BlockPos pos : placed) {
            level.blockUpdated(pos, level.getBlockState(pos).getBlock());
        }
    }
}
