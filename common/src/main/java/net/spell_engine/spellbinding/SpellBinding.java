package net.spell_engine.spellbinding;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellContainerHelper;
import net.spell_engine.internals.SpellRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpellBinding {
    public static final String name = "spell_binding";
    public static final Identifier ID = new Identifier(SpellEngineMod.ID, name);

    public record Offer(int id, int cost, int levelRequirement) {  }

    public static List<Offer> offersFor(ItemStack itemStack) {
        var container = SpellContainerHelper.containerFromItemStack(itemStack);
        if (container == null) {
            return List.of();
        }
        return SpellRegistry.all().entrySet().stream()
                .filter(entry -> entry.getValue().school == container.school)
                .sorted(new Comparator<Map.Entry<Identifier, Spell>>() {
                    @Override
                    public int compare(Map.Entry<Identifier, Spell> spell1, Map.Entry<Identifier, Spell> spell2) {
                        if (spell1.getValue().learn.tier > spell2.getValue().learn.tier) {
                            return 1;
                        }  else if (spell1.getValue().learn.tier < spell2.getValue().learn.tier) {
                            return -1;
                        } else {
                            return spell1.getKey().toString().compareTo(spell2.getKey().toString());
                        }
                    }
                })
                .map(entry -> new Offer(
                    SpellRegistry.rawId(entry.getKey()),
                    entry.getValue().learn.tier * entry.getValue().learn.level_cost_per_tier,
                    entry.getValue().learn.tier * entry.getValue().learn.level_requirement_per_tier
                ))
                .collect(Collectors.toList());
    }

    public static class State {
        public enum ApplyState { ALREADY_APPLIED, NO_MORE_SLOT, APPLICABLE, INVALID }
        public ApplyState state;
        public State(ApplyState state) {
            this.state = state;
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

        public static State of(int spellId, ItemStack itemStack, int lapisCost, int levelCost, int requiredLevel) {
            var validId = SpellRegistry.fromRawId(spellId);
            if (validId.isEmpty()) {
                return new State(ApplyState.INVALID);
            }
            return State.of(validId.get(), itemStack, lapisCost, levelCost, requiredLevel);
        }

        public static State of(Identifier spellId, ItemStack itemStack, int lapisCost, int levelCost, int requiredLevel) {
            var container = SpellContainerHelper.containerFromItemStack(itemStack);
            if (container == null) {
                return new State(ApplyState.INVALID);
            }
            if (container.spell_ids.contains(spellId.toString())) {
                return new State(ApplyState.ALREADY_APPLIED);
            }
            if (container.spell_ids.size() >= container.max_spell_count) {
                return new State(ApplyState.NO_MORE_SLOT);
            }
            var state = new State(ApplyState.APPLICABLE);
            state.requirements = new Requirements(lapisCost, levelCost, requiredLevel);
            return state;
        }

        public boolean readyToApply(PlayerEntity player, int lapisCount) {
            return state == SpellBinding.State.ApplyState.APPLICABLE
                    && requirements != null
                    && requirements.satisfiedFor(player, lapisCount);
        }
    }
}
