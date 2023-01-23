package net.spell_engine.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.SpellContainer;
import net.spell_engine.effect.SilenceEffect;
import net.spell_engine.effect.StatusEffects_SpellEngine;
import net.spell_engine.internals.*;
import net.spell_power.api.MagicSchool;
import net.spell_power.api.enchantment.MagicalItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements SpellCasterItemStack, MagicalItemStack {
    @Shadow public abstract Item getItem();

    private ItemStack itemStack() {
        return (ItemStack) ((Object)this);
    }

    @Nullable
    private SpellContainer spellContainer() {
        var nbtContainer = spellContainerFromNBT();
        if (nbtContainer != null) {
            return nbtContainer;
        }
        return spellContainerDefault();
    }

    @Nullable
    private SpellContainer spellContainerFromNBT() {
        var itemStack = itemStack();
        if (!itemStack.hasNbt()) {
            return null;
        }
        return SpellContainerHelper.fromNBT(itemStack.getNbt());
    }

    @Nullable
    private SpellContainer spellContainerDefault() {
        var item = getItem();
        var id = Registry.ITEM.getId(item);
        return SpellRegistry.containerForItem(id);
    }

    // MagicalItemStack

    public @Nullable MagicSchool getMagicSchool() {
        var container = spellContainer();
        if (container != null) {
            return container.school;
        }
        return null;
    }

    // SpellCasterItemStack

    @Nullable
    public SpellContainer getSpellContainer() {
        return spellContainer();
    }

    // Use conditions

    @Inject(method = "isUsedOnRelease", at = @At("HEAD"), cancellable = true)
    private void isUsedOnRelease_HEAD_SpellEngine(CallbackInfoReturnable<Boolean> cir) {
        if (spellContainer() == null) { return; }
        cir.setReturnValue(false);
        cir.cancel();
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void getMaxUseTime_HEAD_SpellEngine(CallbackInfoReturnable<Integer> cir) {
        if (spellContainer() == null) { return; }
        cir.setReturnValue(SpellHelper.maximumUseTicks);
        cir.cancel();
    }

    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void getUseAction_HEAD_SpellEngine(CallbackInfoReturnable<UseAction> cir) {
        if (spellContainer() == null) { return; }
        cir.setReturnValue(UseAction.NONE);
        cir.cancel();
    }

    // Start casting

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void use_HEAD_SpellEngine(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        // System.out.println("ItemStack use start");
        if (hand == Hand.OFF_HAND && !SpellEngineMod.config.offhand_casting_allowed) {
            return;
        }
        var itemStack = itemStack();
        var container = spellContainer();

        if (container == null) {
            if (user instanceof SpellCasterEntity caster && caster.getCurrentSpellId() != null) {
                caster.clearCasting();
            }

            return;
        }
        else{
            if( Arrays.stream(user.getStatusEffects().toArray()).anyMatch(effect -> ((StatusEffectInstance) effect).getEffectType() instanceof SilenceEffect silenceEffect
                    && (silenceEffect.getSchool() == null
                    || silenceEffect.getSchool() == container.school))){

                cir.setReturnValue(TypedActionResult.fail(itemStack()));
                cir.cancel();
                return;
            }
        }
        if (world.isClient) {

            if (user instanceof SpellCasterClient caster) {
                if (!caster.hasAmmoToStart(container, itemStack) || caster.isOnCooldown(container)) {
                    cir.setReturnValue(TypedActionResult.fail(itemStack()));
                    cir.cancel();
                    return;
                } else {
                    caster.castStart(container, itemStack, SpellHelper.maximumUseTicks);
                }
            }
        }
        cir.setReturnValue(TypedActionResult.consume(itemStack()));
        user.setCurrentHand(hand);
        cir.cancel();
    }

    // Tick cast

    @Inject(method = "usageTick", at = @At("HEAD"), cancellable = true)
    private void usageTick_HEAD_SpellEngine(World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        // System.out.println("ItemStack use tick A " + (world.isClient ? "CLIENT" : "SERVER"));
        var spell = spellContainer();
        if (spell == null) {
            return;
        }

        if (user instanceof PlayerEntity player) {
            if (!world.isClient) {
                // Server
                var caster = (SpellCasterEntity)player;
                if (!SpellHelper.canContinueToCastSpell(caster, caster.getCurrentSpellId())) {
                    player.stopUsingItem();
                }
                if(caster.getCurrentSpell() != null && Arrays.stream(user.getStatusEffects().toArray()).anyMatch(effect -> ((StatusEffectInstance) effect).getEffectType() instanceof SilenceEffect silenceEffect
                        && (silenceEffect.getSchool() == null
                        || silenceEffect.getSchool() == caster.getCurrentSpell().school))){
                    player.stopUsingItem();
                }
            } else {
                // Client
                if (user instanceof SpellCasterClient caster) {
                    caster.castTick(itemStack(), remainingUseTicks);
                }
            }
        }

        ci.cancel();
    }

    // Release casting

    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    private void onStoppedUsing_HEAD_SpellEngine(World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        // System.out.println("ItemStack use stop");
        var spell = spellContainer();
        if (spell == null) { return; }

        if (world.isClient) {
            if (user instanceof SpellCasterClient caster) {
                // WATCH OUT `LivingEntity.clearActiveItem` also calls `castRelease`
                // using a mixin to the method `HEAD`
                // This is to make sure the spell release is released even if switching to another item.
                caster.castRelease(itemStack(), remainingUseTicks);
            }
        }

        ci.cancel();
    }
}
