package io.github.linpatr.groundwork.core.building;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.linpatr.groundwork.core.Id;
import io.github.linpatr.groundwork.core.material.BillOfMaterials;
import io.github.linpatr.groundwork.core.structure.GridPos;
import io.github.linpatr.groundwork.core.structure.LayoutBlock;
import io.github.linpatr.groundwork.core.structure.StructureLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads building definitions from JSON. The format is documented in {@code docs/building-format.md}.
 *
 * <pre>{@code
 * {
 *   "category": "production",
 *   "clearance": 0,
 *   "cost": { "minecraft:iron_ingot": 10 },
 *   "structure": {
 *     "key": { "#": "minecraft:stone_bricks", "F": "minecraft:blast_furnace[facing=south]" },
 *     "layers": [ ["#F#", "###"], ["###", "###"] ],
 *     "anchor": [1, 0, 0]
 *   }
 * }
 * }</pre>
 *
 * Layers go bottom to top; within a layer, rows go front (nearest the builder) to back and each
 * character is one block from left to right. A space leaves the world untouched.
 */
public final class BuildingDefinitionParser {
    /** Layout character that never places or checks a block. */
    public static final char SKIP = ' ';

    private BuildingDefinitionParser() {
    }

    public static BuildingDefinition parse(Id id, JsonElement json) {
        JsonObject root = object(json, "definition");
        String category = optionalString(root, "category").orElse("general");
        int clearance = root.has("clearance") ? integer(root.get("clearance"), "clearance") : 0;
        if (clearance < 0) {
            throw new DefinitionException("'clearance' cannot be negative");
        }
        BillOfMaterials cost = parseCost(object(required(root, "cost"), "cost"));
        StructureLayout layout = parseStructure(object(required(root, "structure"), "structure"));
        return new BuildingDefinition(id, category, clearance, cost, layout, optionalString(root, "name"));
    }

    private static BillOfMaterials parseCost(JsonObject cost) {
        if (cost.isEmpty()) {
            throw new DefinitionException("'cost' must list at least one material");
        }
        BillOfMaterials.Builder bill = BillOfMaterials.builder();
        for (Map.Entry<String, JsonElement> entry : cost.entrySet()) {
            Id material = id(entry.getKey(), "cost");
            int amount = integer(entry.getValue(), "cost." + entry.getKey());
            if (amount <= 0) {
                throw new DefinitionException("'cost." + entry.getKey() + "' must be positive");
            }
            bill.add(material, amount);
        }
        return bill.build();
    }

    private static StructureLayout parseStructure(JsonObject structure) {
        Map<Character, String> key = parseKey(object(required(structure, "key"), "structure.key"));
        JsonArray layers = array(required(structure, "layers"), "structure.layers");
        if (layers.isEmpty()) {
            throw new DefinitionException("'structure.layers' must not be empty");
        }

        List<LayoutBlock> blocks = new ArrayList<>();
        int width = -1;
        int depth = -1;
        for (int y = 0; y < layers.size(); y++) {
            String layerPath = "structure.layers[" + y + "]";
            JsonArray rows = array(layers.get(y), layerPath);
            if (depth < 0) {
                depth = rows.size();
            } else if (rows.size() != depth) {
                throw new DefinitionException(layerPath + " has " + rows.size() + " rows, expected " + depth);
            }
            for (int z = 0; z < rows.size(); z++) {
                String rowPath = layerPath + "[" + z + "]";
                String row = string(rows.get(z), rowPath);
                if (width < 0) {
                    width = row.length();
                } else if (row.length() != width) {
                    throw new DefinitionException(rowPath + " is " + row.length() + " wide, expected " + width);
                }
                for (int x = 0; x < row.length(); x++) {
                    char symbol = row.charAt(x);
                    if (symbol == SKIP) {
                        continue;
                    }
                    String state = key.get(symbol);
                    if (state == null) {
                        throw new DefinitionException(rowPath + " uses '" + symbol + "', which is not in 'structure.key'");
                    }
                    blocks.add(new LayoutBlock(new GridPos(x, y, z), state));
                }
            }
        }
        if (blocks.isEmpty()) {
            throw new DefinitionException("'structure.layers' contains no blocks");
        }
        if (width == 0 || depth == 0) {
            throw new DefinitionException("'structure.layers' rows must not be empty");
        }

        GridPos anchor = structure.has("anchor")
                ? parseAnchor(structure.get("anchor"))
                : new GridPos(width / 2, 0, 0);
        if (anchor.x() < 0 || anchor.x() >= width || anchor.y() < 0 || anchor.y() >= layers.size()
                || anchor.z() < 0 || anchor.z() >= depth) {
            throw new DefinitionException("'structure.anchor' " + anchor + " lies outside the layout");
        }
        return new StructureLayout(blocks, anchor);
    }

    private static Map<Character, String> parseKey(JsonObject keyJson) {
        Map<Character, String> key = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : keyJson.entrySet()) {
            String symbol = entry.getKey();
            if (symbol.length() != 1) {
                throw new DefinitionException("'structure.key' entries must be single characters, got '" + symbol + "'");
            }
            if (symbol.charAt(0) == SKIP) {
                throw new DefinitionException("'structure.key' cannot redefine ' ' (space always means skip)");
            }
            String state = string(entry.getValue(), "structure.key." + symbol).trim();
            int bracket = state.indexOf('[');
            id(bracket < 0 ? state : state.substring(0, bracket), "structure.key." + symbol);
            if (bracket >= 0 && !state.endsWith("]")) {
                throw new DefinitionException("'structure.key." + symbol + "' has an unterminated property list: " + state);
            }
            key.put(symbol.charAt(0), state);
        }
        return key;
    }

    private static GridPos parseAnchor(JsonElement json) {
        JsonArray anchor = array(json, "structure.anchor");
        if (anchor.size() != 3) {
            throw new DefinitionException("'structure.anchor' must be [x, y, z]");
        }
        return new GridPos(
                integer(anchor.get(0), "structure.anchor[0]"),
                integer(anchor.get(1), "structure.anchor[1]"),
                integer(anchor.get(2), "structure.anchor[2]"));
    }

    private static JsonElement required(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) {
            throw new DefinitionException("Missing required field '" + field + "'");
        }
        return value;
    }

    private static Optional<String> optionalString(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull() ? Optional.empty() : Optional.of(string(value, field));
    }

    private static JsonObject object(JsonElement json, String path) {
        if (!json.isJsonObject()) {
            throw new DefinitionException("'" + path + "' must be an object");
        }
        return json.getAsJsonObject();
    }

    private static JsonArray array(JsonElement json, String path) {
        if (!json.isJsonArray()) {
            throw new DefinitionException("'" + path + "' must be an array");
        }
        return json.getAsJsonArray();
    }

    private static String string(JsonElement json, String path) {
        if (!(json instanceof JsonPrimitive primitive) || !primitive.isString()) {
            throw new DefinitionException("'" + path + "' must be a string");
        }
        return primitive.getAsString();
    }

    private static int integer(JsonElement json, String path) {
        if (!(json instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
            throw new DefinitionException("'" + path + "' must be a whole number");
        }
        double value = primitive.getAsDouble();
        if (value != Math.rint(value) || value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new DefinitionException("'" + path + "' must be a whole number");
        }
        return (int) value;
    }

    private static Id id(String value, String path) {
        try {
            return Id.parse(value);
        } catch (IllegalArgumentException e) {
            throw new DefinitionException("'" + path + "' is not a valid identifier: " + value, e);
        }
    }
}
