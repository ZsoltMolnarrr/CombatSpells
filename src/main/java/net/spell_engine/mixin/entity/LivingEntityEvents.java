package net.spell_engine.mixin.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.api.event.CombatEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityEvents {
    @Inject(method = "onAttacking", at = @At("HEAD"))
    private void onAttacking_HEAD_Event(Entity target, CallbackInfo ci) {
        var entity = (LivingEntity) (Object) this;
        if (CombatEvents.ENTITY_ANY_ATTACK.isListened()) {
            var args = new CombatEvents.EntityAttack.Args(entity, target);
            CombatEvents.ENTITY_ANY_ATTACK.invoke(listener -> listener.onEntityAttack(args));
        }
        // Spell impact damage execution does call back here (`onAttacking`)
        // so we need to avoid infinite loop
        if (entity instanceof PlayerEntity player) {
            if (CombatEvents.PLAYER_ANY_ATTACK.isListened()) {
                var args = new CombatEvents.PlayerAttack.Args(player, target);
                CombatEvents.PLAYER_ANY_ATTACK.invoke(listener -> listener.onPlayerAttack(args));
            }
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void damage_RETURN_entity(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            var entity = (LivingEntity) (Object) this;
            if (CombatEvents.ENTITY_DAMAGE_TAKEN.isListened()) {
                var args = new CombatEvents.EntityDamageTaken.Args(entity, source, amount);
                CombatEvents.ENTITY_DAMAGE_TAKEN.invoke(listener -> listener.onDamageTaken(args));
            }
            if (entity instanceof PlayerEntity player) {
                if (CombatEvents.PLAYER_DAMAGE_TAKEN.isListened()) {
                    var args = new CombatEvents.PlayerDamageTaken.Args(player, source, amount);
                    CombatEvents.PLAYER_DAMAGE_TAKEN.invoke(listener -> listener.onPlayerDamageTaken(args));
                }
            }
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
