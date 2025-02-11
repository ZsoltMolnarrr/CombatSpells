package net.spell_engine.mixin.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.entity.SpellEngineAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealthImpacting {
    @Shadow public abstract double getAttributeValue(RegistryEntry<EntityAttribute> attribute);

    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float modifyHealingTaken_SpellEngine(float amount) {
        return amount * (float) SpellEngineAttributes.HEALING_TAKEN
                .asMultiplier(getAttributeValue(SpellEngineAttributes.HEALING_TAKEN.entry));
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    public float modifyDamageTaken_SpellEngine(float amount) {
        return amount * (float) SpellEngineAttributes.DAMAGE_TAKEN
                .asMultiplier(getAttributeValue(SpellEngineAttributes.DAMAGE_TAKEN.entry));
    }
}
