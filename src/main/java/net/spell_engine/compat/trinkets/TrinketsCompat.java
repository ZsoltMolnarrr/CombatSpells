package net.spell_engine.compat.trinkets;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.item.trinket.ISpellBookItem;
import net.spell_engine.api.spell.SpellContainer;
import net.spell_engine.compat.ContainerCompat;
import net.spell_engine.internals.container.SpellContainerHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class TrinketsCompat {
    private static boolean intialized = false;
    private static boolean enabled = false;

    public static void init() {
        if (intialized) {
            return;
        }
        enabled = FabricLoader.getInstance().isModLoaded("trinkets");

        if (enabled) {
            TrinketsApi.registerTrinketPredicate(Identifier.of(SpellEngineMod.ID, "spell_book"), (itemStack, slotReference, livingEntity) -> {
                if (ISpellBookItem.isSpellBook(itemStack.getItem())) {
                    return TriState.TRUE;
                }
                return TriState.DEFAULT;
            });
            ContainerCompat.addProvider(TrinketsCompat::getAll);
        }
        intialized = true;
    }

    public static List<ItemStack> getAll(PlayerEntity player) {
        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) {
            return List.of();
        }
        var trinketComponent = component.get();
        return trinketComponent.getAllEquipped().stream().map(reference -> reference.getRight()).toList();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static List<SpellContainer> getEquippedSpells(PlayerEntity player) {
        if (!enabled) {
            return Collections.emptyList();
        }

        var component = TrinketsApi.getTrinketComponent(player);

        if (component.isEmpty()) {
            return Collections.emptyList();
        }

        var trinketComponent = component.get();
        var items = new LinkedHashSet<ItemStack>();
        var spellBookSlot = trinketComponent.getInventory().get("spell").get("book");

        // Add the spell book slot first
        items.add(spellBookSlot.getStack(0));

        // Add all other equipped items
        trinketComponent.getAllEquipped().forEach(pair -> {
            if (pair.getLeft().getId().contains("spell/book")) { return; } // Spell book slot is already added
            items.add(pair.getRight());
        });

        // Extract spell IDs from the containers
        // Using LinkedHashSet to preserve order and remove duplicates
        var containers = new ArrayList<SpellContainer>();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            var container = SpellContainerHelper.containerFromItemStack(stack);
            if (container != null && container.isValid()) {
                containers.add(container);
            }
        }

        return containers;
    }

    public static SpellContainerHelper.Query getSpellContainers(PlayerEntity player) {
        if (!enabled) {
            return SpellContainerHelper.Query.EMPTY;
        }
        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) {
            return SpellContainerHelper.Query.EMPTY;
        }
        var spellBooks = new ArrayList<SpellContainerHelper.Source>();
        var others = new ArrayList<SpellContainerHelper.Source>();
        var trinketComponent = component.get();
        trinketComponent.getAllEquipped().forEach(pair -> {
            var stack = pair.getRight();
            if (stack.isEmpty()) {
                return;
            }
            var container = SpellContainerHelper.containerFromItemStack(stack);
            if (container != null && container.isValid()) {
                if (pair.getLeft().getId().contains("spell/book")) {
                    spellBooks.add(new SpellContainerHelper.Source(stack, container));
                } else {
                    others.add(new SpellContainerHelper.Source(stack, container));
                }
            }
        });
        return new SpellContainerHelper.Query(spellBooks, others);
    }

    public static ItemStack getSpellBookStack(PlayerEntity player) {
        if (!enabled) {
            return ItemStack.EMPTY;
        }
        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return component.get().getInventory().get("spell").get("book").getStack(0);
    }
}