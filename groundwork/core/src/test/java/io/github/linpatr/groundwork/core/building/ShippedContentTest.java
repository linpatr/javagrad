package io.github.linpatr.groundwork.core.building;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.linpatr.groundwork.core.Id;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates the building definitions and translations shipped in the mod's resources, using the
 * same parser the game uses, so content mistakes fail the build instead of being skipped at runtime.
 * (Checks that need the game registries, such as whether a block exists, happen when the game loads.)
 */
class ShippedContentTest {
    private static final String DIRECTORY = "groundwork/building";
    private static final String NAMESPACE = "groundwork";

    private static Path resources;
    private static List<BuildingDefinition> definitions;
    private static JsonObject english;

    @BeforeAll
    static void load() throws IOException {
        resources = Path.of(System.getProperty("groundwork.modResources", "../fabric/src/main/resources"));
        definitions = new ArrayList<>();
        Path data = resources.resolve("data");
        try (Stream<Path> namespaces = Files.list(data)) {
            for (Path namespace : namespaces.toList()) {
                Path directory = namespace.resolve(DIRECTORY);
                if (!Files.isDirectory(directory)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(directory)) {
                    for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                        String relative = directory.relativize(file).toString().replace('\\', '/');
                        Id id = Id.of(namespace.getFileName().toString(), relative.substring(0, relative.length() - 5));
                        try (Reader reader = Files.newBufferedReader(file)) {
                            definitions.add(BuildingDefinitionParser.parse(id, JsonParser.parseReader(reader)));
                        } catch (DefinitionException e) {
                            fail(file + ": " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
        try (Reader reader = Files.newBufferedReader(resources.resolve("assets/" + NAMESPACE + "/lang/en_us.json"))) {
            english = JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    void shipsBuildings() {
        assertFalse(definitions.isEmpty(), "no building definitions found under " + resources);
        BuildingCatalog.of(definitions);
    }

    @Test
    void everyBuildingIsTranslated() {
        for (BuildingDefinition definition : definitions) {
            assertTrue(english.has(definition.translationKey()), "missing translation " + definition.translationKey());
            assertTrue(english.has(definition.descriptionKey()), "missing translation " + definition.descriptionKey());
            assertTrue(english.has("category." + NAMESPACE + "." + definition.category()),
                    "missing translation for category " + definition.category());
        }
    }

    @Test
    void buildingsAreGrounded() {
        for (BuildingDefinition definition : definitions) {
            assertTrue(definition.layout().blocks().stream().anyMatch(block -> block.local().y() == 0),
                    definition.id() + " has no blocks in its bottom layer");
            assertTrue(definition.layout().anchor().y() == 0, definition.id() + " should be anchored at ground level");
        }
    }
}
