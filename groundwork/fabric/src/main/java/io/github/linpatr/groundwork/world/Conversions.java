package io.github.linpatr.groundwork.world;

import io.github.linpatr.groundwork.core.Id;
import io.github.linpatr.groundwork.core.structure.Facing;
import io.github.linpatr.groundwork.core.structure.GridPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;

/** Converts between core value types and their Minecraft equivalents. */
public final class Conversions {
    private Conversions() {
    }

    public static BlockPos toBlockPos(GridPos pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    public static GridPos toGridPos(BlockPos pos) {
        return new GridPos(pos.getX(), pos.getY(), pos.getZ());
    }

    public static ResourceLocation toResourceLocation(Id id) {
        return ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path());
    }

    public static Id toId(ResourceLocation location) {
        return Id.of(location.getNamespace(), location.getPath());
    }

    /** @throws IllegalArgumentException for vertical directions */
    public static Facing toFacing(Direction direction) {
        return switch (direction) {
            case NORTH -> Facing.NORTH;
            case EAST -> Facing.EAST;
            case SOUTH -> Facing.SOUTH;
            case WEST -> Facing.WEST;
            default -> throw new IllegalArgumentException("Buildings can only face horizontally, got " + direction);
        };
    }

    /** The rotation that turns a block state authored for a north-facing building to {@code facing}. */
    public static Rotation toRotation(Facing facing) {
        return switch (facing) {
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
        };
    }
}
