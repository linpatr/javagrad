package io.github.linpatr.groundwork.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.linpatr.groundwork.Groundwork;
import io.github.linpatr.groundwork.core.Id;
import io.github.linpatr.groundwork.core.building.BuildingCatalog;
import io.github.linpatr.groundwork.core.building.BuildingDefinition;
import io.github.linpatr.groundwork.core.building.BuildingDefinitionParser;
import io.github.linpatr.groundwork.core.building.DefinitionException;
import io.github.linpatr.groundwork.core.structure.LayoutBlock;
import io.github.linpatr.groundwork.world.Conversions;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads building definitions from {@code data/<namespace>/groundwork/building/<name>.json} in every
 * enabled data pack, so other mods and data packs can add buildings without code.
 *
 * <p>Definitions are validated against the game registries; an invalid definition is logged and
 * skipped rather than breaking the reload.
 */
public final class BuildingReloadListener implements SimpleSynchronousResourceReloadListener {
    public static final String DIRECTORY = "groundwork/building";
    private static final String EXTENSION = ".json";

    @Override
    public ResourceLocation getFabricId() {
        return Groundwork.id("buildings");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<BuildingDefinition> definitions = new ArrayList<>();
        Map<String, BlockState> blockStates = new HashMap<>();

        Map<ResourceLocation, Resource> resources =
                manager.listResources(DIRECTORY, location -> location.getPath().endsWith(EXTENSION));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation file = entry.getKey();
            String path = file.getPath().substring(DIRECTORY.length() + 1, file.getPath().length() - EXTENSION.length());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                BuildingDefinition definition = BuildingDefinitionParser.parse(Id.of(file.getNamespace(), path), json);
                validateCost(definition);
                resolveBlockStates(definition, blockStates);
                definitions.add(definition);
            } catch (IOException | RuntimeException e) {
                Groundwork.LOGGER.error("Skipping building definition {} from pack '{}': {}",
                        file, entry.getValue().sourcePackId(), e.getMessage());
            }
        }

        BuildingRegistry.replace(BuildingCatalog.of(definitions), blockStates);
        Groundwork.LOGGER.info("Loaded {} building definitions", definitions.size());
    }

    private static void validateCost(BuildingDefinition definition) {
        for (Id material : definition.cost().amounts().keySet()) {
            ResourceLocation item = Conversions.toResourceLocation(material);
            if (!BuiltInRegistries.ITEM.containsKey(item) || BuiltInRegistries.ITEM.get(item) == Items.AIR) {
                throw new DefinitionException("Unknown item in cost: " + material);
            }
        }
    }

    private static void resolveBlockStates(BuildingDefinition definition, Map<String, BlockState> resolved) {
        for (LayoutBlock block : definition.layout().blocks()) {
            String state = block.blockState();
            if (resolved.containsKey(state)) {
                continue;
            }
            try {
                resolved.put(state, BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), state, false).blockState());
            } catch (CommandSyntaxException e) {
                throw new DefinitionException("Invalid block state '" + state + "': " + e.getMessage(), e);
            }
        }
    }
}
