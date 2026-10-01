package io.github.linpatr.groundwork.core.employee;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeDirectoryTest {
    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private final AtomicInteger changes = new AtomicInteger();
    private final EmployeeDirectory directory = new EmployeeDirectory(Clock.fixed(NOW, ZoneOffset.UTC), changes::incrementAndGet);

    @Test
    void assignsSequentialNumbers() {
        EmployeeRecord first = directory.onboard(UUID.randomUUID());
        EmployeeRecord second = directory.onboard(UUID.randomUUID());
        assertEquals(1, first.employeeNumber());
        assertEquals(2, second.employeeNumber());
        assertEquals(NOW, first.hiredAt());
        assertEquals(0, first.clearance());
        assertEquals(2, changes.get());
    }

    @Test
    void cannotOnboardTwice() {
        UUID player = UUID.randomUUID();
        directory.onboard(player);
        assertThrows(IllegalStateException.class, () -> directory.onboard(player));
    }

    @Test
    void restoredRecordsContinueTheNumbering() {
        directory.restore(new EmployeeRecord(UUID.randomUUID(), 41, NOW, 3, 7));
        assertEquals(0, changes.get(), "restoring is not a change");
        assertEquals(42, directory.onboard(UUID.randomUUID()).employeeNumber());
    }

    @Test
    void updatesRecords() {
        UUID player = UUID.randomUUID();
        assertFalse(directory.isEmployed(player));
        directory.onboard(player);
        directory.setClearance(player, 2);
        directory.recordConstruction(player);
        EmployeeRecord record = directory.find(player).orElseThrow();
        assertTrue(directory.isEmployed(player));
        assertEquals(2, record.clearance());
        assertEquals(1, record.buildingsConstructed());
    }

    @Test
    void cannotUpdateUnknownPlayers() {
        assertThrows(IllegalStateException.class, () -> directory.recordConstruction(UUID.randomUUID()));
    }
}
