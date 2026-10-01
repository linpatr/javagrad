package io.github.linpatr.groundwork.command;

import io.github.linpatr.groundwork.core.Id;
import io.github.linpatr.groundwork.core.building.BuildingDefinition;
import io.github.linpatr.groundwork.core.construction.ConstructionResult;
import io.github.linpatr.groundwork.core.construction.SiteCondition;
import io.github.linpatr.groundwork.core.material.BillOfMaterials;
import io.github.linpatr.groundwork.core.structure.GridPos;
import io.github.linpatr.groundwork.world.Conversions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;
import java.util.Map;

/** Builds the chat components shown by the commands. All player-facing text is translatable. */
final class Messages {
    private static final String PREFIX = "commands.groundwork.";

    private Messages() {
    }

    static MutableComponent tr(String key, Object... args) {
        return Component.translatable(PREFIX + key, args);
    }

    static MutableComponent company() {
        return Component.translatable("groundwork.company");
    }

    static MutableComponent name(BuildingDefinition building) {
        return Component.translatableWithFallback(building.translationKey(),
                building.fallbackName().orElse(building.id().toString()));
    }

    static MutableComponent description(BuildingDefinition building) {
        return Component.translatableWithFallback(building.descriptionKey(), "");
    }

    static MutableComponent category(String category) {
        return Component.translatableWithFallback("category.groundwork." + category, category);
    }

    static Component material(Id material) {
        return BuiltInRegistries.ITEM.get(Conversions.toResourceLocation(material)).getDescription();
    }

    /** e.g. "16× Smooth Stone, 4× Iron Ingot". */
    static MutableComponent bill(BillOfMaterials bill) {
        MutableComponent text = Component.empty();
        boolean first = true;
        for (Map.Entry<Id, Integer> entry : bill.amounts().entrySet()) {
            if (!first) {
                text.append(", ");
            }
            text.append(tr("cost.entry", entry.getValue(), material(entry.getKey())));
            first = false;
        }
        return text;
    }

    static Component position(GridPos pos) {
        return Component.translatable("chat.coordinates", pos.x(), pos.y(), pos.z());
    }

    /** Explains why a construction could not go ahead. Not valid for successful results. */
    static Component failure(ConstructionResult result) {
        return switch (result) {
            case ConstructionResult.NotEmployed ignored -> tr("not_employed");
            case ConstructionResult.InsufficientClearance clearance ->
                    tr("result.clearance", clearance.required(), clearance.actual());
            case ConstructionResult.SiteBlocked blocked -> {
                ConstructionResult.SiteBlocked.Problem first = blocked.problems().getFirst();
                yield tr("result.blocked", blocked.problems().size(), position(first.pos()), condition(first.condition()));
            }
            case ConstructionResult.MissingMaterials missing -> tr("result.missing", bill(missing.shortfall()));
            case ConstructionResult.Ready ignored -> throw new IllegalArgumentException("Not a failure: " + result);
            case ConstructionResult.Completed ignored -> throw new IllegalArgumentException("Not a failure: " + result);
        };
    }

    private static Component condition(SiteCondition condition) {
        return tr("result.condition." + condition.name().toLowerCase(Locale.ROOT));
    }

    static MutableComponent heading(Component text) {
        return text.copy().withStyle(ChatFormatting.GOLD);
    }
}
