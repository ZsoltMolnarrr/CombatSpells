package net.spell_engine.internals;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.*;
import net.spell_engine.compat.TrinketsCompat;
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

    public static SpellPool getPool(SpellContainer container) {
        if (container != null && container.pool() != null) {
            var id = Identifier.of(container.pool());
            return SpellRegistry.spellPool(id);
        }
        return SpellPool.empty;
    }

    public static SpellContainer getEquipped(ItemStack heldItemStack, PlayerEntity player) {
        var weaponContainer = containerFromItemStack(heldItemStack);
        return getEquipped(weaponContainer, player);
    }

    public static SpellContainer getEquipped(SpellContainer proxyContainer, PlayerEntity player) {
        if (proxyContainer == null || !proxyContainer.is_proxy()) {
            return proxyContainer;
        }

        // Using LinkedHashSet to preserve order and remove duplicates
        var spellIds = new LinkedHashSet<>(proxyContainer.spell_ids());

        if (TrinketsCompat.isEnabled()) {
            spellIds.addAll(TrinketsCompat.getEquippedSpells(proxyContainer, player));
        }
        if (SpellEngineMod.config.spell_book_offhand) {
            if (isOffhandContainerValid(player, proxyContainer.content())) {
                spellIds.addAll(getOffhandSpellIds(player));
            }
        }

        var spells = new ArrayList<SpellInfo>();
        for (var idString : spellIds) {
            var id = Identifier.of(idString);
            var spell = SpellRegistry.getSpell(id);
            if (spell != null) {
                spells.add(new SpellInfo(spell, id));
            }
        }

        // Remove spells with the same group, and lower tier
        var toRemove = new HashSet<String>();
        for (var spell : spells) {
            var tag = spell.spell().group;
            if (tag != null) {
                for (var other : spells) {
                    if (spell.id().equals(other.id())) continue;
                    if (tag.equals(other.spell().group)) {
                        if (spell.spell().learn.tier > other.spell().learn.tier) {
                            toRemove.add(other.id().toString());
                        }
                    }
                }
            }
        }
        spellIds.removeAll(toRemove);

        return new SpellContainer(proxyContainer.content(), false, null, 0, new ArrayList<>(spellIds));
    }

    private static boolean isOffhandContainerValid(PlayerEntity player, SpellContainer.ContentType allowedContent) {
        ItemStack offhandItemStack = getOffhandItemStack(player);
        SpellContainer container = containerFromItemStack(offhandItemStack);
        return container != null && container.isValid() && container.content() == allowedContent;
    }

    private static List<String> getOffhandSpellIds(PlayerEntity player) {
        ItemStack offhandItemStack = getOffhandItemStack(player);
        SpellContainer container = containerFromItemStack(offhandItemStack);
        if (container == null) return Collections.emptyList();

        return container.spell_ids();
    }

    /**
     * Get the item stack in the offhand slot of the player's inventory
     * This method is used for BetterCombat mod compatibility.
     * BetterCombat overrides player.getOffHandStack() to return empty stack when player is dual wielding.
     */
    private static ItemStack getOffhandItemStack(PlayerEntity player) {
        return player.getInventory().offHand.get(0);
    }

    public static SpellContainer containerFromItemStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }
        var object = (Object) itemStack;
        if (object instanceof SpellCasterItemStack stack) {
            var container = stack.getSpellContainer();
            if (container != null && container.isValid()) {
                return container;
            }
        }
        return null;
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

    public static SpellContainer addSpell(Identifier spellId, SpellContainer container) {
        var spellIds = new ArrayList<String>(container.spell_ids());
        spellIds.add(spellId.toString());

        // Creating a map just for the sake of sorting
        HashMap<Identifier, Spell> spells = new HashMap<>();
        for (var idString : spellIds) {
            var id = Identifier.of(idString);
            var spell = SpellRegistry.getSpell(id);
            if (spell != null) {
                spells.put(id, spell);
            }
        }
        var sortedSpellIds = spells.entrySet().stream()
                .sorted(SpellContainerHelper.spellSorter)
                .map(entry -> entry.getKey().toString())
                .collect(Collectors.toList());

        return container.copyWith(sortedSpellIds);
    }

    public static void addSpell(Identifier spellId, ItemStack itemStack) {
        var container = containerFromItemStack(itemStack);
        if (container == null || !container.isValid()) {
            System.err.println("Trying to add spell: " + spellId + " to an ItemStack without valid spell container");
            return;
        }
        var modifiedContainer = addSpell(spellId, container);
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
}
