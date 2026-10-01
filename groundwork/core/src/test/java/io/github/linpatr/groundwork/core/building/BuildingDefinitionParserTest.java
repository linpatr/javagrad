package io.github.linpatr.groundwork.core.building;

import com.google.gson.JsonParser;
import io.github.linpatr.groundwork.core.Id;
import io.github.linpatr.groundwork.core.structure.GridPos;
import io.github.linpatr.groundwork.core.structure.LayoutBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingDefinitionParserTest {
    private static final Id ID = Id.parse("groundwork:test");

    private static BuildingDefinition parse(String json) {
        return BuildingDefinitionParser.parse(ID, JsonParser.parseString(json));
    }

    private static DefinitionException parseError(String json) {
        return assertThrows(DefinitionException.class, () -> parse(json));
    }

    @Test
    void parsesAFullDefinition() {
        BuildingDefinition definition = parse("""
                {
                  "name": "Test Hut",
                  "category": "production",
                  "clearance": 2,
                  "cost": { "minecraft:iron_ingot": 3, "stone": 8 },
                  "structure": {
                    "key": { "#": "minecraft:stone", "F": "minecraft:furnace[facing=south]" },
                    "layers": [
                      ["#F#", "# #"],
                      ["###", "###"]
                    ],
                    "anchor": [1, 0, 0]
                  }
                }
                """);

        assertEquals(ID, definition.id());
        assertEquals("production", definition.category());
        assertEquals(2, definition.requiredClearance());
        assertEquals("Test Hut", definition.fallbackName().orElseThrow());
        assertEquals(8, definition.cost().amountOf(Id.parse("minecraft:stone")));
        assertEquals(new GridPos(3, 2, 2), definition.layout().size());
        assertEquals(new GridPos(1, 0, 0), definition.layout().anchor());
        assertEquals(11, definition.layout().blocks().size(), "the space is skipped");
        assertTrue(definition.layout().blocks().contains(
                new LayoutBlock(new GridPos(1, 0, 0), "minecraft:furnace[facing=south]")));
        assertEquals("building.groundwork.test", definition.translationKey());
    }

    @Test
    void appliesDefaults() {
        BuildingDefinition definition = parse("""
                { "cost": { "stone": 1 }, "structure": { "key": { "#": "stone" }, "layers": [["###"]] } }
                """);
        assertEquals("general", definition.category());
        assertEquals(0, definition.requiredClearance());
        assertTrue(definition.fallbackName().isEmpty());
        assertEquals(new GridPos(1, 0, 0), definition.layout().anchor(), "front-centre by default");
    }

    @Test
    void reportsMissingFields() {
        assertTrue(parseError("{ \"structure\": {} }").getMessage().contains("'cost'"));
        assertTrue(parseError("{ \"cost\": { \"stone\": 1 } }").getMessage().contains("'structure'"));
    }

    @Test
    void rejectsUnknownSymbols() {
        DefinitionException error = parseError("""
                { "cost": { "stone": 1 }, "structure": { "key": { "#": "stone" }, "layers": [["#X#"]] } }
                """);
        assertTrue(error.getMessage().contains("'X'"), error.getMessage());
    }

    @Test
    void rejectsRaggedLayers() {
        parseError("""
                { "cost": { "stone": 1 }, "structure": { "key": { "#": "stone" }, "layers": [["###", "##"]] } }
                """);
        parseError("""
                { "cost": { "stone": 1 }, "structure": { "key": { "#": "stone" }, "layers": [["#"], ["#", "#"]] } }
                """);
    }

    @Test
    void rejectsBadCosts() {
        parseError("""
                { "cost": {}, "structure": { "key": { "#": "stone" }, "layers": [["#"]] } }
                """);
        parseError("""
                { "cost": { "stone": 0 }, "structure": { "key": { "#": "stone" }, "layers": [["#"]] } }
                """);
        parseError("""
                { "cost": { "stone": 1.5 }, "structure": { "key": { "#": "stone" }, "layers": [["#"]] } }
                """);
        parseError("""
                { "cost": { "Bad Id": 1 }, "structure": { "key": { "#": "stone" }, "layers": [["#"]] } }
                """);
    }

    @Test
    void rejectsBadKeys() {
        parseError("""
                { "cost": { "stone": 1 }, "structure": { "key": { "##": "stone" }, "layers": [["#"]] } }
                """);
        parseError("""
                { "cost": { "stone": 1 }, "structure": { "key": { "#": "Stone" }, "layers": [["#"]] } }
                """);
        parseError("""
                { "cost": { "stone": 1 }, "structure": { "key": { "#": "furnace[facing=south" }, "layers": [["#"]] } }
                """);
    }

    @Test
    void rejectsAnchorOutsideLayout() {
        parseError("""
                { "cost": { "stone": 1 }, "structure": { "key": { "#": "stone" }, "layers": [["#"]], "anchor": [1, 0, 0] } }
                """);
    }

    @Test
    void rejectsLayoutsWithoutBlocks() {
        parseError("""
                { "cost": { "stone": 1 }, "structure": { "key": { "#": "stone" }, "layers": [["   "]] } }
                """);
    }

    @Test
    void catalogRejectsDuplicates() {
        BuildingDefinition definition = parse("""
                { "cost": { "stone": 1 }, "structure": { "key": { "#": "stone" }, "layers": [["#"]] } }
                """);
        assertThrows(IllegalArgumentException.class, () -> BuildingCatalog.of(List.of(definition, definition)));
    }
}
