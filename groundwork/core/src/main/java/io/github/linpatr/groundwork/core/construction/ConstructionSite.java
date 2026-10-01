package io.github.linpatr.groundwork.core.construction;

import io.github.linpatr.groundwork.core.structure.GridPos;

/** The platform's view of the world where a building is being constructed. */
public interface ConstructionSite {
    SiteCondition inspect(GridPos pos);

    /**
     * Places every block of an already validated plan. Implementations choose the placement order
     * and update strategy, rotating block states by {@link ConstructionPlan#facing()}.
     */
    void build(ConstructionPlan plan);
}
