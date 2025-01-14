package net.spell_engine.spellbinding;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.item.SpellEngineItemTags;
import net.spell_engine.api.item.trinket.SpellBooks;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellRegistry_V2;
import net.spell_engine.internals.SpellContainerHelper;
import net.spell_engine.internals.SpellRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SpellBinding {
    public static final Identifier ADVANCEMENT_VISIT_ID = Identifier.of(SpellEngineMod.ID, "visit_spell_binding_table");
    public static final String name = "spell_binding";
    public static final Identifier ID = Identifier.of(SpellEngineMod.ID, name);
    private static final float LIBRARY_POWER_BASE = 10;
    private static final float LIBRARY_POWER_MULTIPLIER = 1.5F;
    private static final int LIBRARY_POWER_CAP = 18;
    public static final int BOOK_OFFSET = 1;
    public enum Mode { SPELL, BOOK }
    public record Offer(int id, int levelCost, int levelRequirement, int lapisCost, boolean isPowered) {  }
    public record OfferResult(Mode mode, List<Offer> offers) { }

    public static OfferResult offersFor(World world, boolean creative, ItemStack itemStack, ItemStack consumableStack, int libraryPower) {
        if (itemStack.getItem() == Items.BOOK) {
            var books = SpellBooks.sorted(world);
            var offers = new ArrayList<Offer>();
            if (SpellEngineMod.config.spell_book_creation_enabled) {
                for (int i = 0; i < books.size(); ++i) {
                    offers.add(new Offer(
                            i + BOOK_OFFSET,
                            SpellEngineMod.config.spell_book_creation_cost,
                            SpellEngineMod.config.spell_book_creation_requirement,
                            0,
                            true));
                }
            }
            return new OfferResult(Mode.BOOK, offers);
        }

        var container = SpellContainerHelper.containerFromItemStack(itemStack);
        if (container == null) {
            return new OfferResult(Mode.SPELL, List.of());
        }
        var pool = SpellRegistry_V2.entries(world, container.pool());
        if (pool == null || pool.isEmpty()) {
            return new OfferResult(Mode.SPELL, List.of());
        }

        List<RegistryEntry<Spell>> spells;
        var consumableContainer = SpellContainerHelper.containerFromItemStack(consumableStack);
        var scrollMode = false;
        if (consumableStack.isIn(SpellEngineItemTags.SPELL_BOOK_MERGEABLE) && consumableContainer != null) {
            scrollMode = true;
            var consumableSpells = SpellRegistry_V2.entries(world, consumableContainer.pool());
            var availableSpellIds = pool.stream()
                    .map(entry -> entry.getKey().get().getValue())
                    .collect(Collectors.toSet());
            spells = consumableSpells.stream()
                    .filter(entry -> {
                        var spellId = entry.getKey().get().getValue();
                        return availableSpellIds.contains(spellId);
                    })
                    .toList();
        } else {
            spells = pool;
        }

        var spellMap = new HashMap<Identifier, Spell>(); // Refactor: remove this conversion
        spells.forEach(entry -> {
            var spell = entry.value();
            spellMap.put(entry.getKey().get().getValue(), spell);
        });
        final var finalScrollMode = scrollMode;
        return new OfferResult(Mode.SPELL,
                spellMap.entrySet().stream()
                .filter(entry ->  {
                    var spell = entry.getValue();
                    if (finalScrollMode) {
                        return spell.scroll != null;
                    } else {
                        return spell.learn != null
                                && spell.learn.enabled
                                && spell.learn.tier > 0;
                    }
                })
                .sorted(SpellContainerHelper.spellSorter)
                .map(entry -> {
                    var spell = entry.getValue();
                    if (finalScrollMode) {
                        var cost = spell.learn.tier * spell.scroll.level_cost_per_tier + spell.scroll.apply_cost_base;
                        var levelRequirement = spell.learn.tier * spell.scroll.level_requirement_per_tier;
                        return new Offer(
                                SpellRegistry.rawSpellId(entry.getKey()),
                                cost,
                                levelRequirement,
                                0,
                                true);
                    } else {
                        var cost = spell.learn.tier * spell.learn.level_cost_per_tier;
                        var levelRequirement = spell.learn.tier * spell.learn.level_requirement_per_tier;
                        return new Offer(
                                SpellRegistry.rawSpellId(entry.getKey()),
                                cost * SpellEngineMod.config.spell_binding_level_cost_multiplier,
                                levelRequirement,
                                cost * SpellEngineMod.config.spell_binding_lapis_cost_multiplier,
                                (libraryPower == LIBRARY_POWER_CAP)
                                        || ((LIBRARY_POWER_BASE + libraryPower * LIBRARY_POWER_MULTIPLIER) >= levelRequirement)
                        );
                    }
                })
                .collect(Collectors.toList())
        );
    }

    public static class State {
        public enum ApplyState { ALREADY_APPLIED, NO_MORE_SLOT, APPLICABLE, INVALID }
        public ApplyState state;
        public State(ApplyState state, Requirements requirements) {
            this.state = state;
            this.requirements = requirements;
        }

        public Requirements requirements;
        public record Requirements(int lapisCost, int levelCost, int requiredLevel) {
            public boolean satisfiedFor(PlayerEntity player, int lapisCount) {
                return player.isCreative() ||
                        (metRequiredLevel(player)
                        && hasEnoughLapis(lapisCount)
                        && hasEnoughLevelsToSpend(player));
            }

            public boolean metRequiredLevel(PlayerEntity player) {
                return player.experienceLevel >= requiredLevel;
            }

            public boolean hasEnoughLapis(int lapisCount) {
                return lapisCount >= lapisCost;
            }

            public boolean hasEnoughLevelsToSpend(PlayerEntity player) {
                return player.experienceLevel >= levelCost;
            }
        }

        public static State of(int spellId, ItemStack itemStack, int levelCost, int requiredLevel, int lapisCost) {
            var validId = SpellRegistry.fromRawSpellId(spellId);
            if (validId.isEmpty()) {
                return new State(ApplyState.INVALID, null);
            }
            return State.of(validId.get(), itemStack, levelCost, requiredLevel, lapisCost);
        }

        public static State of(Identifier spellId, ItemStack itemStack, int requiredLevel, int levelCost, int lapisCost) {
            var container = SpellContainerHelper.containerFromItemStack(itemStack);
            var requirements = new Requirements(
                    lapisCost,
                    levelCost,
                    requiredLevel);
            if (container == null) {
                return new State(ApplyState.INVALID, requirements);
            }
            if (container.spell_ids().contains(spellId.toString())) {
                return new State(ApplyState.ALREADY_APPLIED, requirements);
            }
            if (container.max_spell_count() > 0 && container.spell_ids().size() >= container.max_spell_count()) {
                return new State(ApplyState.NO_MORE_SLOT, requirements);
            }
            return new State(ApplyState.APPLICABLE, requirements);
        }

        public boolean readyToApply(PlayerEntity player, int lapisCount) {
            return state == SpellBinding.State.ApplyState.APPLICABLE
                    && requirements != null
                    && requirements.satisfiedFor(player, lapisCount);
        }

        public static State forBook(int cost, int requiredLevel) {
            var requirements = new Requirements(0, cost, requiredLevel);
            return new State(ApplyState.APPLICABLE, requirements);
        }
    }
}
