package io.github.linpatr.groundwork.core.employee;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A player's file with the company.
 *
 * @param playerId            the player's UUID
 * @param employeeNumber      sequential number assigned at onboarding, starting at 1
 * @param hiredAt             when the player signed on
 * @param clearance           clearance level; buildings may require a minimum level
 * @param buildingsConstructed number of buildings this employee has completed
 */
public record EmployeeRecord(UUID playerId, int employeeNumber, Instant hiredAt, int clearance, int buildingsConstructed) {
    public EmployeeRecord {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(hiredAt, "hiredAt");
        if (employeeNumber <= 0) {
            throw new IllegalArgumentException("Employee numbers start at 1, got " + employeeNumber);
        }
        if (clearance < 0 || buildingsConstructed < 0) {
            throw new IllegalArgumentException("Clearance and building count cannot be negative");
        }
    }

    public EmployeeRecord withClearance(int newClearance) {
        return new EmployeeRecord(playerId, employeeNumber, hiredAt, newClearance, buildingsConstructed);
    }

    public EmployeeRecord withConstruction() {
        return new EmployeeRecord(playerId, employeeNumber, hiredAt, clearance, buildingsConstructed + 1);
    }
}
