package io.github.linpatr.groundwork.core.construction;

import io.github.linpatr.groundwork.core.structure.GridPos;

import java.util.HashMap;
import java.util.Map;

/** An in-memory world for tests: positions are clear unless marked otherwise. */
final class FakeSite implements ConstructionSite {
    final Map<GridPos, SiteCondition> conditions = new HashMap<>();
    final Map<GridPos, String> placed = new HashMap<>();

    @Override
    public SiteCondition inspect(GridPos pos) {
        return conditions.getOrDefault(pos, SiteCondition.CLEAR);
    }

    @Override
    public void build(ConstructionPlan plan) {
        plan.blocks().forEach(block -> placed.put(block.pos(), block.blockState()));
    }
}
