package io.github.linpatr.groundwork.core.construction;

import java.util.UUID;

/** Notified after a building has been constructed, e.g. to attach machine logic to it. */
@FunctionalInterface
public interface ConstructionListener {
    void onConstructed(UUID employee, ConstructionPlan plan);
}
