package io.github.linpatr.groundwork.core.construction;

import io.github.linpatr.groundwork.core.Id;
import io.github.linpatr.groundwork.core.building.BuildingDefinition;
import io.github.linpatr.groundwork.core.employee.EmployeeDirectory;
import io.github.linpatr.groundwork.core.material.BillOfMaterials;
import io.github.linpatr.groundwork.core.material.FakeMaterials;
import io.github.linpatr.groundwork.core.structure.Facing;
import io.github.linpatr.groundwork.core.structure.GridPos;
import io.github.linpatr.groundwork.core.structure.LayoutBlock;
import io.github.linpatr.groundwork.core.structure.StructureLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructionServiceTest {
    private static final Id STONE = Id.parse("minecraft:stone");
    private static final Id IRON = Id.parse("minecraft:iron_ingot");
    private static final GridPos ORIGIN = new GridPos(0, 64, 0);
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private EmployeeDirectory employees;
    private ConstructionService service;
    private FakeSite site;
    private BuildingDefinition wall;

    @BeforeEach
    void setUp() {
        employees = new EmployeeDirectory(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), () -> { });
        service = new ConstructionService(employees);
        site = new FakeSite();
        StructureLayout layout = new StructureLayout(List.of(
                new LayoutBlock(new GridPos(0, 0, 0), "minecraft:stone"),
                new LayoutBlock(new GridPos(1, 0, 0), "minecraft:stone"),
                new LayoutBlock(new GridPos(0, 1, 0), "minecraft:iron_block")), GridPos.ORIGIN);
        wall = new BuildingDefinition(Id.parse("groundwork:wall"), "structure", 1,
                BillOfMaterials.builder().add(STONE, 2).add(IRON, 9).build(), layout, Optional.empty());
    }

    private FakeMaterials plenty() {
        return new FakeMaterials(Map.of(STONE, 64, IRON, 64));
    }

    private void hire(int clearance) {
        employees.onboard(PLAYER);
        employees.setClearance(PLAYER, clearance);
    }

    @Test
    void requiresEmployment() {
        ConstructionResult result = service.construct(PLAYER, wall, ORIGIN, Facing.NORTH, plenty(), site);
        assertInstanceOf(ConstructionResult.NotEmployed.class, result);
        assertTrue(site.placed.isEmpty());
    }

    @Test
    void requiresClearance() {
        hire(0);
        ConstructionResult result = service.construct(PLAYER, wall, ORIGIN, Facing.NORTH, plenty(), site);
        assertEquals(new ConstructionResult.InsufficientClearance(1, 0), result);
    }

    @Test
    void reportsEveryObstructionAndChargesNothing() {
        hire(1);
        site.conditions.put(ORIGIN, SiteCondition.OBSTRUCTED);
        site.conditions.put(ORIGIN.offset(0, 1, 0), SiteCondition.OUT_OF_BOUNDS);
        FakeMaterials materials = plenty();

        ConstructionResult result = service.construct(PLAYER, wall, ORIGIN, Facing.NORTH, materials, site);

        ConstructionResult.SiteBlocked blocked = assertInstanceOf(ConstructionResult.SiteBlocked.class, result);
        assertEquals(2, blocked.problems().size());
        assertEquals(64, materials.count(STONE));
        assertTrue(site.placed.isEmpty());
    }

    @Test
    void reportsShortfallAndChargesNothing() {
        hire(1);
        FakeMaterials materials = new FakeMaterials(Map.of(STONE, 64, IRON, 4));

        ConstructionResult result = service.construct(PLAYER, wall, ORIGIN, Facing.NORTH, materials, site);

        ConstructionResult.MissingMaterials missing = assertInstanceOf(ConstructionResult.MissingMaterials.class, result);
        assertEquals(Map.of(IRON, 5), missing.shortfall().amounts());
        assertEquals(4, materials.count(IRON));
        assertTrue(site.placed.isEmpty());
    }

    @Test
    void evaluateChangesNothing() {
        hire(1);
        FakeMaterials materials = plenty();
        ConstructionResult result = service.evaluate(PLAYER, wall, ORIGIN, Facing.NORTH, materials, site);
        assertInstanceOf(ConstructionResult.Ready.class, result);
        assertEquals(64, materials.count(STONE));
        assertTrue(site.placed.isEmpty());
        assertEquals(0, employees.find(PLAYER).orElseThrow().buildingsConstructed());
    }

    @Test
    void constructsChargesAndRecords() {
        hire(1);
        FakeMaterials materials = plenty();
        List<ConstructionPlan> notified = new ArrayList<>();
        service.addListener((employee, plan) -> notified.add(plan));

        ConstructionResult result = service.construct(PLAYER, wall, ORIGIN, Facing.EAST, materials, site);

        ConstructionResult.Completed completed = assertInstanceOf(ConstructionResult.Completed.class, result);
        assertEquals(62, materials.count(STONE));
        assertEquals(55, materials.count(IRON));
        assertEquals(Map.of(
                ORIGIN, "minecraft:stone",
                ORIGIN.offset(0, 0, 1), "minecraft:stone",
                ORIGIN.offset(0, 1, 0), "minecraft:iron_block"), site.placed);
        assertEquals(1, completed.employee().buildingsConstructed());
        assertEquals(List.of(completed.plan()), notified);
    }

    @Test
    void planReportsBoundingBox() {
        ConstructionPlan plan = ConstructionPlan.of(wall, ORIGIN, Facing.WEST);
        assertEquals(new GridPos(0, 64, -1), plan.min());
        assertEquals(new GridPos(0, 65, 0), plan.max());
    }
}
