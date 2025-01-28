package net.spell_engine.mixin.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.api.event.CombatEvents;
import net.spell_engine.internals.SpellTriggers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityEvents {
    private boolean executingSpellTriggers = false;

    @Inject(method = "onAttacking", at = @At("HEAD"))
    private void onAttacking_HEAD_Event(Entity target, CallbackInfo ci) {
        var entity = (LivingEntity) (Object) this;
        if (CombatEvents.ENTITY_ATTACK.isListened()) {
            var args = new CombatEvents.EntityAttack.Args(entity, target);
            CombatEvents.ENTITY_ATTACK.invoke(listener -> listener.onEntityAttack(args));
        }
        // Spell impact damage execution does call back here (`onAttacking`)
        // so we need to avoid infinite loop
        if (!executingSpellTriggers && entity instanceof PlayerEntity player) {
            executingSpellTriggers = true;
            SpellTriggers.onMeleeImpact(player, target);
            executingSpellTriggers = false;
        }
    }

    @Inject(method = "tickItemStackUsage", at = @At("HEAD"))
    private void tickItemStackUsage_HEAD_Event(CallbackInfo ci) {
        var entity = (LivingEntity) (Object) this;
        if (CombatEvents.ITEM_USE.isListened()) {
            var args = new CombatEvents.ItemUse.Args(entity, CombatEvents.ItemUse.Stage.TICK);
            CombatEvents.ITEM_USE.invoke(listener -> listener.onItemUseStart(args));
        }
    }
}
