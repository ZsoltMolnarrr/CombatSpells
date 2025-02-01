package net.spell_engine.internals.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.spell_engine.api.item.SpellEngineItemTags;
import net.spell_engine.api.item.trinket.ISpellBookItem;
import net.spell_engine.api.spell.*;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_power.api.SpellSchool;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class SpellContainerHelper {
    public static Identifier getPoolId(SpellContainer container) {
        if (container != null && container.pool() != null) {
            return Identifier.of(container.pool());
        }
        return null;
    }

    /**
     * Get the item stack in the offhand slot of the player's inventory
     * This method is used for BetterCombat mod compatibility.
     * BetterCombat overrides player.getOffHandStack() to return empty stack when player is dual wielding.
     */
    static ItemStack getOffhandItemStack(PlayerEntity player) {
        return player.getInventory().offHand.get(0);
    }

    public static SpellContainer containerFromItemStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }
        var component = itemStack.get(SpellDataComponents.SPELL_CONTAINER);
        if (component != null) {
            return component;
        }
        var id = itemStack.getItem().getRegistryEntry().getKey().get().getValue();
        return SpellAssignments.containerForItem(id);
    }

    public static boolean contains(SpellContainer container, Identifier spellId) {
        return container != null && container.spell_ids().contains(spellId.toString());
    }

    public static void addContainerToItemStack(SpellContainer container, ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }
        itemStack.set(SpellDataComponents.SPELL_CONTAINER, container);
    }

    @Nullable
    public static Identifier spellId(SpellContainer container, int selectedIndex) {
        if (container == null || !container.isUsable()) {
            return null;
        }
        return Identifier.of(container.spellId(selectedIndex));
    }

    public static SpellContainer addSpell(World world, Identifier spellId, SpellContainer container) {
        var spellIds = new ArrayList<String>(container.spell_ids());
        spellIds.add(spellId.toString());

        // Creating a map just for the sake of sorting
        HashMap<Identifier, Spell> spells = new HashMap<>();
        for (var idString : spellIds) {
            var id = Identifier.of(idString);
            var spellEntry = SpellRegistry.from(world).getEntry(id).orElse(null);
            if (spellEntry != null) {
                spells.put(id, spellEntry.value());
            }
        }
        var sortedSpellIds = spells.entrySet().stream()
                .sorted(SpellContainerHelper.spellSorter)
                .map(entry -> entry.getKey().toString())
                .collect(Collectors.toList());

        return container.copyWith(sortedSpellIds);
    }

    public static void addSpell(World world, Identifier spellId, ItemStack itemStack) {
        var container = containerFromItemStack(itemStack);
        if (container == null || !container.isValid()) {
            System.err.println("Trying to add spell: " + spellId + " to an ItemStack without valid spell container");
            return;
        }
        var modifiedContainer = addSpell(world, spellId, container);
        itemStack.set(SpellDataComponents.SPELL_CONTAINER, modifiedContainer);
    }

    public static final Comparator<Map.Entry<Identifier, Spell>> spellSorter = (spell1, spell2) -> {
        if (spell1.getValue().learn.tier > spell2.getValue().learn.tier) {
            return 1;
        } else if (spell1.getValue().learn.tier < spell2.getValue().learn.tier) {
            return -1;
        } else {
            return spell1.getKey().toString().compareTo(spell2.getKey().toString());
        }
    };

    public static boolean hasValidContainer(ItemStack itemStack) {
        return containerFromItemStack(itemStack) != null;
    }

    public static boolean hasBindableContainer(ItemStack itemStack) {
        var container = containerFromItemStack(itemStack);
        return container != null && container.pool() != null && !container.pool().isEmpty();
    }

    public static boolean hasUsableContainer(ItemStack itemStack) {
        var container = containerFromItemStack(itemStack);
        return container != null && (container.isUsable() || container.is_proxy());
    }

    public static boolean isSpellValidForItem(Item item, RegistryEntry<Spell> spell) {
        var spellType = spell.value().school.archetype == SpellSchool.Archetype.ARCHERY
                ? SpellContainer.ContentType.ARCHERY : SpellContainer.ContentType.MAGIC;
        var expectedContentType = (item instanceof RangedWeaponItem) ? SpellContainer.ContentType.ARCHERY : SpellContainer.ContentType.MAGIC;
        return spellType == expectedContentType;
    }

    public static SpellContainer.ContentType contentTypeForItem(Spell spell) {
        return spell.school.archetype == SpellSchool.Archetype.ARCHERY
                ? SpellContainer.ContentType.ARCHERY : SpellContainer.ContentType.MAGIC;
    }

    public static SpellContainer create(RegistryEntry<Spell> spell, Item item) {
        return create(List.of(spell), item);
    }

    public static SpellContainer create(List<RegistryEntry<Spell>> spells, Item item) {
        final var contentType = contentTypeForItem(spells.get(0).value());
        var isProxy = !(ISpellBookItem.isSpellBook(item) || item.getRegistryEntry().isIn(SpellEngineItemTags.SPELL_BOOK_MERGEABLE));
        var spellIds = spells.stream()
                .filter(entry -> contentTypeForItem(entry.value()) == contentType)
                .map(entry -> entry.getKey().get().getValue().toString())
                .toList();
        return new SpellContainer(contentType, isProxy, "", spellIds.size(), spellIds);
    }
}