package net.spell_engine.internals;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.arrow.ArrowExtension;
import net.spell_engine.internals.spell_stash.SpellStashHelper;
import org.jetbrains.annotations.Nullable;

public class SpellTriggers {

    public static class Event {
        /// Type of the trigger
        public final Spell.Trigger.Type type;
        /// Player that triggers the event
        public final PlayerEntity player;
        /// Target of the player, or the entity that deals damage against the player
        @Nullable public final Entity target;
        /// Arrow that was fired
        public ArrowExtension arrow;

        public Event(Spell.Trigger.Type type, PlayerEntity player, Entity target) {
            this.type = type;
            this.player = player;
            this.target = target;
        }
    }

    public static void onArrowShot(ArrowExtension arrow, PlayerEntity shooter) {
        var event = new Event(Spell.Trigger.Type.ARROW_SHOT, shooter, null);
        event.arrow = arrow;
        fireTriggers(event);
    }
    
    public static void onArrowHit(ArrowExtension arrow, PlayerEntity shooter, Entity target) {

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
