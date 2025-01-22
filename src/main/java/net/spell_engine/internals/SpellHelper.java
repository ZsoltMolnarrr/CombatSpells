package net.spell_engine.internals;

import com.google.common.base.Suppliers;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.effect.EntityImmunity;
import net.spell_engine.api.entity.SpellSpawnedEntity;
import net.spell_engine.api.event.CombatEvents;
import net.spell_engine.api.item.trinket.ISpellBookItem;
import net.spell_engine.api.spell.event.CustomSpellHandler;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.entity.ConfigurableKnockback;
import net.spell_engine.entity.SpellCloud;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.internals.arrow.ArrowHelper;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCasterEntity;
import net.spell_engine.particle.ParticleHelper;
import net.spell_engine.utils.*;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellDamageSource;
import net.spell_power.api.SpellPower;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class SpellHelper {
    public static SpellCast.Attempt attemptCasting(PlayerEntity player, ItemStack itemStack, Identifier spellId) {
        return attemptCasting(player, itemStack, spellId, true);
    }

    public static SpellCast.Attempt attemptCasting(PlayerEntity player, ItemStack itemStack, Identifier spellId, boolean checkAmmo) {
        var caster = (SpellCasterEntity)player;
        var spellEntry = SpellRegistry.from(player.getWorld()).getEntry(spellId).orElse(null);
        if (spellEntry == null) {
            return SpellCast.Attempt.none();
        }
        var spell = spellEntry.value();
        if (caster.getCooldownManager().isCoolingDown(spellId)) {
            return SpellCast.Attempt.failOnCooldown(new SpellCast.Attempt.OnCooldownInfo());
        }
        if (checkAmmo) {
            var ammoResult = Ammo.ammoForSpell(player, spell, itemStack);
            if (!ammoResult.satisfied()) {
                return SpellCast.Attempt.failMissingItem(new SpellCast.Attempt.MissingItemInfo(ammoResult.item()));
            }
        }
        return SpellCast.Attempt.success();
    }

    public static float hasteAffectedValue(float value, float haste) {
        return value / haste;
    }

    public static float hasteAffectedValue(LivingEntity caster, SpellSchool school, float value) {
        return hasteAffectedValue(caster, school, value, null);
    }

    public static float hasteAffectedValue(LivingEntity caster, SpellSchool school, float value, ItemStack provisionedWeapon) {
        var haste = SpellPower.getHaste(caster, school); // FIXME: ? Provisioned weapon
        return hasteAffectedValue(value, haste) ;
    }

    public static float getRange(PlayerEntity player, Spell spell) {
        return spell.range_melee
                ? (float) player.getEntityInteractionRange() + spell.range
                : spell.range;
    }

    public static float getCastDuration(LivingEntity caster, Spell spell) {
        return getCastDuration(caster, spell, null);
    }

    public static float getCastDuration(LivingEntity caster, Spell spell, ItemStack provisionedWeapon) {
        if (spell.cast == null) {
            return 0;
        }
        return hasteAffectedValue(caster, spell.school, spell.cast.duration, provisionedWeapon);
    }

    public static SpellCast.Duration getCastTimeDetails(LivingEntity caster, Spell spell) {
        var haste = spell.cast.haste_affected
                ? (float) SpellPower.getHaste(caster, spell.school)
                : 1F;
        var duration =  hasteAffectedValue(spell.cast.duration, haste);
        return new SpellCast.Duration(haste, Math.round(duration * 20F));
    }

    public static float getCooldownDuration(LivingEntity caster, Spell spell) {
        return getCooldownDuration(caster, spell, null);
    }

    public static float getCooldownDuration(LivingEntity caster, Spell spell, ItemStack provisionedWeapon) {
        var duration = spell.cost.cooldown.duration;
        if (duration > 0) {
            if (SpellEngineMod.config.haste_affects_cooldown && spell.cost.cooldown.haste_affected) {
                duration = hasteAffectedValue(caster, spell.school, spell.cost.cooldown.duration, provisionedWeapon);
            }
        }
        return duration;
    }

    public static boolean isChanneled(Spell spell) {
        return channelValueMultiplier(spell) != 0;
    }

    public static boolean isInstant(Spell spell) {
        return spell.cast.duration == 0;
    }

    public static float channelValueMultiplier(Spell spell) {
        var ticks = spell.cast.channel_ticks;
        if (ticks <= 0) {
            return 0;
        }
        return ((float)ticks) / 20F;
    }

    public static void startCasting(PlayerEntity player, Identifier spellId, float speed, int length) {
        var spellEntry = SpellRegistry.from(player.getWorld()).getEntry(spellId).orElse(null);
        if (spellEntry == null) {
            return;
        }
        var spell = spellEntry.value();
        var itemStack = player.getMainHandStack();
        var attempt = attemptCasting(player, itemStack, spellId);
        if (!attempt.isSuccess()) {
            return;
        }
        // Allow clients to specify their haste without validation
        // var details = SpellHelper.getCastTimeDetails(player, spell);
        var process = new SpellCast.Process(spellEntry, itemStack.getItem(), speed, length, player.getWorld().getTime());
        SpellCastSyncHelper.setCasting(player, process);
        SoundHelper.playSound(player.getWorld(), player, spell.cast.start_sound);
    }

    public static void performSpell(World world, PlayerEntity player, Identifier spellId, TargetHelper.SpellTargetResult targetResult, SpellCast.Action action, float progress) {
        if (player.isSpectator()) { return; }
        var optionalSpellEntry = SpellRegistry.from(world).getEntry(spellId);
        if (optionalSpellEntry.isEmpty()) {
            return;
        }
        var spellEntry = optionalSpellEntry.get();
        var spell = spellEntry.value();

        var heldItemStack = player.getMainHandStack();
        var spellSource = SpellContainerHelper.getFirstSourceOfSpell(spellId, player);
        if (spellSource == null) {
            return;
        }
        var attempt = attemptCasting(player, heldItemStack, spellId);
        if (!attempt.isSuccess()) {
            return;
        }
        var caster = (SpellCasterEntity)player;
        var targets = targetResult.entities();
        var targetLocation = targetResult.location();
        var castingSpeed = caster.getCurrentCastingSpeed();
        // Normalized progress in 0 to 1
        progress = Math.max(Math.min(progress, 1F), 0F);
        var channelMultiplier = 1F;
        var channelTickIndex = 0;
        int incrementChannelTicks = 0;
        boolean shouldPerformImpact = true;
        Supplier<Collection<ServerPlayerEntity>> trackingPlayers = Suppliers.memoize(() -> { // Suppliers.memoize = Lazy
            return PlayerLookup.tracking(player);
        });
        switch (action) {
            case CHANNEL -> {
                channelTickIndex = caster.getChannelTickIndex();
                incrementChannelTicks = 1;
                channelMultiplier = channelValueMultiplier(spell);
            }
            case RELEASE -> {
                if (isChanneled(spell)) {
                    shouldPerformImpact = false;
                    channelMultiplier = 1;
                } else {
                    channelMultiplier = (progress >= 1) ? 1 : 0;
                }
                SpellCastSyncHelper.clearCasting(player);
            }
        }
        var ammoResult = Ammo.ammoForSpell(player, spell, heldItemStack);

        if (channelMultiplier > 0 && ammoResult.satisfied()) {
            var targeting = spell.release.target;
            boolean released = action == SpellCast.Action.RELEASE;
            if (shouldPerformImpact) {
                var context = new ImpactContext(channelMultiplier,
                        1F,
                        null,
                        SpellPower.getSpellPower(spell.school, player),
                        impactTargetingMode(spell),
                        channelTickIndex);

                if (spell.release.custom_impact) {
                    var handler = CustomSpellHandler.handlers.get(spellId);
                    released = false;
                    if (handler != null) {
                        released = handler.apply(new CustomSpellHandler.Data(
                                player, targets, heldItemStack, action, progress, context));
                    }
                } else {
                    switch (targeting.type) {
                        case AREA -> {
                            var center = player.getPos().add(0, player.getHeight() / 2F, 0);
                            var area = spell.release.target.area;
                            var range = getRange(player, spell) * player.getScale();
                            applyAreaImpact(world, player, targets, range, area, spellEntry, spell.impact, context.position(center), true);
                        }
                        case BEAM -> {
                            beamImpact(world, player, targets, spellEntry, context);
                        }
                        case CLOUD -> {
                            placeCloud(world, player, spellEntry, context);
                            released = true;
                        }
                        case CURSOR -> {
                            var target = targets.stream().findFirst();
                            if (target.isPresent()) {
                                directImpact(world, player, target.get(), spellEntry, context);
                            } else {
                                released = false;
                            }
                        }
                        case PROJECTILE -> {
                            Entity target = null;
                            var entityFound = targets.stream().findFirst();
                            if (entityFound.isPresent()) {
                                target = entityFound.get();
                            }
                            shootProjectile(world, player, target, spellEntry, context);
                        }
                        case METEOR -> {
                            var target = targets.stream().findFirst();
                            if (target.isPresent() || targetLocation != null) {
                                // Not setting `released` flag to allow channeling
                                fallProjectile(world, player, target.orElse(null), targetLocation, spellEntry, context);
                            } else {
                                released = false;
                            }
                        }
                        case SELF -> {
                            directImpact(world, player, player, spellEntry, context);
                            released = true;
                        }
                        case SHOOT_ARROW -> {
                            ArrowHelper.shootArrow(world, player, spellEntry, context);
                            released = true;
                        }
                    }
                }
                caster.setChannelTickIndex(channelTickIndex + incrementChannelTicks);
            }
            if (released) {
                ParticleHelper.sendBatches(player, spell.release.particles);
                SoundHelper.playSound(world, player, spell.release.sound);
                AnimationHelper.sendAnimation(player, trackingPlayers.get(), SpellCast.Animation.RELEASE, spell.release.animation, castingSpeed);
                // Consume things
                // Cooldown
                imposeCooldown(player, spellSource, spellId, spell, progress);
                // Exhaust
                player.addExhaustion(spell.cost.exhaust * SpellEngineMod.config.spell_cost_exhaust_multiplier);
                // Durability
                if (SpellEngineMod.config.spell_cost_durability_allowed && spell.cost.durability > 0) {
                    var stackToDamage = spellSource.itemStack().isDamageable() ? spellSource.itemStack() : heldItemStack;
                    stackToDamage.damage(spell.cost.durability, player, EquipmentSlot.MAINHAND);
                }
                // Item
                Ammo.consume(ammoResult, player);
                // Status effect
                if (spell.cost.effect_id != null) {
                    var effect = Registries.STATUS_EFFECT.getEntry(Identifier.of(spell.cost.effect_id));
                    if (effect.isPresent()) {
                        player.removeStatusEffect(effect.get());
                    }
                }
                if (CombatEvents.SPELL_CAST.isListened()) {
                    var args = new CombatEvents.SpellCast.Args(player, spellEntry, targets, action, progress);
                    CombatEvents.SPELL_CAST.invoke((listener) -> listener.onSpellCast(args));
                }
            }
        }
    }

    public static void imposeCooldown(PlayerEntity player, SpellContainerHelper.Source source, Identifier spellId, Spell spell, float progress) {
        var duration = cooldownToSet(player, spell, progress);
        var durationTicks = Math.round(duration * 20F);
        if (duration > 0) {
            ((SpellCasterEntity) player).getCooldownManager().set(spellId, durationTicks);
        }
        if (SpellEngineMod.config.spell_item_cooldown_lock && spell.cost.cooldown.hosting_item) {
            var hostingItem = source.itemStack().getItem();
            var itemCooldowns = player.getItemCooldownManager();
            var durationLeft = ((ItemCooldownManagerExtension)itemCooldowns).SE_getLastCooldownDuration(hostingItem)
                    * itemCooldowns.getCooldownProgress(hostingItem, 0);
            if (durationTicks > durationLeft) {
                itemCooldowns.set(hostingItem, durationTicks);
            }
        }
    }

    private static float cooldownToSet(LivingEntity caster, Spell spell, float progress) {
        if (spell.cost.cooldown.proportional) {
            return getCooldownDuration(caster, spell) * progress;
        } else {
            return getCooldownDuration(caster, spell);
        }
    }

    public static float launchHeight(LivingEntity livingEntity) {
        var eyeHeight = livingEntity.getStandingEyeHeight();
        var shoulderDistance = livingEntity.getHeight() * 0.15;
        return (float) ((eyeHeight - shoulderDistance) * livingEntity.getScaleFactor());
    }

    public static Vec3d launchPoint(LivingEntity caster) {
        return launchPoint(caster, launchPointOffsetDefault);
    }

    public static float launchPointOffsetDefault = 0.5F;

    public static Vec3d launchPoint(LivingEntity caster, float forward) {
        Vec3d look = caster.getRotationVector().multiply(forward * caster.getScaleFactor());
        return caster.getPos().add(0, launchHeight(caster), 0).add(look);
    }

    public static void shootProjectile(World world, LivingEntity caster, Entity target, RegistryEntry<Spell> spellEntry, ImpactContext context) {
        shootProjectile(world, caster, target, spellEntry, context, 0);
    }

    public static void shootProjectile(World world, LivingEntity caster, Entity target, RegistryEntry<Spell> spellEntry, ImpactContext context, int sequenceIndex) {
        if (world.isClient) {
            return;
        }

        var spell = spellEntry.value();
        var launchPoint = launchPoint(caster);
        var data = spell.release.target.projectile;
        var projectileData = data.projectile;
        var mutablePerks = projectileData.perks.copy();

        var projectile = new SpellProjectile(world, caster,
                launchPoint.getX(), launchPoint.getY(), launchPoint.getZ(),
                SpellProjectile.Behaviour.FLY, spellEntry.getKey().get().getValue(), target, context, mutablePerks);

        var mutableLaunchProperties = data.launch_properties.copy();
        if (SpellEvents.PROJECTILE_SHOOT.isListened()) {
            SpellEvents.PROJECTILE_SHOOT.invoke((listener) -> listener.onProjectileLaunch(
                    new SpellEvents.ProjectileLaunchEvent(projectile, mutableLaunchProperties, caster, target, spellEntry, context, sequenceIndex)));
        }
        var velocity = mutableLaunchProperties.velocity;
        var divergence = projectileData.divergence;
        var casterPitch = caster.getPitch();
        var casterYaw = caster.getYaw();
        if (data.inherit_shooter_velocity) {
            projectile.setVelocity(caster, casterPitch, casterYaw, 0, velocity, divergence);
        } else {
            if (data.direction_offsets != null && data.direction_offsets.length > 0
                && (!data.direction_offsets_require_target || target != null)) {
                var baseIndex = context.isChanneled() ? context.channelTickIndex() : sequenceIndex;
                var index = baseIndex % data.direction_offsets.length;
                var offset = data.direction_offsets[index];
                casterPitch += offset.pitch;
                casterYaw += offset.yaw;
            }
            // var look = caster.getRotationVector().normalize();
            var look = caster.getRotationVector(casterPitch, casterYaw).normalize();
            projectile.setVelocity(look.x, look.y, look.z, velocity, divergence);
        }
        projectile.range = spell.range;
        projectile.setPitch(casterPitch);
        projectile.setYaw(casterYaw);

        projectile.setFollowedTarget(target);
        world.spawnEntity(projectile);
        SoundHelper.playSound(world, projectile, mutableLaunchProperties.sound);

        if (sequenceIndex == 0 && mutableLaunchProperties.extra_launch_count > 0) {
            for (int i = 0; i < mutableLaunchProperties.extra_launch_count; i++) {
                var ticks = (i + 1) * mutableLaunchProperties.extra_launch_delay;
                var nextSequenceIndex = i + 1;
                ((WorldScheduler)world).schedule(ticks, () -> {
                    if (caster == null || !caster.isAlive()) {
                        return;
                    }
                    shootProjectile(world, caster, target, spellEntry, context, nextSequenceIndex);
                });
            }
        }
    }

    public static boolean fallProjectile(World world, LivingEntity caster, Entity target, @Nullable Vec3d targetLocation, RegistryEntry<Spell> spellEntry, ImpactContext context) {
        return fallProjectile(world, caster, target, targetLocation, spellEntry, context, 0);
    }

    public static boolean fallProjectile(World world, LivingEntity caster, Entity target, @Nullable Vec3d targetLocation, RegistryEntry<Spell> spellEntry, ImpactContext context, int sequenceIndex) {
        if (world.isClient) {
            return false;
        }

        Vec3d targetPosition = (target != null) ? target.getPos() : targetLocation;
        if (targetPosition == null) {
            return false;
        }

        var spell = spellEntry.value();
        var meteor = spell.release.target.meteor;
        if (meteor.requires_entity && target == null) {
            return false;
        }
        var height = meteor.launch_height;
        var launchPoint = targetPosition.add(0, height, 0);
        var data = spell.release.target.meteor;
        var projectileData = data.projectile;
        var mutableLaunchProperties = data.launch_properties.copy();
        var mutablePerks = projectileData.perks.copy();

        var projectile = new SpellProjectile(world, caster,
                launchPoint.getX(), launchPoint.getY(), launchPoint.getZ(),
                SpellProjectile.Behaviour.FALL, spellEntry.getKey().get().getValue(), target, context, mutablePerks);

        if (SpellEvents.PROJECTILE_FALL.isListened()) {
            SpellEvents.PROJECTILE_FALL.invoke((listener) -> listener.onProjectileLaunch(new SpellEvents.ProjectileLaunchEvent(projectile, mutableLaunchProperties, caster, target, spellEntry, context, sequenceIndex)));
        }

        projectile.setYaw(0);
        projectile.setPitch(90);

        if (launchSequenceEligible(sequenceIndex, meteor.divergence_requires_sequence)) {
            projectile.setVelocity( 0, - 1, 0, mutableLaunchProperties.velocity, 0.5F, projectileData.divergence);
        } else {
            projectile.setVelocity(new Vec3d(0, - mutableLaunchProperties.velocity, 0));
        }
        if (launchSequenceEligible(sequenceIndex, meteor.follow_target_requires_sequence)) {
            projectile.setFollowedTarget(target);
        } else {
            projectile.setFollowedTarget(null);
        }
        if (meteor.launch_radius > 0 && launchSequenceEligible(sequenceIndex, meteor.offset_requires_sequence)) {
            var randomAngle = Math.toRadians(world.random.nextFloat() * 360);
            var offset = (new Vec3d(meteor.launch_radius, 0, 0)).rotateY((float) randomAngle);
            projectile.setPosition(projectile.getPos().add(offset));
        }

        projectile.prevYaw = projectile.getYaw();
        projectile.prevPitch = projectile.getPitch();
        projectile.range = height;

        world.spawnEntity(projectile);

        if (sequenceIndex == 0 && mutableLaunchProperties.extra_launch_count > 0) {
            for (int i = 0; i < mutableLaunchProperties.extra_launch_count; i++) {
                var ticks = (i + 1) * mutableLaunchProperties.extra_launch_delay;
                var nextSequenceIndex = i + 1;
                ((WorldScheduler)world).schedule(ticks, () -> {
                    if (caster == null || !caster.isAlive()) {
                        return;
                    }
                    fallProjectile(world, caster, target, targetLocation, spellEntry, context, nextSequenceIndex);
                });
            }
        }
        return true;
    }

    private static boolean launchSequenceEligible(int index, int rule) {
        if (rule == 0) {
            return false;
        }
        if (rule > 0) {
            return index >= rule;
        } else {
            return index < (-1 * rule);
        }
    }

    private static void directImpact(World world, LivingEntity caster, Entity target, RegistryEntry<Spell> spellEntry, ImpactContext context) {
        performImpacts(world, caster, target, target, spellEntry, spellEntry.value().impact, context);
    }

    private static void beamImpact(World world, LivingEntity caster, List<Entity> targets, RegistryEntry<Spell> spellEntry, ImpactContext context) {
        for(var target: targets) {
            performImpacts(world, caster, target, target, spellEntry, spellEntry.value().impact, context.position(target.getPos()));
        }
    }

    public static void fallImpact(LivingEntity caster, Entity projectile, RegistryEntry<Spell> spellEntry, ImpactContext context) {
        var adjustedCenter = context.position().add(0, 1, 0); // Adding a bit of height to avoid raycast hitting the ground
        performImpacts(projectile.getWorld(), caster, null, projectile, spellEntry, spellEntry.value().impact, context.position(adjustedCenter));
    }

    public static boolean projectileImpact(LivingEntity caster, Entity projectile, Entity target, RegistryEntry<Spell> spellEntry, ImpactContext context) {
        return performImpacts(projectile.getWorld(), caster, target, projectile, spellEntry, spellEntry.value().impact, context);
    }

    public static boolean arrowImpact(LivingEntity caster, Entity projectile, Entity target, RegistryEntry<Spell> spellEntry, ImpactContext context) {
        var arrowPerks = spellEntry.value().arrow_perks;
        if (arrowPerks != null && arrowPerks.impact != null) {
            return performImpacts(projectile.getWorld(), caster, target, projectile, spellEntry, arrowPerks.impact, context);
        }
        return false;
    }

    public static void lookupAndPerformAreaImpact(Spell.AreaImpact area_impact, RegistryEntry<Spell> spellEntry, LivingEntity caster, Entity exclude, Entity aoeSource,
                                                  Spell.Impact[] impacts, ImpactContext context, boolean additionalTargetLookup) {
        var center = context.position();
        var radius = area_impact.combinedRadius(context.power());
        var targets = TargetHelper.targetsFromArea(aoeSource, center, radius, area_impact.area, null);
        if (exclude != null) {
            targets.remove(exclude);
        }
        applyAreaImpact(aoeSource.getWorld(), caster, targets, radius, area_impact.area, spellEntry, impacts,
                context.target(TargetHelper.TargetingMode.AREA), additionalTargetLookup);
        ParticleHelper.sendBatches(aoeSource, area_impact.particles);
        SoundHelper.playSound(aoeSource.getWorld(), aoeSource, area_impact.sound);
    }

    private static void applyAreaImpact(World world, LivingEntity caster, List<Entity> targets,
                                        float range, Spell.Release.Target.Area area,
                                        RegistryEntry<Spell> spellEntry, Spell.Impact[] impacts, ImpactContext context, boolean additionalTargetLookup) {
        double squaredRange = range * range;
        var center = context.position();
        for(var target: targets) {
            float distanceBasedMultiplier = 1F;
            switch (area.distance_dropoff) {
                case NONE -> { }
                case SQUARED -> {
                    distanceBasedMultiplier = (float) ((squaredRange - target.squaredDistanceTo(center)) / squaredRange);
                    distanceBasedMultiplier = Math.max(distanceBasedMultiplier, 0F);
                }
            }
            performImpacts(world, caster, target, target, spellEntry, impacts, context
                            .distance(distanceBasedMultiplier),
                    additionalTargetLookup
            );
        }
    }

    public record ImpactContext(float channel, float distance, @Nullable Vec3d position, SpellPower.Result power, TargetHelper.TargetingMode targetingMode, int channelTickIndex) {
        public ImpactContext() {
            this(1, 1, null, null, TargetHelper.TargetingMode.DIRECT, 0);
        }

        public ImpactContext channeled(float multiplier) {
            return new ImpactContext(multiplier, distance, position, power, targetingMode, channelTickIndex);
        }

        public ImpactContext distance(float multiplier) {
            return new ImpactContext(channel, multiplier, position, power, targetingMode, channelTickIndex);
        }

        public ImpactContext position(Vec3d position) {
            return new ImpactContext(channel, distance, position, power, targetingMode, channelTickIndex);
        }

        public ImpactContext power(SpellPower.Result spellPower) {
            return new ImpactContext(channel, distance, position, spellPower, targetingMode, channelTickIndex);
        }

        public ImpactContext target(TargetHelper.TargetingMode targetingMode) {
            return new ImpactContext(channel, distance, position, power, targetingMode, channelTickIndex);
        }

        public boolean hasOffset() {
            return position != null;
        }

        public Vec3d knockbackDirection(Vec3d targetPosition) {
            return targetPosition.subtract(position).normalize();
        }

        public boolean isChanneled() {
            return channel != 1;
        }

        public float total() {
            return channel * distance;
        }
    }

    public static boolean performImpacts(World world, LivingEntity caster, @Nullable Entity target, Entity aoeSource, RegistryEntry<Spell> spellEntry, Spell.Impact[] impacts, ImpactContext context) {
        return performImpacts(world, caster, target, aoeSource, spellEntry, impacts, context, true);
    }

//    public static boolean performImpacts(World world, LivingEntity caster, @Nullable Entity target, Entity aoeSource, RegistryEntry<Spell> spellEntry, ImpactContext context) {
//        return performImpacts(world, caster, target, aoeSource, spellEntry, context, true);
//    }
//
//    public static boolean performImpacts(World world, LivingEntity caster, @Nullable Entity target, Entity aoeSource, RegistryEntry<Spell> spellEntry, ImpactContext context, boolean additionalTargetLookup) {
//        return performImpacts(world, caster, target, aoeSource, spellEntry, spellEntry.value().impact, context, additionalTargetLookup);
//    }

    public static boolean performImpacts(World world, LivingEntity caster, @Nullable Entity target, Entity aoeSource, RegistryEntry<Spell> spellEntry,
                                         Spell.Impact[] impacts, ImpactContext context, boolean additionalTargetLookup) {
        var trackers = target != null ? PlayerLookup.tracking(target) : null;
        var spell = spellEntry.value();
        var performed = false;
        TargetHelper.Intent selectedIntent = null;
        for (var impact: impacts) {
            var intent = intent(impact.action);
            if (!impact.action.apply_to_caster // Only filtering for cases when another entity is actually targeted
                    && (selectedIntent != null && selectedIntent != intent)) {
                // Filter out mixed intents
                // So dual intent spells either damage or heal, and not do both
                continue;
            }

            if (target != null) {
                var result = performImpact(world, caster, target, spellEntry, impact, context, trackers);
                performed = performed || result;
                if (result) {
                    selectedIntent = intent;
                }
            }
        }
        var area_impact = spell.area_impact;
        if (area_impact != null
                && additionalTargetLookup
                && (performed || target == null) ) {
            lookupAndPerformAreaImpact(area_impact, spellEntry, caster, target, aoeSource, impacts, context, false);
        }

        return performed;
    }

    private static final float knockbackDefaultStrength = 0.4F;

    private static boolean performImpact(World world, LivingEntity caster, Entity target, RegistryEntry<Spell> spellEntry, Spell.Impact impact, ImpactContext context, Collection<ServerPlayerEntity> trackers) {
        if (!target.isAttackable()) {
            return false;
        }
        var success = false;
        boolean isKnockbackPushed = false;
        var spell = spellEntry.value();
        try {
            // Guards

            if (impact.action.apply_to_caster) {
                target = caster;
            } else {
                var intent = intent(impact.action);
                if (!TargetHelper.actionAllowed(context.targetingMode(), intent, caster, target)) {
                    return false;
                }
                if (intent == TargetHelper.Intent.HARMFUL
                        && context.targetingMode() == TargetHelper.TargetingMode.AREA
                        && ((EntityImmunity)target).isImmuneTo(EntityImmunity.Type.AREA_EFFECT)) {
                    return false;
                }
            }
            var conditionResult = evaluateImpactConditions(target, impact.target_conditions);
            if (!conditionResult.allowed) {
                return false;
            }

            // Power calculation

            double particleMultiplier = 1 * context.total();
            var power = context.power();
            var school = impact.school != null ? impact.school : spell.school;
            if (power == null || power.school() != school) {
                power = SpellPower.getSpellPower(school, caster);
            }

            var bonusPower = 1 + (conditionResult.modifiers().stream().map(modifier -> modifier.power_multiplier).reduce(0F, Float::sum));
            var bonusCritChance = conditionResult.modifiers().stream().map(modifier -> modifier.critical_chance_bonus).reduce(0F, Float::sum);
            var bonusCritDamage = conditionResult.modifiers().stream().map(modifier -> modifier.critical_damage_bonus).reduce(0F, Float::sum);
            power = new SpellPower.Result(power.school(),
                    power.baseValue() * bonusPower,
                    power.criticalChance() + bonusCritChance,
                    power.criticalDamage() + bonusCritDamage);

            if (power.baseValue() < impact.action.min_power) {
                power = new SpellPower.Result(power.school(), impact.action.min_power, power.criticalChance(), power.criticalDamage());
            }

            // Action execution

            switch (impact.action.type) {
                case DAMAGE -> {
                    var damageData = impact.action.damage;
                    var knockbackMultiplier = Math.max(0F, damageData.knockback * context.total());
                    var vulnerability = SpellPower.Vulnerability.none;
                    var timeUntilRegen = target.timeUntilRegen;
                    if (target instanceof LivingEntity livingEntity) {
                        ((ConfigurableKnockback) livingEntity).pushKnockbackMultiplier_SpellEngine(context.hasOffset() ? 0 : knockbackMultiplier);
                        isKnockbackPushed = true;
                        if (damageData.bypass_iframes && SpellEngineMod.config.bypass_iframes) {
                            target.timeUntilRegen = 0;
                        }
                        vulnerability = SpellPower.getVulnerability(livingEntity, school);
                    }
                    var amount = power.randomValue(vulnerability);
                    amount *= damageData.spell_power_coefficient;
                    amount *= context.total();
                    if (context.isChanneled()) {
                        amount *= SpellPower.getHaste(caster, school);
                    }
                    particleMultiplier = power.criticalDamage() + vulnerability.criticalDamageBonus();

                    caster.onAttacking(target);
                    target.damage(SpellDamageSource.create(school, caster), (float) amount);

                    if (target instanceof LivingEntity livingEntity) {
                        ((ConfigurableKnockback)livingEntity).popKnockbackMultiplier_SpellEngine();
                        isKnockbackPushed = false;
                        target.timeUntilRegen = timeUntilRegen;
                        if (context.hasOffset()) {
                            var direction = context.knockbackDirection(livingEntity.getPos()).negate(); // Negate for smart Vanilla API :)
                            livingEntity.takeKnockback(knockbackDefaultStrength * knockbackMultiplier, direction.x, direction.z);
                        }
                    }
                    success = true;
                }
                case HEAL -> {
                    if (target instanceof LivingEntity livingTarget) {
                        var healData = impact.action.heal;
                        particleMultiplier = power.criticalDamage();
                        var amount = power.randomValue();
                        amount *= healData.spell_power_coefficient;
                        amount *= context.total();
                        if (context.isChanneled()) {
                            amount *= SpellPower.getHaste(caster, school);
                        }

                        livingTarget.heal((float) amount);
                        success = true;
                    }
                }
                case STATUS_EFFECT -> {
                    var data = impact.action.status_effect;
                    if (target instanceof LivingEntity livingTarget) {
                        var id = Identifier.of(data.effect_id);
                        var effectQuery = Registries.STATUS_EFFECT.getEntry(id);
                        if (effectQuery.isEmpty()) {
                            return false;
                        }
                        var effect = effectQuery.get();
                        if(!underApplyLimit(power, livingTarget, school, data.apply_limit)) {
                            return false;
                        }
                        var amplifier = data.amplifier + (int)(data.amplifier_power_multiplier * power.nonCriticalValue());
                        switch (data.apply_mode) {
                            case ADD, SET -> {
                                var duration = Math.round(data.duration * 20F);
                                var showParticles = data.show_particles;

                                if (data.apply_mode == Spell.Impact.Action.StatusEffect.ApplyMode.ADD) {
                                    var currentEffect = livingTarget.getStatusEffect(effect);
                                    int newAmplifier = 0;
                                    if (currentEffect != null) {
                                        var incrementedAmplifier = currentEffect.getAmplifier() + 1;
                                        newAmplifier = Math.min(incrementedAmplifier, amplifier);
                                    }
                                    amplifier = newAmplifier;
                                }

                                livingTarget.addStatusEffect(
                                        new StatusEffectInstance(effect, duration, amplifier, false, showParticles, true),
                                        caster);
                                success = true;
                            }
                            case REMOVE -> {
                                if (livingTarget.hasStatusEffect(effect)) {
                                    var currentEffect = livingTarget.getStatusEffect(effect);
                                    var newAmplifier = (amplifier > 0) ? (currentEffect.getAmplifier() - amplifier) : -1;
                                    if (newAmplifier < 0) {
                                        livingTarget.removeStatusEffect(effect);
                                    } else {
                                        livingTarget.addStatusEffect(new StatusEffectInstance(
                                                effect, currentEffect.getDuration(), newAmplifier, currentEffect.isAmbient(), currentEffect.shouldShowParticles(),
                                                        currentEffect.shouldShowIcon()),
                                                caster);
                                    }
                                    success = true;
                                }
                            }
                        }
                    }
                }
                case FIRE -> {
                    var data = impact.action.fire;
                    target.setOnFireFor(data.duration);
                    if (target.getFireTicks() > 0) {
                        target.setFireTicks(target.getFireTicks() + data.tick_offset);
                    }
                }
                case SPAWN -> {
                    List<Spell.Impact.Action.Spawn> spawns;
                    if (impact.action.spawns.length > 0) {
                        spawns = List.of(impact.action.spawns);
                    } else {
                        spawns = List.of(impact.action.spawn);
                    }

                    for(var data: spawns) {
                        var id = Identifier.of(data.entity_type_id);
                        var type = Registries.ENTITY_TYPE.get(id);

                        var entity = (Entity)type.create(world);
                        applyEntityPlacement(entity, caster, target.getPos(), data.placement);
                        if (entity instanceof SpellSpawnedEntity spellSpawnedEntity) {
                            spellSpawnedEntity.onCreatedFromSpell(caster, spellEntry.getKey().get().getValue(), data);
                        }
                        ((WorldScheduler)world).schedule(data.delay_ticks, () -> {
                            world.spawnEntity(entity);
                        });
                        success = true;
                    }
                }
                case TELEPORT -> {
                    var data = impact.action.teleport;
                    if (target instanceof LivingEntity livingTarget) {
                        LivingEntity teleportedEntity = null;
                        Vec3d destination = null;
                        Vec3d startingPosition = null;
                        Float applyRotation = null;
                        switch (data.mode) {
                            case FORWARD -> {
                                teleportedEntity = livingTarget;
                                var forward = data.forward;
                                var look = target.getRotationVector();
                                startingPosition = target.getPos();
                                destination = TargetHelper.findTeleportDestination(teleportedEntity, look, forward.distance, data.required_clearance_block_y);
                                var groundJustBelow = TargetHelper.findSolidBlockBelow(teleportedEntity, destination, target.getWorld(), -1.5F);
                                if (groundJustBelow != null) {
                                    destination = groundJustBelow;
                                }
                            }
                            case BEHIND_TARGET -> {
                                if (livingTarget == caster) {
                                    return false;
                                }
                                var look = target.getRotationVector();
                                var distance = 1F;
                                if (data.behind_target != null) {
                                    distance = data.behind_target.distance;
                                }
                                teleportedEntity = caster;
                                startingPosition = caster.getPos();
                                destination = target.getPos().add(look.multiply(-distance));
                                var groundJustBelow = TargetHelper.findSolidBlockBelow(teleportedEntity, destination, target.getWorld(), -1.5F);
                                if (groundJustBelow != null) {
                                    destination = groundJustBelow;
                                }

                                double x = look.x;
                                double z = look.z;
                                // Calculate yaw using arctangent function
                                float yaw = (float) Math.toDegrees(Math.atan2(-x, z));
                                // Normalize yaw to the range [0, 360)
                                yaw = yaw < 0 ? yaw + 360 : yaw;
                                applyRotation = yaw;
                            }
                        }
                        if (destination != null && startingPosition != null && teleportedEntity != null) {
                            ParticleHelper.sendBatches(teleportedEntity, data.depart_particles, false);
                            world.emitGameEvent(GameEvent.TELEPORT, startingPosition, GameEvent.Emitter.of(teleportedEntity));

                            if (applyRotation != null
                                    && teleportedEntity instanceof ServerPlayerEntity serverPlayer
                                    && world instanceof ServerWorld serverWorld) {
                                serverPlayer.teleport(serverWorld, destination.x, destination.y, destination.z, applyRotation, serverPlayer.getPitch());
                                // teleportedEntity.teleport(destination.x, destination.y, destination.z, new HashSet<>(), applyRotation, 0);
                            } else {
                                teleportedEntity.teleport(destination.x, destination.y, destination.z, false);
                            }
                            success = true;

                            ParticleHelper.sendBatches(teleportedEntity, data.arrive_particles, false);
                        }
                    }
                }
            }
            if (success) {
                if (impact.particles != null) {
                    ParticleHelper.sendBatches(target, impact.particles, (float) particleMultiplier * caster.getScale(), trackers);
                }
                if (impact.sound != null) {
                    SoundHelper.playSound(world, target, impact.sound);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to perform impact effect");
            System.err.println(e.getMessage());
            if (isKnockbackPushed) {
                ((ConfigurableKnockback)target).popKnockbackMultiplier_SpellEngine();
            }
        }
        return success;
    }

    public record TargetConditionResult(boolean allowed, List<Spell.Impact.Modifier> modifiers) {
        public static final TargetConditionResult ALLOWED = new TargetConditionResult(true, List.of());
        public static final TargetConditionResult DENIED = new TargetConditionResult(false, List.of());
    }
    public static TargetConditionResult evaluateImpactConditions(Entity target, Spell.Impact.TargetCondition[] target_conditions) {
        if (target_conditions == null) {
            return TargetConditionResult.ALLOWED;
        }
        var modifiers = new ArrayList<Spell.Impact.Modifier>();
        for (var condition: target_conditions) {
            var entityTypeMatches = false;
            if (condition.entity_type != null && !condition.entity_type.isEmpty()) {
                var invert = condition.entity_type.startsWith("!");
                if (invert) {
                    entityTypeMatches = !entityTypeMatches(condition.entity_type.substring(1), target);
                } else {
                    entityTypeMatches = entityTypeMatches(condition.entity_type, target);
                }
            }
            var hasStatusEffect = false;
            if (condition.has_effect != null && !condition.has_effect.isEmpty() && target instanceof LivingEntity livingTarget) {
                var invert = condition.has_effect.startsWith("!");
                var idString = invert ? condition.has_effect.substring(1) : condition.has_effect;
                var id = Identifier.of(idString);
                var effect = Registries.STATUS_EFFECT.getEntry(id);
                hasStatusEffect = effect.isPresent() && livingTarget.hasStatusEffect(effect.get());
            }

            var conditionMet = condition.all_required
                    ? entityTypeMatches && hasStatusEffect
                    : entityTypeMatches || hasStatusEffect;
            if (conditionMet) {
                if (!condition.allow_action) {
                    return TargetConditionResult.DENIED;
                }
                if (condition.modifier != null) {
                    modifiers.add(condition.modifier);
                }
            }
        }
        return new TargetConditionResult(true, modifiers);
    }

    public static boolean entityTypeMatches(String typeString, Entity target) {
        if (typeString.startsWith("#")) {
            var id = Identifier.of(typeString.substring(1));
            var tag = TagKey.of(Registries.ENTITY_TYPE.getKey(), id);
            if (tag != null) {
                return target.getType().isIn(tag);
            }
        } else {
            var id = Identifier.of(typeString);
            var type = Registries.ENTITY_TYPE.getEntry(id);
            if (type.isPresent()) {
                return type.get().value().equals(target.getType());
            }
        }
        return false;
    }

    public static void placeCloud(World world, LivingEntity caster, RegistryEntry<Spell> spellEntry, ImpactContext context) {
        var spell = spellEntry.value();

        List<Spell.Release.Target.Cloud> clouds;
        if (spell.release.target.clouds.length > 0) {
            clouds = List.of(spell.release.target.clouds);
        } else {
            clouds = List.of(spell.release.target.cloud);
        }

        for (var cloud: clouds) {
            SpellCloud entity;
            if (cloud.entity_type_id != null) {
                var id = Identifier.of(cloud.entity_type_id);
                var type = Registries.ENTITY_TYPE.get(id);
                entity = (SpellCloud) type.create(world);
            } else {
                entity = new SpellCloud(world);
            }
            entity.setOwner(caster);

            entity.onCreatedFromSpell(spellEntry.getKey().get().getValue(), cloud, context);
            applyEntityPlacement(entity, caster, caster.getPos(), cloud.placement);
            ((WorldScheduler)world).schedule(cloud.delay_ticks, () -> {
                world.spawnEntity(entity);
                var sound = cloud.spawn.sound;
                if (sound != null) {
                    SoundHelper.playSound(world, entity, sound);
                }
                var particles = cloud.spawn.particles;
                if (particles != null) {
                    ParticleHelper.sendBatches(entity, particles);
                }
            });
        }
    }


    public static void applyEntityPlacement(Entity entity, LivingEntity target, Vec3d initialPosition, Spell.EntityPlacement placement) {
        var position = initialPosition;
        if (placement != null) {
            if (placement.location_offset_by_look > 0) {
                float yaw = target.getYaw() + placement.location_yaw_offset;
                position = position.add(Vec3d.fromPolar(0, yaw).multiply(placement.location_offset_by_look));
            }
            position = position.add(new Vec3d(placement.location_offset_x, placement.location_offset_y, placement.location_offset_z));
            if (placement.force_onto_ground) {
                var searchPosition = position;
                var blockPos = BlockPos.ofFloored(searchPosition.getX(), searchPosition.getY(), searchPosition.getZ());
                if (target.getWorld().getBlockState(blockPos).isSolid()) {
                    searchPosition = searchPosition.add(0, 2, 0);
                }
                var groundPosBelow = TargetHelper.findSolidBlockBelow(target, searchPosition, target.getWorld(), -20);
                position = groundPosBelow != null ? groundPosBelow : position;
            }
            if (placement.apply_yaw) {
                entity.setYaw(target.getYaw());
            }
            if (placement.apply_pitch) {
                entity.setPitch(target.getPitch());
            }
            position = position.add(new Vec3d(placement.location_offset_x, placement.location_offset_y, placement.location_offset_z));
        }
        entity.setPosition(position.getX(), position.getY(), position.getZ());
    }

    public static TargetHelper.TargetingMode selectionTargetingMode(Spell spell) {
        switch (spell.release.target.type) {
            case AREA, BEAM -> {
                return TargetHelper.TargetingMode.AREA;
            }
            case CURSOR, PROJECTILE, METEOR, SELF, CLOUD, SHOOT_ARROW -> {
                return TargetHelper.TargetingMode.DIRECT;
            }
        }
        assert true;
        return null;
    }


    public static TargetHelper.TargetingMode impactTargetingMode(Spell spell) {
        switch (spell.release.target.type) {
            case AREA, BEAM, METEOR -> {
                return TargetHelper.TargetingMode.AREA;
            }
            case CURSOR, PROJECTILE, SELF, CLOUD, SHOOT_ARROW -> {
                return TargetHelper.TargetingMode.DIRECT;
            }
        }
        assert true;
        return null;
    }

    public static EnumSet<TargetHelper.Intent> intents(Spell spell) {
        var intents = new HashSet<TargetHelper.Intent>();
        for (var impact: spell.impact) {
            intents.add(intent(impact.action));
            //return intent(impact.action);
        }
        return EnumSet.copyOf(intents);
    }

    public static TargetHelper.Intent intent(Spell.Impact.Action action) {
        switch (action.type) {
            case DAMAGE, FIRE -> {
                return TargetHelper.Intent.HARMFUL;
            }
            case HEAL, SPAWN -> {
                return TargetHelper.Intent.HELPFUL;
            }
            case STATUS_EFFECT -> {
                var data = action.status_effect;
                var id = Identifier.of(data.effect_id);
                var effect = Registries.STATUS_EFFECT.get(id);
                return effect.isBeneficial() ? TargetHelper.Intent.HELPFUL : TargetHelper.Intent.HARMFUL;
            }
            case TELEPORT -> {
                return action.teleport.intent;
            }
        }
        assert true;
        return null;
    }

    public static boolean underApplyLimit(SpellPower.Result spellPower, LivingEntity target, SpellSchool school, Spell.Impact.Action.StatusEffect.ApplyLimit limit) {
        if (limit == null) {
            return true;
        }
        var power = (float) spellPower.nonCriticalValue();
        float cap = limit.health_base + (power * limit.spell_power_multiplier);
        return cap >= target.getMaxHealth();
    }

    // DAMAGE/HEAL OUTPUT ESTIMATION

    public static EstimatedOutput estimate(Spell spell, PlayerEntity caster, ItemStack itemStack) {
        var spellSchool = spell.school;
        var damageEffects = new ArrayList<EstimatedValue>();
        var healEffects = new ArrayList<EstimatedValue>();

        boolean forSpellBook = itemStack.getItem() instanceof ISpellBookItem;
        var replaceAttributes = (caster.getMainHandStack() != itemStack && !forSpellBook);

        var heldAttributes = AttributeModifierHelper.modifierMultimap(caster.getMainHandStack());
        var itemAttributes = AttributeModifierHelper.modifierMultimap(itemStack);
        if (replaceAttributes) {
            caster.getAttributes().removeModifiers(heldAttributes);
            caster.getAttributes().addTemporaryModifiers(itemAttributes);
        }

        for (var impact: spell.impact) {
            var school = impact.school != null ? impact.school : spellSchool;
            var power = SpellPower.getSpellPower(school, caster);  // FIXME: ? Provisioned weapon
            if (power.baseValue() < impact.action.min_power) {
                power = new SpellPower.Result(power.school(), impact.action.min_power, power.criticalChance(), power.criticalDamage());
            }
            switch (impact.action.type) {
                case DAMAGE -> {
                    var damageData = impact.action.damage;
                    var damage = new EstimatedValue(power.nonCriticalValue(), power.forcedCriticalValue())
                            .multiply(damageData.spell_power_coefficient);
                    damageEffects.add(damage);
                }
                case HEAL -> {
                    var healData = impact.action.heal;
                    var healing = new EstimatedValue(power.nonCriticalValue(), power.forcedCriticalValue())
                            .multiply(healData.spell_power_coefficient);
                    healEffects.add(healing);
                }
                case STATUS_EFFECT, FIRE, SPAWN -> {
                }
            }
        }

        if (replaceAttributes) {
            caster.getAttributes().removeModifiers(itemAttributes );
            caster.getAttributes().addTemporaryModifiers(heldAttributes);
        }

        return new EstimatedOutput(damageEffects, healEffects);
    }

    public record EstimatedValue(double min, double max) {
        public EstimatedValue multiply(double value) {
            return new EstimatedValue(min * value, max * value);
        }
    }
    public record EstimatedOutput(List<EstimatedValue> damage, List<EstimatedValue> heal) { }
}