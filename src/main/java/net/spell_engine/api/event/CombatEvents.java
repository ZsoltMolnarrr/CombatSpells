package net.spell_engine.api.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.spell.Spell;

import java.util.List;

public class CombatEvents {
    public static final Event<EntityAttack> ENTITY_ANY_ATTACK = new Event<EntityAttack>();
    public interface EntityAttack {
        record Args(LivingEntity attacker, Entity target) {}
        void onEntityAttack(Args args);
    }
    public static final Event<PlayerAttack> PLAYER_ANY_ATTACK = new Event<PlayerAttack>();
    public static final Event<PlayerAttack> PLAYER_MELEE_ATTACK = new Event<PlayerAttack>();
    public interface PlayerAttack {
        record Args(PlayerEntity player, Entity target) {}
        void onPlayerAttack(Args args);
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

    public static final Event<EntityDamageTaken> ENTITY_DAMAGE_TAKEN = new Event<EntityDamageTaken>();
    public interface EntityDamageTaken {
        record Args(LivingEntity entity, DamageSource source, float amount) {}
        void onDamageTaken(Args args);
    }

    public static final Event<PlayerDamageTaken> PLAYER_DAMAGE_TAKEN = new Event<PlayerDamageTaken>();
    public interface PlayerDamageTaken {
        record Args(PlayerEntity player, DamageSource source, float amount) {}
        void onPlayerDamageTaken(Args args);
    }
}
