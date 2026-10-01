package io.github.linpatr.groundwork.core.building;

import io.github.linpatr.groundwork.core.Id;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** An immutable snapshot of every known building, sorted by identifier. */
public final class BuildingCatalog {
    public static final BuildingCatalog EMPTY = new BuildingCatalog(Map.of());

    private final Map<Id, BuildingDefinition> definitions;

    private BuildingCatalog(Map<Id, BuildingDefinition> definitions) {
        this.definitions = definitions;
    }

    /** @throws IllegalArgumentException if two definitions share an identifier */
    public static BuildingCatalog of(Collection<BuildingDefinition> definitions) {
        Map<Id, BuildingDefinition> sorted = new TreeMap<>();
        for (BuildingDefinition definition : definitions) {
            if (sorted.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalArgumentException("Duplicate building definition: " + definition.id());
            }
        }
        return new BuildingCatalog(Collections.unmodifiableMap(sorted));
    }

    public Optional<BuildingDefinition> get(Id id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public Collection<BuildingDefinition> all() {
        return definitions.values();
    }

    public int size() {
        return definitions.size();
    }
}
