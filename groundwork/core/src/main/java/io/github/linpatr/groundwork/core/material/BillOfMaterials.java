package io.github.linpatr.groundwork.core.material;

import io.github.linpatr.groundwork.core.Id;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable, ordered list of materials and the quantity of each. Used for construction costs
 * and for reporting shortfalls.
 */
public final class BillOfMaterials {
    public static final BillOfMaterials EMPTY = new BillOfMaterials(Map.of());

    private final Map<Id, Integer> amounts;

    private BillOfMaterials(Map<Id, Integer> amounts) {
        this.amounts = amounts;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Quantities in declaration order. */
    public Map<Id, Integer> amounts() {
        return amounts;
    }

    public int amountOf(Id material) {
        return amounts.getOrDefault(material, 0);
    }

    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    /** The part of this bill that {@code source} cannot currently cover. Empty if it can cover all of it. */
    public BillOfMaterials shortfall(MaterialSource source) {
        Builder missing = builder();
        amounts.forEach((material, required) -> {
            int available = source.count(material);
            if (available < required) {
                missing.add(material, required - available);
            }
        });
        return missing.build();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BillOfMaterials other && amounts.equals(other.amounts);
    }

    @Override
    public int hashCode() {
        return amounts.hashCode();
    }

    @Override
    public String toString() {
        return amounts.toString();
    }

    public static final class Builder {
        private final Map<Id, Integer> amounts = new LinkedHashMap<>();

        private Builder() {
        }

        /** Adds {@code amount} of {@code material}; repeated materials are summed. */
        public Builder add(Id material, int amount) {
            Objects.requireNonNull(material, "material");
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount of " + material + " must be positive, got " + amount);
            }
            amounts.merge(material, amount, Math::addExact);
            return this;
        }

        public BillOfMaterials build() {
            return amounts.isEmpty() ? EMPTY : new BillOfMaterials(Collections.unmodifiableMap(new LinkedHashMap<>(amounts)));
        }
    }
}
