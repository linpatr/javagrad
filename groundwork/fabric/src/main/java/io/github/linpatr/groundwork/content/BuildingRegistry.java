package io.github.linpatr.groundwork.content;

import io.github.linpatr.groundwork.core.Id;
import io.github.linpatr.groundwork.core.building.BuildingCatalog;
import io.github.linpatr.groundwork.core.building.BuildingDefinition;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Optional;

/**
 * The buildings currently loaded from data packs, with their block states already resolved.
 * Replaced wholesale on every data pack reload.
 */
public final class BuildingRegistry {
    private static volatile Snapshot snapshot = new Snapshot(BuildingCatalog.EMPTY, Map.of());

    private BuildingRegistry() {
    }

    public static BuildingCatalog catalog() {
        return snapshot.catalog();
    }

    /**
     * Looks a building up by identifier. A bare name such as {@code smelter} (which Minecraft
     * parses as {@code minecraft:smelter}) also matches a building with that path in any namespace.
     */
    public static Optional<BuildingDefinition> find(Id id) {
        BuildingCatalog catalog = snapshot.catalog();
        Optional<BuildingDefinition> exact = catalog.get(id);
        if (exact.isPresent() || !id.namespace().equals(Id.DEFAULT_NAMESPACE)) {
            return exact;
        }
        return catalog.all().stream().filter(definition -> definition.id().path().equals(id.path())).findFirst();
    }

    /** The resolved state for a block state string of a loaded building. */
    public static BlockState blockState(String blockState) {
        BlockState state = snapshot.blockStates().get(blockState);
        if (state == null) {
            throw new IllegalStateException("Block state was not resolved at load time: " + blockState);
        }
        return state;
    }

    static void replace(BuildingCatalog catalog, Map<String, BlockState> blockStates) {
        snapshot = new Snapshot(catalog, Map.copyOf(blockStates));
    }

    private record Snapshot(BuildingCatalog catalog, Map<String, BlockState> blockStates) {
    }
}
