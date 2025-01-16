package net.spell_engine.api.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.spell.Spell;

import java.util.List;

public class CombatEvents {
    public static final Event<EntityAttack> ENTITY_ATTACK = new Event<EntityAttack>();
    public interface EntityAttack {
        record Args(LivingEntity attacker, Entity target) {}
        void onEntityAttack(Args args);
    }

    public static final Event<SpellCast> SPELL_CAST = new Event<SpellCast>();
    public interface SpellCast {
        record Args(PlayerEntity caster, RegistryEntry<Spell> spell, List<Entity> targets, net.spell_engine.internals.casting.SpellCast.Action action, float progress) {}
        void onSpellCast(Args args);
    }

    public static final Event<ItemUse> ITEM_USE = new Event<ItemUse>();
    public interface ItemUse {
        enum Stage { START, TICK, END }
        record Args(LivingEntity user, Stage stage) {}
        void onItemUseStart(Args args);
    }
}
