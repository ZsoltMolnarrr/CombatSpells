package net.spell_engine.internals;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.arrow.ArrowExtension;
import net.spell_engine.internals.delivery.SpellStashHelper;
import org.jetbrains.annotations.Nullable;

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

        public Event(Spell.Trigger.Type type, PlayerEntity player, Entity aoeSource, Entity target) {
            this.type = type;
            this.player = player;
            this.aoeSource = aoeSource;
            this.target = target;
        }
    }

    public static void initialize() {
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

    public static boolean matches(Spell.Trigger trigger, Event event) {
        // TODO: Add more checks
        return trigger.type == event.type;
    }
}
