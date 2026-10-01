package io.github.linpatr.groundwork.core.material;

import io.github.linpatr.groundwork.core.Id;

import java.util.HashMap;
import java.util.Map;

/** An in-memory material source for tests. */
public final class FakeMaterials implements MaterialSource {
    private final Map<Id, Integer> stock;

    public FakeMaterials(Map<Id, Integer> stock) {
        this.stock = new HashMap<>(stock);
    }

    @Override
    public int count(Id material) {
        return stock.getOrDefault(material, 0);
    }

    @Override
    public void remove(Id material, int amount) {
        int available = count(material);
        if (available < amount) {
            throw new IllegalStateException("Only " + available + " " + material + " available, tried to remove " + amount);
        }
        stock.put(material, available - amount);
    }
}
