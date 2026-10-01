package io.github.linpatr.groundwork.world;

import io.github.linpatr.groundwork.core.Id;
import io.github.linpatr.groundwork.core.material.MaterialSource;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Draws materials from a player's main inventory and offhand. Worn armour is never consumed.
 * Creative-mode players get {@link MaterialSource#UNLIMITED} via {@link #of(Player)}.
 */
public final class InventoryMaterials implements MaterialSource {
    private final Inventory inventory;

    private InventoryMaterials(Inventory inventory) {
        this.inventory = inventory;
    }

    public static MaterialSource of(Player player) {
        return player.isCreative() ? MaterialSource.UNLIMITED : new InventoryMaterials(player.getInventory());
    }

    @Override
    public int count(Id material) {
        Item item = item(material);
        long total = 0;
        for (NonNullList<ItemStack> section : sections()) {
            for (ItemStack stack : section) {
                if (stack.is(item)) {
                    total += stack.getCount();
                }
            }
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    @Override
    public void remove(Id material, int amount) {
        Item item = item(material);
        int remaining = amount;
        for (NonNullList<ItemStack> section : sections()) {
            for (ItemStack stack : section) {
                if (remaining == 0) {
                    break;
                }
                if (stack.is(item)) {
                    int taken = Math.min(remaining, stack.getCount());
                    stack.shrink(taken);
                    remaining -= taken;
                }
            }
        }
        inventory.setChanged();
        if (remaining > 0) {
            throw new IllegalStateException("Inventory was missing " + remaining + " " + material);
        }
    }

    private List<NonNullList<ItemStack>> sections() {
        return List.of(inventory.items, inventory.offhand);
    }

    private static Item item(Id material) {
        return BuiltInRegistries.ITEM.get(Conversions.toResourceLocation(material));
    }
}
