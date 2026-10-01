package io.github.linpatr.groundwork.core.employee;

import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every employee on record for one world. Persistence is the platform's job: it seeds the directory
 * with {@link #restore} and is told about changes through the {@code onChange} callback.
 */
public final class EmployeeDirectory {
    private final Map<UUID, EmployeeRecord> employees = new LinkedHashMap<>();
    private final Clock clock;
    private final Runnable onChange;
    private int lastEmployeeNumber;

    public EmployeeDirectory(Clock clock, Runnable onChange) {
        this.clock = clock;
        this.onChange = onChange;
    }

    public Optional<EmployeeRecord> find(UUID playerId) {
        return Optional.ofNullable(employees.get(playerId));
    }

    public boolean isEmployed(UUID playerId) {
        return employees.containsKey(playerId);
    }

    public Collection<EmployeeRecord> all() {
        return Collections.unmodifiableCollection(employees.values());
    }

    /**
     * Signs a player on with the next employee number at clearance 0.
     *
     * @throws IllegalStateException if the player is already employed
     */
    public EmployeeRecord onboard(UUID playerId) {
        if (employees.containsKey(playerId)) {
            throw new IllegalStateException("Player " + playerId + " is already employed");
        }
        EmployeeRecord record = new EmployeeRecord(playerId, ++lastEmployeeNumber, clock.instant(), 0, 0);
        employees.put(playerId, record);
        onChange.run();
        return record;
    }

    public EmployeeRecord setClearance(UUID playerId, int clearance) {
        return update(require(playerId).withClearance(clearance));
    }

    public EmployeeRecord recordConstruction(UUID playerId) {
        return update(require(playerId).withConstruction());
    }

    /** Loads a previously saved record without triggering {@code onChange}. */
    public void restore(EmployeeRecord record) {
        employees.put(record.playerId(), record);
        lastEmployeeNumber = Math.max(lastEmployeeNumber, record.employeeNumber());
    }

    private EmployeeRecord require(UUID playerId) {
        EmployeeRecord record = employees.get(playerId);
        if (record == null) {
            throw new IllegalStateException("Player " + playerId + " is not employed");
        }
        return record;
    }

    private EmployeeRecord update(EmployeeRecord record) {
        employees.put(record.playerId(), record);
        onChange.run();
        return record;
    }
}
