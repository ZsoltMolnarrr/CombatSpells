package net.spell_engine.internals;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.item.SpellEngineItemTags;
import net.spell_engine.api.item.trinket.ISpellBookItem;
import net.spell_engine.api.spell.*;
import net.spell_engine.compat.trinkets.TrinketsCompat;
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

    @Deprecated(forRemoval = true)
    public static SpellContainer getEquipped(ItemStack heldItemStack, PlayerEntity player) {
        return getAvailable(player);
    }

    @Deprecated(forRemoval = true)
    public static SpellContainer getEquipped(SpellContainer starterContainer, PlayerEntity player) {
        return mergedContainerSources(player);
    }

    public static SpellContainer getAvailable(PlayerEntity player) {
        return mergedContainerSources(player);
    }

    public record Source(ItemStack itemStack, SpellContainer container) { }
    public record Query(List<Source> spell_book, List<Source> others) {
        public static final Query EMPTY = new Query(List.of(), List.of());
    }

    @Nullable public static Source getFirstSourceOfSpell(Identifier spellId, PlayerEntity player) {
        var query = getContainerSources(player);
        for (var source : query) {
            if (contains(source.container(), spellId)) {
                return source;
            }
        }
        return null;
    }

    public static List<Source> getContainerSources(PlayerEntity player) {
        var sources = new ArrayList<Source>();
        var heldItemStack = player.getMainHandStack();
        var heldContainer = containerFromItemStack(heldItemStack);
        if (heldContainer == null || !heldContainer.is_proxy()) {
            return List.of();
        }
        sources.add(new Source(heldItemStack, heldContainer));
        if (TrinketsCompat.isEnabled()) {
            var trinketSources = TrinketsCompat.getSpellContainers(player);
            sources.addAll(trinketSources.spell_book());
            sources.addAll(trinketSources.others());
        }
        if (SpellEngineMod.config.spell_container_from_offhand) {
            var offhandStack = SpellEngineMod.config.spell_container_from_offhand_ignore_dual_wielding ?
                    getOffhandItemStack(player) : player.getOffHandStack();
            addSourceIfValid(offhandStack, sources);
        }
        if (SpellEngineMod.config.spell_container_from_equipment) {
            for (var slot : player.getInventory().armor) {
                addSourceIfValid(slot, sources);
            }
        }
        return sources;
    }

    public static SpellContainer mergedContainerSources(PlayerEntity player) {
        var sources = getContainerSources(player);
        if (sources.isEmpty()) {
            return SpellContainer.EMPTY;
        }
        var heldContainer = sources.get(0).container();
        var spells = new ArrayList<RegistryEntry<Spell>>();
        var registry = SpellRegistry_V2.from(player.getWorld());
        for (var source : sources) {
            var container = source.container();
            if (container.content() == heldContainer.content()) {
                for (var idString : container.spell_ids()) {
                    var id = Identifier.of(idString);
                    var spell = registry.getEntry(id).orElse(null);
                    if (spell != null) {
                        spells.add(spell);
                    }
                }
            }
        }

        var spellIds = new LinkedHashSet<String>(); // We need the IDs only, but remove duplicates
        for (var spell : spells) {
            spellIds.add(spell.getKey().get().getValue().toString());
        }

        // Remove spells with the same group, and lower tier
        var toRemove = new HashSet<String>();
        for (var spellEntry : spells) {
            var spell = spellEntry.value();
            var tag = spell.group;
            if (tag != null) {
                for (var other : spells) {
                    var spellId = spellEntry.getKey().get();
                    var otherId = other.getKey().get();
                    if (spellId.equals(otherId)) continue;
                    if (tag.equals(other.value().group)) {
                        if (spellEntry.value().learn.tier == other.value().learn.tier) {
                            if (spellEntry.value().rank > other.value().rank) {
                                toRemove.add(otherId.toString());
                            }
                        }
                        if (spellEntry.value().learn.tier > other.value().learn.tier) {
                            toRemove.add(otherId.toString());
                        }
                    }
                }
            }
        }
        spellIds.removeAll(toRemove);

        return new SpellContainer(heldContainer.content(), heldContainer.is_proxy(), null, 0, new ArrayList<>(spellIds));
    }

    private static void addContainerIfValid(ItemStack fromItemStack, List<SpellContainer> intoContainers) {
        SpellContainer container = containerFromItemStack(fromItemStack);
        if (container != null && container.isValid()) {
            intoContainers.add(container);
        }
    }

    private static void addSourceIfValid(ItemStack fromItemStack, List<Source> sources) {
        SpellContainer container = containerFromItemStack(fromItemStack);
        if (container != null && container.isValid()) {
            sources.add(new Source(fromItemStack, container));
        }
    }

    @Deprecated(forRemoval = true)
    private static boolean isOffhandContainerValid(PlayerEntity player, SpellContainer.ContentType allowedContent) {
        ItemStack offhandItemStack = getOffhandItemStack(player);
        SpellContainer container = containerFromItemStack(offhandItemStack);
        return container != null && container.isValid() && container.content() == allowedContent;
    }

    @Deprecated(forRemoval = true)
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

    public static SpellContainer addSpell(World world, Identifier spellId, SpellContainer container) {
        var spellIds = new ArrayList<String>(container.spell_ids());
        spellIds.add(spellId.toString());

        // Creating a map just for the sake of sorting
        HashMap<Identifier, Spell> spells = new HashMap<>();
        for (var idString : spellIds) {
            var id = Identifier.of(idString);
            var spellEntry = SpellRegistry_V2.from(world).getEntry(id).orElse(null);
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