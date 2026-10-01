package io.github.linpatr.groundwork.core.construction;

import io.github.linpatr.groundwork.core.building.BuildingDefinition;
import io.github.linpatr.groundwork.core.construction.ConstructionResult.SiteBlocked.Problem;
import io.github.linpatr.groundwork.core.employee.EmployeeDirectory;
import io.github.linpatr.groundwork.core.employee.EmployeeRecord;
import io.github.linpatr.groundwork.core.material.BillOfMaterials;
import io.github.linpatr.groundwork.core.material.MaterialSource;
import io.github.linpatr.groundwork.core.structure.Facing;
import io.github.linpatr.groundwork.core.structure.GridPos;
import io.github.linpatr.groundwork.core.structure.StructureLayout.PlacedBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The single entry point for constructing buildings. Every way of building something (commands
 * today, tools or blueprints later) goes through here so the rules are applied consistently:
 *
 * <ol>
 *   <li>the player must be an employee,</li>
 *   <li>with at least the building's required clearance,</li>
 *   <li>every position of the footprint must be buildable,</li>
 *   <li>and the material source must cover the full cost.</li>
 * </ol>
 *
 * Materials are only consumed once every check has passed, so a failed construction never costs
 * anything.
 */
public final class ConstructionService {
    private final EmployeeDirectory employees;
    private final List<ConstructionListener> listeners = new CopyOnWriteArrayList<>();

    public ConstructionService(EmployeeDirectory employees) {
        this.employees = Objects.requireNonNull(employees, "employees");
    }

    public void addListener(ConstructionListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Runs every check without changing anything. Returns {@link ConstructionResult.Ready} on success. */
    public ConstructionResult evaluate(UUID employee, BuildingDefinition building, GridPos origin, Facing facing,
                                       MaterialSource materials, ConstructionSite site) {
        EmployeeRecord record = employees.find(employee).orElse(null);
        if (record == null) {
            return new ConstructionResult.NotEmployed();
        }
        if (record.clearance() < building.requiredClearance()) {
            return new ConstructionResult.InsufficientClearance(building.requiredClearance(), record.clearance());
        }

        ConstructionPlan plan = ConstructionPlan.of(building, origin, facing);
        List<Problem> problems = new ArrayList<>();
        for (PlacedBlock block : plan.blocks()) {
            SiteCondition condition = site.inspect(block.pos());
            if (condition != SiteCondition.CLEAR) {
                problems.add(new Problem(block.pos(), condition));
            }
        }
        if (!problems.isEmpty()) {
            return new ConstructionResult.SiteBlocked(plan, problems);
        }

        BillOfMaterials shortfall = building.cost().shortfall(materials);
        if (!shortfall.isEmpty()) {
            return new ConstructionResult.MissingMaterials(plan, shortfall);
        }
        return new ConstructionResult.Ready(plan);
    }

    /**
     * Evaluates and, if every check passes, consumes the cost, builds the structure and updates the
     * employee's record. Returns {@link ConstructionResult.Completed} on success.
     */
    public ConstructionResult construct(UUID employee, BuildingDefinition building, GridPos origin, Facing facing,
                                        MaterialSource materials, ConstructionSite site) {
        ConstructionResult evaluation = evaluate(employee, building, origin, facing, materials, site);
        if (!(evaluation instanceof ConstructionResult.Ready ready)) {
            return evaluation;
        }
        ConstructionPlan plan = ready.plan();
        building.cost().amounts().forEach(materials::remove);
        site.build(plan);
        EmployeeRecord updated = employees.recordConstruction(employee);
        for (ConstructionListener listener : listeners) {
            listener.onConstructed(employee, plan);
        }
        return new ConstructionResult.Completed(plan, updated);
    }
}
