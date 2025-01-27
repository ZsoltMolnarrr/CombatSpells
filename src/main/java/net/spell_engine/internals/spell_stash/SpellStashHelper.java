package net.spell_engine.internals.spell_stash;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.SpellTriggers;

import java.util.HashMap;
import java.util.Map;

public class SpellStashHelper {
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(SpellStashHelper::link);
    }

    private static void link(MinecraftServer minecraftServer) {
        var manager = minecraftServer.getRegistryManager();
        var registry = manager.get(SpellRegistry.KEY);
        registry.streamEntries().forEach(entry -> {
            var spell = entry.value();
            var id = entry.getKey().get().getValue();
            if (spell.deliver.type == Spell.Delivery.Type.STASH_EFFECT) {
                if (spell.deliver.stash_effect == null) {
                    System.err.println("Spell Engine: Stash spell linking error! Spell:" + id + " is missing `stash_effect`!");
                    return;
                }
                var stash = spell.deliver.stash_effect;
                if (stash.id == null || stash.id.isEmpty()) {
                    System.err.println("Spell Engine: Stash spell linking error! Spell:" + id + " is missing `stash_effect.id`!");
                    return;
                }
                var trigger = stash.trigger;
                if (trigger == null) {
                    System.err.println("Spell Engine: Stash spell linking error! Spell:" + id + " is missing `stash_effect.trigger`!");
                    return;
                }
                var effectId = Identifier.of(stash.id);
                var statusEffect = Registries.STATUS_EFFECT.get(effectId);
                if (statusEffect == null) {
                    System.err.println("Spell Engine: Stash spell linking error! Spell:" + id + " found no status effect for `stash_effect.id`: " + stash.id);
                    return;
                }
                SpellStash.configure(statusEffect, entry, trigger, stash.consume);
            }
        });
    }

//    public static void onArrowHit(ArrowExtension arrow, PlayerEntity shooter, Entity target) {
//        useStashes(shooter, Spell.Trigger.Type.ARROW_SHOT, target, true, arrow, false);
//    }

//    public static void onMeleeHit(PlayerEntity caster, Entity target) {
//        useStashes(caster, Spell.Trigger.Type.MELEE_HIT, target, true, null, false);
//    }
//
//    public static void onSpellHit(PlayerEntity caster, Entity target, RegistryEntry<Spell> spell) {
//        // `spell` parameter currently ignored, maybe use for filtering in the future
//        useStashes(caster, Spell.Trigger.Type.SPELL_HIT, target, true, null, false);
//    }

    public static void useStashes(SpellTriggers.Event event) {
        var caster = event.player;
        var world = caster.getWorld();
        Map<StatusEffectInstance, Integer> updateEffectStacks = new HashMap<>();
        var activeEffects = caster.getActiveStatusEffects();
        for(var entry: activeEffects.entrySet()) {
            var effect = entry.getKey().value();
            var stack = entry.getValue();

            for (var stashedSpell: ((SpellStash) effect).getStashedSpells()) {
                var spellEntry = stashedSpell.spell();
                var trigger = stashedSpell.trigger();
                if (spellEntry == null || trigger == null) { continue; }
                if (!SpellTriggers.matches(trigger, event)) { continue; }

                var consume = stashedSpell.consume();
                var stacksAvailable = updateEffectStacks.getOrDefault(stack, stack.getAmplifier());
                if ((stacksAvailable + 1) < consume) {
                    continue;
                }

                var applied = false;
                var arrow = event.arrow;
                if (arrow != null && spellEntry.value().arrow_perks != null) {
                    arrow.applyArrowPerks(spellEntry);
                    applied = true;
                }

                var target = event.target;
                if (target != null) {
                    SpellHelper.performImpacts(world, caster, target, target, spellEntry, spellEntry.value().impact,
                            new SpellHelper.ImpactContext().position(target.getPos()));
                    applied = true;
                }

                if (applied) {
                    updateEffectStacks.put(stack, stacksAvailable - consume);
                }
            }
        }

        for (var entry: updateEffectStacks.entrySet()) {
            var instance = entry.getKey();
            var newAmplifier = entry.getValue();
            var effect = instance.getEffectType();

            caster.removeStatusEffect(effect);
            if (newAmplifier >= 0) {
                caster.addStatusEffect(new StatusEffectInstance(effect, instance.getDuration(), newAmplifier,
                        instance.isAmbient(), instance.shouldShowParticles(), instance.shouldShowIcon()));
            }
        }
    }
}
