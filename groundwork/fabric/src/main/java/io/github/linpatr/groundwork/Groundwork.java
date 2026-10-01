package io.github.linpatr.groundwork;

import io.github.linpatr.groundwork.command.GroundworkCommand;
import io.github.linpatr.groundwork.command.PreviewRenderer;
import io.github.linpatr.groundwork.content.BuildingReloadListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mod entry point: wires the platform-independent core into Minecraft through Fabric API events. */
public final class Groundwork implements ModInitializer {
    public static final String MOD_ID = "groundwork";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new BuildingReloadListener());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GroundworkCommand.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(PreviewRenderer::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PreviewRenderer.clear();
            GroundworkServer.release(server);
        });
    }
}
