package io.github.linpatr.groundwork.core.material;

import io.github.linpatr.groundwork.core.Id;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillOfMaterialsTest {
    private static final Id IRON = Id.parse("minecraft:iron_ingot");
    private static final Id STONE = Id.parse("minecraft:stone");

    @Test
    void mergesRepeatedMaterialsAndKeepsOrder() {
        BillOfMaterials bill = BillOfMaterials.builder().add(STONE, 4).add(IRON, 2).add(STONE, 1).build();
        assertEquals(List.of(STONE, IRON), List.copyOf(bill.amounts().keySet()));
        assertEquals(5, bill.amountOf(STONE));
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThrows(IllegalArgumentException.class, () -> BillOfMaterials.builder().add(IRON, 0));
    }

    @Test
    void reportsOnlyWhatIsMissing() {
        BillOfMaterials bill = BillOfMaterials.builder().add(STONE, 10).add(IRON, 3).build();
        FakeMaterials stock = new FakeMaterials(Map.of(STONE, 4, IRON, 3));
        BillOfMaterials shortfall = bill.shortfall(stock);
        assertEquals(Map.of(STONE, 6), shortfall.amounts());
    }

    @Test
    void unlimitedSourceCoversAnything() {
        BillOfMaterials bill = BillOfMaterials.builder().add(STONE, 1_000_000).build();
        assertTrue(bill.shortfall(MaterialSource.UNLIMITED).isEmpty());
    }
}
