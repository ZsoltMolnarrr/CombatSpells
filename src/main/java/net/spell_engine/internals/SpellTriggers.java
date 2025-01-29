package net.spell_engine.internals;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.event.CombatEvents;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.arrow.ArrowExtension;
import net.spell_engine.internals.delivery.SpellStashHelper;
import net.spell_engine.mixin.entity.LivingEntityAccessor;
import net.spell_engine.utils.ObjectHelper;
import net.spell_engine.utils.PatternMatching;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class SpellTriggers {

    public static class Event {
        /// Type of the trigger
        public final Spell.Trigger.Type type;
        /// Player that triggers the event
        public final PlayerEntity player;
        /// Entity to be used as the source of the area of effect
        public final Entity aoeSource;
        /// Target of the player, or the entity that deals damage against the player
        @Nullable public final Entity target;
        /// Arrow that was fired
        public ArrowExtension arrow;

        @Nullable public RegistryEntry<Spell> spell;
        @Nullable public Spell.Impact impact;

        @Nullable public DamageSource damageSource;
        public float damageAmount = 0;

        public Event(Spell.Trigger.Type type, PlayerEntity player, Entity aoeSource, Entity target) {
            this.type = type;
            this.player = player;
            this.aoeSource = aoeSource;
            this.target = target;
        }
    }

    public static void initialize() {
        CombatEvents.PLAYER_MELEE_ATTACK.register(args -> {
            onMeleeImpact(args.player(), args.target());
        });
        CombatEvents.PLAYER_DAMAGE_TAKEN.register(args -> {
            onDamageTaken(args.player(), args.source(), args.amount());
        });
    }

    public static void onArrowShot(ArrowExtension arrow, PlayerEntity player) {
        var event = new Event(Spell.Trigger.Type.ARROW_SHOT, player, player, null);
        event.arrow = arrow;
        fireTriggers(event);
    }

    public static void onArrowImpact(ArrowExtension arrow, PlayerEntity player, Entity target) {
        var event = new Event(Spell.Trigger.Type.ARROW_IMPACT, player, target, target);
        event.arrow = arrow;
        fireTriggers(event);
    }

    public static void onMeleeImpact(PlayerEntity player, Entity target) {
        var event = new Event(Spell.Trigger.Type.MELEE_IMPACT, player, target, target);
        if (target instanceof LivingEntity livingTarget) {
            event.damageSource = ((LivingEntityAccessor)livingTarget).getLastDamageSource();
            event.damageAmount = ((LivingEntityAccessor)livingTarget).getLastDamageTaken();
        }
        fireTriggers(event);
    }

    public static void onSpellImpactAny(PlayerEntity player, Entity target, Entity aoeSource, RegistryEntry<Spell> spell) {
        var event = new Event(Spell.Trigger.Type.SPELL_IMPACT_ANY, player, aoeSource, target);
        event.spell = spell;
        fireTriggers(event);
    }

    public static void onSpellImpactSpecific(PlayerEntity player, Entity target, RegistryEntry<Spell> spell, Spell.Impact impact) {
        var event = new Event(Spell.Trigger.Type.SPELL_IMPACT_ANY, player, target, target);
        event.spell = spell;
        event.impact = impact;
        fireTriggers(event);
    }

    public static void onDamageTaken(PlayerEntity player, DamageSource source, float amount) {
        Entity sourceEntity = source.getAttacker();
        Entity aoeSourceEntity = ObjectHelper.coalesce(sourceEntity, player);
        var event = new Event(Spell.Trigger.Type.DAMAGE_TAKEN, player, aoeSourceEntity, sourceEntity);
        event.damageSource = source;
        event.damageAmount = amount;
        fireTriggers(event);
    }

    public static void onRoll(PlayerEntity player) {
        var event = new Event(Spell.Trigger.Type.ROLL, player, player, null);
        fireTriggers(event);
    }

    private static void fireTriggers(Event event) {
        // Iterate stash effects
        SpellStashHelper.useStashes(event);
        // Iterate passive spells
        // TODO ...
    }

    private static final Random random = new Random();
    public static boolean matches(Spell.Trigger trigger, Event event) {
        if (trigger.type != event.type) {
            return false;
        }
        if (trigger.chance < 1 && random.nextFloat() > trigger.chance) {
            return false;
        }
        switch (trigger.type) {
            case SPELL_IMPACT_ANY, SPELL_IMPACT_SPECIFIC -> {
                var spell_impact = trigger.spell_impact;
                if (spell_impact != null) {
                    var spellEntry = event.spell;
                    if (spellEntry != null)  {
                        var spell = spellEntry.value();
                        if (spell_impact.school != null
                                && !spell.school.id.toString().contains(spell_impact.school.toLowerCase())) {
                            return false;
                        }
                        if (spell_impact.id != null
                                && !PatternMatching.matches(spellEntry, SpellRegistry.KEY, spell_impact.id)) {
                            return false;
                        }
                    }
                    var impact = event.impact;
                    if (impact != null) {
                        if (spell_impact.impact_type != null
                                && !impact.action.type.toString().equals(spell_impact.impact_type.toLowerCase())) {
                            return false;
                        }
                    }
                }
                return true;
            }
            default -> {
                return true;
            }
        }
    }
}