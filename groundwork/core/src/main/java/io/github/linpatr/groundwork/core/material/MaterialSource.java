package io.github.linpatr.groundwork.core.material;

import io.github.linpatr.groundwork.core.Id;

/**
 * Somewhere construction materials can be drawn from: a player's inventory today, and potentially
 * containers, storage networks or other machines later.
 */
public interface MaterialSource {
    /** How many units of {@code material} are currently available. */
    int count(Id material);

    /**
     * Removes {@code amount} units of {@code material}. Callers must check {@link #count(Id)} first;
     * implementations may throw if the amount is not available.
     */
    void remove(Id material, int amount);

    /** A source that has every material in unlimited supply and never changes, e.g. for creative mode. */
    MaterialSource UNLIMITED = new MaterialSource() {
        @Override
        public int count(Id material) {
            return Integer.MAX_VALUE;
        }

        @Override
        public void remove(Id material, int amount) {
        }
    };
}
