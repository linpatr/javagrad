package io.github.linpatr.groundwork;

import io.github.linpatr.groundwork.core.construction.ConstructionService;
import io.github.linpatr.groundwork.core.employee.EmployeeDirectory;
import io.github.linpatr.groundwork.employee.EmployeeData;
import net.minecraft.server.MinecraftServer;

/** Services scoped to one running server (and therefore one world save). */
public final class GroundworkServer {
    private static GroundworkServer current;

    private final MinecraftServer server;
    private final EmployeeDirectory employees;
    private final ConstructionService construction;

    private GroundworkServer(MinecraftServer server) {
        this.server = server;
        this.employees = EmployeeData.get(server).directory();
        this.construction = new ConstructionService(employees);
        construction.addListener((employee, plan) -> Groundwork.LOGGER.debug(
                "{} constructed {} at {} facing {}", employee, plan.building().id(), plan.origin(), plan.facing()));
    }

    /** The services for {@code server}, created on first use. Must be called on the server thread. */
    public static GroundworkServer of(MinecraftServer server) {
        if (current == null || current.server != server) {
            current = new GroundworkServer(server);
        }
        return current;
    }

    static void release(MinecraftServer server) {
        if (current != null && current.server == server) {
            current = null;
        }
    }

    public EmployeeDirectory employees() {
        return employees;
    }

    public ConstructionService construction() {
        return construction;
    }
}
