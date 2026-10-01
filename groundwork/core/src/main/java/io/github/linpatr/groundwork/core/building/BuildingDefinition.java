package io.github.linpatr.groundwork.core.building;

import io.github.linpatr.groundwork.core.Id;
import io.github.linpatr.groundwork.core.material.BillOfMaterials;
import io.github.linpatr.groundwork.core.structure.StructureLayout;

import java.util.Objects;
import java.util.Optional;

/**
 * Everything needed to construct a building: what it costs, what it looks like and who may build it.
 *
 * @param id                unique identifier, derived from the definition's data pack location
 * @param category          free-form grouping used by the catalog, e.g. {@code production}
 * @param requiredClearance minimum employee clearance level needed to construct it
 * @param cost              materials consumed on construction
 * @param layout            the blocks placed in the world
 * @param fallbackName      display name used when no translation exists, if the definition gave one
 */
public record BuildingDefinition(
        Id id,
        String category,
        int requiredClearance,
        BillOfMaterials cost,
        StructureLayout layout,
        Optional<String> fallbackName) {

    public BuildingDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(fallbackName, "fallbackName");
        if (requiredClearance < 0) {
            throw new IllegalArgumentException("Clearance cannot be negative: " + requiredClearance);
        }
    }

    /** Translation key for the building's display name, e.g. {@code building.groundwork.smelter}. */
    public String translationKey() {
        return "building." + id.namespace() + "." + id.path().replace('/', '.');
    }

    /** Translation key for the building's one-line description. */
    public String descriptionKey() {
        return translationKey() + ".desc";
    }
}
