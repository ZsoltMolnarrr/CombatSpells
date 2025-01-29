package net.spell_engine.internals.container;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellContainer;
import net.spell_engine.api.spell.registry.SpellRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

public class SpellContainerSource {
    public record Result(SpellContainer actives, List<RegistryEntry<Spell>> passives, List<SpellContainerSource.SourcedContainer> sources) {
        public static final Result EMPTY = new Result(SpellContainer.EMPTY, List.of(), List.of());
    }
    public interface Owner {
//        boolean isSpellContainerSourceDirty();
//        void setSpellContainerSourceDirty(boolean dirty);

        void setSpellContainers(Result result);
        Result getSpellContainers();
    }
    public static SpellContainer activeSpellsOf(PlayerEntity player) {
        return ((Owner)player).getSpellContainers().actives;
    }
    public static List<RegistryEntry<Spell>> passiveSpellsOf(PlayerEntity player) {
        return ((Owner)player).getSpellContainers().passives;
    }
    public static Result getSpellsOf(PlayerEntity player) {
        return ((Owner)player).getSpellContainers();
    }


    public record SourcedContainer(ItemStack itemStack, SpellContainer container) { }
    public interface Source {
        List<SourcedContainer> getSpellContainers(PlayerEntity player);
    }
    public record Entry(String name, Source source) { }
    private static final List<Entry> sources = new ArrayList<>();
    private static Entry entry(String name, Source source) {
        var newEntry = new Entry(name, source);
        sources.add(newEntry);
        return newEntry;
    }
    public static final Entry MAIN_HAND = entry("main_hand", player -> {
        var heldItemStack = player.getMainHandStack();
        var sources = new ArrayList<SourcedContainer>();
        addSourceIfValid(heldItemStack, sources);
        return sources;
    });
    public static final Entry OFF_HAND = entry("off_hand", player -> {
        var offhandStack = SpellEngineMod.config.spell_container_from_offhand_ignore_dual_wielding ?
                SpellContainerHelper.getOffhandItemStack(player) : player.getOffHandStack();
        var sources = new ArrayList<SourcedContainer>();
        if (SpellEngineMod.config.spell_container_from_offhand) {
            addSourceIfValid(offhandStack, sources);
        } else {
            addSourceIfValid(offhandStack, sources, EquipmentSlot.OFFHAND.asString());
        }
        return sources;
    });
    public static final Entry EQUIPMENT = entry("equipment", player -> {
        var sources = new ArrayList<SourcedContainer>();
        if (SpellEngineMod.config.spell_container_from_equipment) {
            for (var slot : player.getInventory().armor) {
                addSourceIfValid(slot, sources);
            }
        }
        return sources;
    });
    private static void addSourceIfValid(ItemStack fromItemStack, List<SourcedContainer> sources) {
        addSourceIfValid(fromItemStack, sources, null);
    }
    private static void addSourceIfValid(ItemStack fromItemStack, List<SourcedContainer> sources, @Nullable String requiredSlot) {
        SpellContainer container = SpellContainerHelper.containerFromItemStack(fromItemStack);
        if (container != null && container.isValid()
                && (requiredSlot == null || container.slot().contains(requiredSlot)) ) {
            sources.add(new SpellContainerSource.SourcedContainer(fromItemStack, container));
        }
    }

    public static void addSource(Entry entry) {
        sources.add(entry);
    }
    public static void addSource(Entry entry, @Nullable String after) {
        boolean added = false;
        if (after != null) {
            // Index of the entry with the name `after`
            int index = -1;
            for (int i = 0; i < sources.size(); i++) {
                if (sources.get(i).name().equals(after)) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                sources.add(index + 1, entry);
                added = true;
            }
        }
        if (!added) {
            sources.add(entry);
        }
    }

    public static void update(PlayerEntity player) {
        var actives = SpellContainer.EMPTY;
        var passives = SpellContainer.EMPTY;

        var heldItemStack = player.getMainHandStack();
        var heldContainer = SpellContainerHelper.containerFromItemStack(heldItemStack);

        var allContainers = new ArrayList<SourcedContainer>();
        for (var entry : sources) {
            allContainers.addAll(entry.source().getSpellContainers(player));
        }

        if (heldContainer != null && heldContainer.is_proxy()) {
            actives = mergedContainerSources(allContainers, heldContainer.is_proxy(), heldContainer.content(), Spell.Type.ACTIVE, player.getWorld());
        }
        passives = mergedContainerSources(allContainers, false, null, Spell.Type.PASSIVE, player.getWorld());
        var passiveList = passives.spell_ids()
                .stream().map(id -> (RegistryEntry<Spell>) SpellRegistry.from(player.getWorld()).getEntry(Identifier.of(id)).orElse(null))
                .toList();

        ((Owner)player).setSpellContainers(new Result(actives, passiveList, allContainers));
    }

    public static SpellContainer mergedContainerSources(List<SourcedContainer> sources, boolean proxy, @Nullable SpellContainer.ContentType contentType, Spell.Type type, World world) {
        if (sources.isEmpty()) {
            return SpellContainer.EMPTY;
        }
        var spells = new ArrayList<RegistryEntry<Spell>>();
        var registry = SpellRegistry.from(world);
        for (var source : sources) {
            var container = source.container();
            if (contentType == null || container.content() == contentType) {
                for (var idString : container.spell_ids()) {
                    var id = Identifier.of(idString);
                    var spell = registry.getEntry(id).orElse(null);
                    if (spell != null && spell.value().type == type) {
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
                    var spellId = spellEntry.getKey().get().getValue();
                    var otherId = other.getKey().get().getValue();
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

        var finalContentType = contentType != null ? contentType : SpellContainer.ContentType.MAGIC;
        return new SpellContainer(finalContentType, proxy, null, 0, new ArrayList<>(spellIds));
    }

    @Nullable public static SourcedContainer getFirstSourceOfSpell(Identifier spellId, PlayerEntity player) {
        var result = ((Owner)player).getSpellContainers();
        for (var source : result.sources()) {
            if (contains(source.container(), spellId)) {
                return source;
            }
        }
        return null;
    }
    private static boolean contains(SpellContainer container, Identifier spellId) {
        return container != null && container.spell_ids().contains(spellId.toString());
    }
}
