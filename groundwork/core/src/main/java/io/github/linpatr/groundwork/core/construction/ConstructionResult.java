package io.github.linpatr.groundwork.core.construction;

import io.github.linpatr.groundwork.core.employee.EmployeeRecord;
import io.github.linpatr.groundwork.core.material.BillOfMaterials;
import io.github.linpatr.groundwork.core.structure.GridPos;

import java.util.List;

/** Outcome of evaluating or performing a construction. */
public sealed interface ConstructionResult {
    /** True for {@link Ready} and {@link Completed}. */
    default boolean isSuccess() {
        return this instanceof Ready || this instanceof Completed;
    }

    /** The player has not signed on with the company yet. */
    record NotEmployed() implements ConstructionResult {
    }

    record InsufficientClearance(int required, int actual) implements ConstructionResult {
    }

    /** One or more positions cannot be built on. */
    record SiteBlocked(ConstructionPlan plan, List<Problem> problems) implements ConstructionResult {
        public SiteBlocked {
            problems = List.copyOf(problems);
        }

        public record Problem(GridPos pos, SiteCondition condition) {
        }
    }

    /** The material source cannot cover the cost; {@code shortfall} lists what is missing. */
    record MissingMaterials(ConstructionPlan plan, BillOfMaterials shortfall) implements ConstructionResult {
    }

    /** All checks passed; nothing has been changed yet. */
    record Ready(ConstructionPlan plan) implements ConstructionResult {
    }

    /** The building was constructed and its cost consumed. */
    record Completed(ConstructionPlan plan, EmployeeRecord employee) implements ConstructionResult {
    }
}
