package io.github.linpatr.groundwork.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.linpatr.groundwork.GroundworkServer;
import io.github.linpatr.groundwork.content.BuildingRegistry;
import io.github.linpatr.groundwork.core.Id;
import io.github.linpatr.groundwork.core.building.BuildingCatalog;
import io.github.linpatr.groundwork.core.building.BuildingDefinition;
import io.github.linpatr.groundwork.core.construction.ConstructionResult;
import io.github.linpatr.groundwork.core.construction.ConstructionService;
import io.github.linpatr.groundwork.core.employee.EmployeeDirectory;
import io.github.linpatr.groundwork.core.employee.EmployeeRecord;
import io.github.linpatr.groundwork.core.material.MaterialSource;
import io.github.linpatr.groundwork.core.structure.Facing;
import io.github.linpatr.groundwork.core.structure.GridPos;
import io.github.linpatr.groundwork.world.Conversions;
import io.github.linpatr.groundwork.world.InventoryMaterials;
import io.github.linpatr.groundwork.world.LevelConstructionSite;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.github.linpatr.groundwork.command.Messages.tr;

/**
 * {@code /groundwork} (alias {@code /gw}): the player's construction terminal.
 *
 * <pre>
 * /gw                                       help
 * /gw onboard                               sign the employment contract
 * /gw status                                show your employee record
 * /gw catalog                               list buildings
 * /gw info &lt;building&gt;                       cost and dimensions
 * /gw preview &lt;building&gt; [pos] [facing]     outline the site and check it
 * /gw build &lt;building&gt; [pos] [facing]       construct
 * /gw admin clearance &lt;players&gt; &lt;level&gt;    (operators) set clearance
 * </pre>
 *
 * Without a position, buildings are anchored on the block face the player is looking at; without a
 * facing, they face the way the player is looking.
 */
public final class GroundworkCommand {
    private static final int TARGET_REACH = 32;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private static final DynamicCommandExceptionType UNKNOWN_BUILDING =
            new DynamicCommandExceptionType(id -> tr("error.unknown_building", id));
    private static final DynamicCommandExceptionType INVALID_FACING =
            new DynamicCommandExceptionType(value -> tr("error.facing", value));
    private static final DynamicCommandExceptionType NO_TARGET =
            new DynamicCommandExceptionType(reach -> tr("error.no_target", reach));

    private static final SuggestionProvider<CommandSourceStack> BUILDINGS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(
                    BuildingRegistry.catalog().all().stream().map(definition -> Conversions.toResourceLocation(definition.id())),
                    builder);
    private static final SuggestionProvider<CommandSourceStack> FACINGS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Facing.values()).map(Facing::serializedName), builder);

    private GroundworkCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(tree("groundwork"));
        dispatcher.register(tree("gw"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree(String name) {
        return Commands.literal(name)
                .executes(context -> help(context.getSource()))
                .then(Commands.literal("onboard").executes(context -> onboard(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("catalog").executes(context -> catalog(context.getSource())))
                .then(Commands.literal("info")
                        .then(Commands.argument("building", ResourceLocationArgument.id()).suggests(BUILDINGS)
                                .executes(context -> info(context.getSource(), building(context)))))
                .then(placement("preview", false))
                .then(placement("build", true))
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("clearance")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                .executes(context -> setClearance(context.getSource(),
                                                        EntityArgument.getPlayers(context, "players"),
                                                        IntegerArgumentType.getInteger(context, "level")))))));
    }

    /** {@code <literal> <building> [pos] [facing]}, shared by preview and build. */
    private static LiteralArgumentBuilder<CommandSourceStack> placement(String literal, boolean commit) {
        return Commands.literal(literal)
                .then(Commands.argument("building", ResourceLocationArgument.id()).suggests(BUILDINGS)
                        .executes(context -> place(context, null, null, commit))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> place(context, BlockPosArgument.getLoadedBlockPos(context, "pos"), null, commit))
                                .then(Commands.argument("facing", StringArgumentType.word()).suggests(FACINGS)
                                        .executes(context -> place(context,
                                                BlockPosArgument.getLoadedBlockPos(context, "pos"), facing(context), commit)))));
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Messages.heading(tr("help.header", Messages.company())), false);
        for (String line : List.of("onboard", "status", "catalog", "info", "preview", "build")) {
            source.sendSuccess(() -> tr("help." + line), false);
        }
        return 1;
    }

    private static int onboard(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmployeeDirectory employees = GroundworkServer.of(source.getServer()).employees();
        EmployeeRecord existing = employees.find(player.getUUID()).orElse(null);
        if (existing != null) {
            source.sendFailure(tr("onboard.already", employeeNumber(existing)));
            return 0;
        }
        EmployeeRecord record = employees.onboard(player.getUUID());
        source.sendSuccess(() -> Messages.heading(tr("onboard.header", Messages.company())), false);
        source.sendSuccess(() -> tr("onboard.line1", employeeNumber(record)), false);
        source.sendSuccess(() -> tr("onboard.line2"), false);
        source.sendSuccess(() -> tr("onboard.line3"), false);
        source.sendSuccess(() -> tr("onboard.line4").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmployeeRecord record = GroundworkServer.of(source.getServer()).employees().find(player.getUUID()).orElse(null);
        if (record == null) {
            source.sendFailure(tr("not_employed"));
            return 0;
        }
        source.sendSuccess(() -> tr("status", employeeNumber(record), record.clearance(),
                record.buildingsConstructed(), DATE.format(record.hiredAt())), false);
        return 1;
    }

    private static int catalog(CommandSourceStack source) {
        BuildingCatalog catalog = BuildingRegistry.catalog();
        if (catalog.size() == 0) {
            source.sendFailure(tr("catalog.empty"));
            return 0;
        }
        int clearance = clearanceOf(source);
        source.sendSuccess(() -> Messages.heading(tr("catalog.header", catalog.size())), false);
        for (BuildingDefinition building : catalog.all()) {
            MutableComponent name = Messages.name(building).withStyle(style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/gw build " + building.id()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            tr("catalog.hover", Messages.description(building)))));
            MutableComponent line = tr("catalog.entry", name, Messages.bill(building.cost()));
            if (building.requiredClearance() > clearance) {
                line.append(" ").append(tr("catalog.locked", building.requiredClearance()).withStyle(ChatFormatting.RED));
            }
            source.sendSuccess(() -> line, false);
        }
        return catalog.size();
    }

    private static int info(CommandSourceStack source, BuildingDefinition building) {
        GridPos size = building.layout().size();
        MaterialSource materials = source.getPlayer() != null ? InventoryMaterials.of(source.getPlayer()) : null;

        source.sendSuccess(() -> Messages.heading(tr("info.header", Messages.name(building), building.id().toString())), false);
        source.sendSuccess(() -> Messages.description(building).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> tr("info.details", size.x(), size.y(), size.z(),
                Messages.category(building.category()), building.requiredClearance()), false);
        source.sendSuccess(() -> tr("info.cost"), false);
        for (Map.Entry<Id, Integer> entry : building.cost().amounts().entrySet()) {
            int required = entry.getValue();
            String have = materials == null ? "-" : Integer.toString(Math.min(materials.count(entry.getKey()), required));
            ChatFormatting colour = materials == null || materials.count(entry.getKey()) >= required
                    ? ChatFormatting.GREEN : ChatFormatting.RED;
            source.sendSuccess(() -> tr("info.cost_line", have, required, Messages.material(entry.getKey())).withStyle(colour), false);
        }
        return 1;
    }

    private static int place(CommandContext<CommandSourceStack> context, BlockPos pos, Facing facing, boolean commit)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        BuildingDefinition building = building(context);
        GridPos origin = Conversions.toGridPos(pos != null ? pos : target(player));
        Facing direction = facing != null ? facing : Conversions.toFacing(player.getDirection());

        ConstructionService construction = GroundworkServer.of(source.getServer()).construction();
        MaterialSource materials = InventoryMaterials.of(player);
        LevelConstructionSite site = new LevelConstructionSite(source.getLevel());
        ConstructionResult result = commit
                ? construction.construct(player.getUUID(), building, origin, direction, materials, site)
                : construction.evaluate(player.getUUID(), building, origin, direction, materials, site);

        switch (result) {
            case ConstructionResult.Completed completed -> {
                source.sendSuccess(() -> tr("build.success", Messages.name(building),
                        completed.employee().buildingsConstructed()).withStyle(ChatFormatting.GREEN), false);
                return 1;
            }
            case ConstructionResult.Ready ready -> {
                PreviewRenderer.show(player, ready.plan(), List.of());
                source.sendSuccess(() -> tr("preview.ready", Messages.name(building)).withStyle(ChatFormatting.GREEN), false);
                return 1;
            }
            case ConstructionResult.SiteBlocked blocked -> {
                PreviewRenderer.show(player, blocked.plan(),
                        blocked.problems().stream().map(ConstructionResult.SiteBlocked.Problem::pos).toList());
                source.sendFailure(Messages.failure(result));
                return 0;
            }
            case ConstructionResult.MissingMaterials missing -> {
                if (!commit) {
                    PreviewRenderer.show(player, missing.plan(), List.of());
                }
                source.sendFailure(Messages.failure(result));
                return 0;
            }
            default -> {
                source.sendFailure(Messages.failure(result));
                return 0;
            }
        }
    }

    private static int setClearance(CommandSourceStack source, Collection<ServerPlayer> players, int level) {
        EmployeeDirectory employees = GroundworkServer.of(source.getServer()).employees();
        int updated = 0;
        for (ServerPlayer player : players) {
            if (!employees.isEmployed(player.getUUID())) {
                source.sendFailure(tr("admin.not_employed", player.getDisplayName()));
                continue;
            }
            employees.setClearance(player.getUUID(), level);
            source.sendSuccess(() -> tr("admin.clearance", player.getDisplayName(), level), true);
            updated++;
        }
        return updated;
    }

    private static BuildingDefinition building(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Id id = Conversions.toId(ResourceLocationArgument.getId(context, "building"));
        return BuildingRegistry.find(id).orElseThrow(() -> UNKNOWN_BUILDING.create(id.toString()));
    }

    private static Facing facing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String value = StringArgumentType.getString(context, "facing");
        return Facing.byName(value).orElseThrow(() -> INVALID_FACING.create(value));
    }

    /** The position in front of the block face the player is looking at. */
    private static BlockPos target(ServerPlayer player) throws CommandSyntaxException {
        HitResult hit = player.pick(TARGET_REACH, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            throw NO_TARGET.create(TARGET_REACH);
        }
        BlockHitResult blockHit = (BlockHitResult) hit;
        return blockHit.getBlockPos().relative(blockHit.getDirection());
    }

    private static int clearanceOf(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return Integer.MAX_VALUE;
        }
        return GroundworkServer.of(source.getServer()).employees().find(player.getUUID())
                .map(EmployeeRecord::clearance).orElse(0);
    }

    private static String employeeNumber(EmployeeRecord record) {
        return String.format("%04d", record.employeeNumber());
    }
}
