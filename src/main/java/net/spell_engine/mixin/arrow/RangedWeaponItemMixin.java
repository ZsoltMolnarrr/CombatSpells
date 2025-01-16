package net.spell_engine.mixin.arrow;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import net.spell_engine.api.effect.SpellStash;
import net.spell_engine.api.spell.SpellRegistry_V2;
import net.spell_engine.internals.arrow.ArrowExtension;
import net.spell_engine.internals.casting.SpellCasterEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;

@Mixin(RangedWeaponItem.class)
public class RangedWeaponItemMixin {

    @WrapOperation(method = "shootAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/RangedWeaponItem;createArrowEntity(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/projectile/ProjectileEntity;"))
    private ProjectileEntity shootAll_wrap_createArrowEntity(
            RangedWeaponItem instance, World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack, boolean critical,
            Operation<ProjectileEntity> original) {
        var projectile = original.call(instance, world, shooter, weaponStack, projectileStack, critical);
        if (shooter instanceof PlayerEntity player
                && projectile instanceof ArrowExtension arrow) {
            var caster = (SpellCasterEntity) player;
            var activeSpellEntry = caster.getTemporaryActiveSpell();
            if (activeSpellEntry != null) {
                if (activeSpellEntry.value().arrow_perks != null) {
                    arrow.applyArrowPerks(activeSpellEntry);
                }
            } else {
                var removeEffects = new ArrayList<RegistryEntry<StatusEffect>>();
                var addEffects = new ArrayList<StatusEffectInstance>();
                var activeEffects = shooter.getActiveStatusEffects();
                for(var entry: activeEffects.entrySet()) {
                    var effectEntry = entry.getKey();
                    var effect = entry.getKey().value();
                    var stack = entry.getValue();
                    var stashedSpell = ((SpellStash) effect).getStashedSpell();
                    if (stashedSpell != null) {
                        var spellEntry = SpellRegistry_V2.from(world).getEntry(stashedSpell.id()).orElse(null);
                        if (spellEntry != null && spellEntry.value().arrow_perks != null) {
                            arrow.applyArrowPerks(spellEntry);
                        }

                        removeEffects.add(effectEntry);
                        var newAmplifier = stack.getAmplifier() - stashedSpell.consumed();
                        if (newAmplifier >= 0) {
                            addEffects.add(new StatusEffectInstance(
                                    effectEntry, stack.getDuration(), newAmplifier,
                                    stack.isAmbient(), stack.shouldShowParticles(), stack.shouldShowIcon()));
                        }
                    }
                }
                for (var effectEntry: removeEffects) {
                    shooter.removeStatusEffect(effectEntry);
                }
                for (var effectInstance: addEffects) {
                    shooter.addStatusEffect(effectInstance);
                }
            }
        }
        return projectile;
    }
}
