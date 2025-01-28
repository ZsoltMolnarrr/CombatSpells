package net.spell_engine.internals.arrow;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_engine.internals.casting.SpellCasterEntity;
import net.spell_engine.mixin.item.RangedWeaponAccessor;

public class ArrowHelper {
    public static void shootArrow(World world, LivingEntity shooter, RegistryEntry<Spell> spellEntry, SpellHelper.ImpactContext context) {
        shootArrow(world, shooter, spellEntry, context, 0);
    }

    public static void shootArrow(World world, LivingEntity shooter, RegistryEntry<Spell> spellEntry, SpellHelper.ImpactContext context, int sequenceIndex) {
        var spell = spellEntry.value();
        var shoot_arrow = spell.deliver.shoot_arrow;
        var weaponStack = shooter.getMainHandStack();

        // var weapon = weaponStack.getItem();
        // Using CROSSBOW statically, so arrows fired behave consistently
        // When using any bow, divergence is applied in an unhelpful manner
        var weapon = Items.CROSSBOW;
        if (shoot_arrow != null
                && (world instanceof ServerWorld serverWorld)
                && (weapon instanceof RangedWeaponItem rangedWeapon)) {
            var launchProperties = shoot_arrow.launch_properties.copy();

            ItemStack ammo;
            if (shooter instanceof PlayerEntity player) {
                ammo = player.getProjectileType(weaponStack);
            } else {
                ammo = new ItemStack(Items.ARROW);
            }
            var loadedAmmo = RangedWeaponAccessor.load_SpellEngine(weaponStack, ammo, shooter);
            if (loadedAmmo.isEmpty()) {
                return;
            }

            // Save as active spell
            if (shooter instanceof SpellCasterEntity caster) {
                caster.setTemporaryActiveSpell(spellEntry);
            }
            var divergence = (sequenceIndex == 0) ? 0F : shoot_arrow.divergence;
            // Perform shoot
            ((RangedWeaponAccessor) rangedWeapon).shootAll_SpellEngine(
                    serverWorld,
                    shooter,
                    Hand.MAIN_HAND,
                    weaponStack,
                    loadedAmmo,
                    shoot_arrow.launch_properties.velocity,
                    divergence,
                    shoot_arrow.arrow_critical_strike,
                    null);
            // Arrow perks applied by `RangedWeaponItemMixin`

            // Fixing inconsistent Vanille code, shoot sound is played by `BOW` outside of `shootAll`
            if (weapon instanceof BowItem) {
                world.playSound(
                        null,
                        shooter.getX(),
                        shooter.getY(),
                        shooter.getZ(),
                        SoundEvents.ENTITY_ARROW_SHOOT,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + 1 * 0.5F
                );
            }

            if (shooter instanceof SpellCasterEntity caster) {
                caster.setTemporaryActiveSpell(null);
            }

            var extra_launch = launchProperties.extra_launch_count;
            if (sequenceIndex == 0 && extra_launch > 0) {
                for (int i = 0; i < extra_launch; i++) {
                    var ticks = (i + 1) * launchProperties.extra_launch_delay;
                    var nextSequenceIndex = i + 1;
                    ((WorldScheduler)world).schedule(ticks, () -> {
                        if (shooter == null || !shooter.isAlive()) {
                            return;
                        }
                        shootArrow(world, shooter, spellEntry, context, nextSequenceIndex);
                    });
                }
            }
        }
    }
}