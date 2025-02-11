package net.spell_engine.entity;

import com.google.gson.Gson;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.entity.TwoWayCollisionChecker;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.client.render.FlyingSpellEntity;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.target.EntityRelations;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.VectorHelper;
import net.spell_power.api.SpellPower;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public class SpellProjectile extends ProjectileEntity implements FlyingSpellEntity {
    public static EntityType<SpellProjectile> ENTITY_TYPE;
    private static Random random = new Random();

    public float range = 128;
    private Spell.ProjectileData.Perks perks;
    private SpellHelper.ImpactContext context;
    public Vec3d previousVelocity;

    public SpellProjectile(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    protected SpellProjectile(World world, LivingEntity owner) {
        super(ENTITY_TYPE, world);
        this.setOwner(owner);
    }

    public enum Behaviour {
        FLY, FALL
    }

    public SpellProjectile(World world, LivingEntity caster, double x, double y, double z,
                           Behaviour behaviour, RegistryEntry<Spell> spellEntry, SpellHelper.ImpactContext context, Spell.ProjectileData.Perks mutablePerks) {
        this(world, caster);
        this.setPosition(x, y, z);
        this.setBehaviour(behaviour);
        this.setSpell(spellEntry);

        this.perks = mutablePerks;
        this.context = context;

        var projectileData = projectileData();
        if (projectileData.client_data != null && projectileData.client_data.model != null) {
            var model = projectileData.client_data.model;
            if (model.use_held_item) {
                setItemStackModel(caster.getMainHandStack());
            }
        }
    }

    /**
     * A copy of the spell projectile perks, can be safely modified
      */
    public Spell.ProjectileData.Perks mutablePerks() {
        return perks;
    }

    public Spell.ProjectileData projectileData() {
        var spell = getSpellEntry().value();
        var release = spell.deliver;
        switch (release.type) {
            case PROJECTILE -> {
                return release.projectile.projectile;
            }
            case METEOR -> {
                return release.meteor.projectile;
            }
        }
        assert true;
        return null;
    }

    public void setVelocity(double x, double y, double z, float speed, float spread, float divergence) {
        var rotX = Math.toRadians(divergence * random.nextFloat(spread, 1F));
        var rotY = Math.toRadians(360 * random.nextFloat());
        Vec3d vec3d = (new Vec3d(x, y, z))
                .rotateX((float) rotX)
                .rotateY((float) rotY)
                .multiply(speed);
        this.setVelocity(vec3d);
        double d = vec3d.horizontalLength();
        this.setYaw((float)(MathHelper.atan2(vec3d.x, vec3d.z) * 57.2957763671875));
        this.setPitch((float)(MathHelper.atan2(vec3d.y, d) * 57.2957763671875));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }

    public Entity getFollowedTarget() {
        Entity entityReference = null;
        if (getWorld().isClient) {
            var id = this.getDataTracker().get(TRACKER_TARGET_ID);
            if (id != null && id > 0) {
                entityReference = getWorld().getEntityById(id);
            }
        } else {
            entityReference = followedTarget;
        }
        if (entityReference != null && entityReference.isAttackable() && entityReference.isAlive()) {
            return entityReference;
        }
        return entityReference;
    }

//    @Override
//    public void setVelocityClient(double x, double y, double z) {
//        super.setVelocityClient(x, y, z);
//    }

    public boolean shouldRender(double distance) {
        double d0 = this.getBoundingBox().getAverageSideLength() * 4.0;
        if (Double.isNaN(d0)) {
            d0 = 4.0;
        }

        d0 *= 128.0;
        var result =  distance < d0 * d0;
        return result;
    }

    private boolean skipTravel = false;

    public void tick() {
        skipTravel = false;
        Entity entity = this.getOwner();
        var behaviour = getBehaviour();
        var spellEntry = getSpellEntry();
        if (!this.getWorld().isClient) {
            // Server side
            if (spellEntry == null) {
                System.err.println("Spell Projectile safeguard termination, failed to resolve spell: " + spellId());
                this.kill();
                return;
            }
            switch (behaviour) {
                case FLY -> {
                    if (distanceTraveled >= range || age > 1200) { // 1200 ticks = 1 minute
                        this.kill();
                        return;
                    }
                }
                case FALL -> {
                    if (distanceTraveled >= (range * 0.98)) {
                        finishFalling();
                        this.kill();
                        return;
                    }
                    if (age > 1200) { // 1200 ticks = 1 minute
                        this.kill();
                        return;
                    }
                }
            }
            if (distanceTraveled >= range || age > 1200) { // 1200 ticks = 1 minute
                this.kill();
                return;
            }
        }
        this.previousVelocity = new Vec3d(getVelocity().x, getVelocity().y, getVelocity().z);
        if (this.getWorld().isClient || (entity == null || !entity.isRemoved()) && this.getWorld().isChunkLoaded(this.getBlockPos())) {
            super.tick();

            if (!getWorld().isClient) {
                HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
                if (hitResult.getType() != HitResult.Type.MISS) {
                    switch (behaviour) {
                        case FLY -> {
                            boolean shouldCollideWithEntity = true;
                            if (hitResult.getType() == HitResult.Type.ENTITY) {
                                var target = ((EntityHitResult) hitResult).getEntity();
                                var spell = spellEntry.value();
                                if (SpellEngineMod.config.projectiles_pass_thru_irrelevant_targets
                                        && spell != null
                                        && !spell.impacts.isEmpty()
                                        && getOwner() instanceof LivingEntity owner) {
                                    var intents = SpellHelper.impactIntents(spell);

                                    boolean intentAllows = false;
                                    for (var intent: intents) {
                                        intentAllows = intentAllows || EntityRelations.actionAllowed(SpellTarget.FocusMode.DIRECT, intent, owner, target);
                                    }
                                    shouldCollideWithEntity = intentAllows;
                                }
                            }
                            if (shouldCollideWithEntity) {
                                this.onCollision(hitResult);
                            } else {
                                this.setFollowedTarget(null);
                            }
                        }
                        case FALL -> {
                            if (hitResult.getType() == HitResult.Type.ENTITY) {
                                var target = ((EntityHitResult) hitResult).getEntity();
                                var reverse = ((TwoWayCollisionChecker) target).getReverseCollisionChecker();
                                if (reverse != null) {
                                    var result = reverse.apply(this);
                                    if (result == TwoWayCollisionChecker.CollisionResult.COLLIDE) {
                                        this.finishFalling();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.checkBlockCollision();

            // Travel
            if (!skipTravel) {
                this.followTarget();
                Vec3d velocity = this.getVelocity();
                double d = this.getX() + velocity.x;
                double e = this.getY() + velocity.y;
                double f = this.getZ() + velocity.z;
                ProjectileUtil.setRotationFromVelocity(this, 0.2F);

                float g = this.getDrag();
                if (this.isTouchingWater()) {
                    for(int i = 0; i < 4; ++i) {
                        float h = 0.25F;
                        this.getWorld().addParticle(ParticleTypes.BUBBLE, d - velocity.x * 0.25, e - velocity.y * 0.25, f - velocity.z * 0.25, velocity.x, velocity.y, velocity.z);
                    }
                    g = 0.8F;
                }

                var data = projectileData();
                if (data != null) {
                    if (getWorld().isClient) {
                        for (var travel_particles : data.client_data.travel_particles) {
                            ParticleHelper.play(getWorld(), this, getYaw(), getPitch(), travel_particles);
                        }
                    } else {
                        if (data.travel_sound != null && age % data.travel_sound_interval == 0) {
                            SoundHelper.playSound(getWorld(), this, data.travel_sound);
                        }
                    }
                }

                this.setPosition(d, e, f);
                this.distanceTraveled += velocity.length();
            }
        } else {
            this.discard();
        }
    }

    private void finishFalling() {
        Entity owner = this.getOwner();
        if (owner == null || owner.isRemoved()) {
            return;
        }
        if (owner instanceof LivingEntity livingEntity) {
            SpellHelper.fallImpact(livingEntity, this, this.getSpellEntry(), context.position(this.getPos()));
        }
    }

    private int followTicks = 0;
    private void followTarget() {
        var target = getFollowedTarget();
        var data = projectileData();
        if (data == null) {
            return;
        }
        var homing_angle = projectileData().homing_angle;
        if (projectileData().homing_angles != null && followTicks < projectileData().homing_angles.length) {
            homing_angle = projectileData().homing_angles[followTicks];
        }
        if (target != null && homing_angle > 0) {
            if (data.homing_after_relative_distance > 0 || data.homing_after_absolute_distance > 0) {
                var shouldFollow = distanceTraveled >= (distanceToFollow * data.homing_after_relative_distance)
                        || distanceTraveled >= data.homing_after_absolute_distance;
                if (!shouldFollow) {
                    return;
                }
            }
//            System.out.println((this.getWorld().isClient ? "Client: " : "Server: ") + "Following target: " + target + " with angle: " + homing_angle);
            var distanceVector = (target.getPos().add(0, target.getHeight() / 2F, 0))
                    .subtract(this.getPos().add(0, this.getHeight() / 2F, 0));
//            System.out.println((world.isClient ? "Client: " : "Server: ") + "Distance: " + distanceVector);
//            System.out.println((world.isClient ? "Client: " : "Server: ") + "Velocity: " + getVelocity());
            var newVelocity = VectorHelper.rotateTowards(getVelocity(), distanceVector, homing_angle);
            if (newVelocity.lengthSquared() > 0) {
//                System.out.println((world.isClient ? "Client: " : "Server: ") + "Rotated to: " + newVelocity);
                this.setVelocity(newVelocity);
                // this.velocityDirty = true;
                followTicks += 1;
            }
        }
    }

    protected float getDrag() {
        return 0.95F;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (!getWorld().isClient) {
            var target = entityHitResult.getEntity();
            if (target != null
                    && !impactHistory.contains(target.getId())
                    && this.getOwner() != null
                    && this.getOwner() instanceof LivingEntity caster) {
                setFollowedTarget(null);
                var context = this.context;
                if (context == null) {
                    context = new SpellHelper.ImpactContext();
                    var spell = this.getSpellEntry().value();
                    if (getOwner() instanceof PlayerEntity player && spell != null)  {
                        context = context.power(SpellPower.getSpellPower(spell.school, player));
                    }
                }
                if (context.power() == null) {
                    this.kill();
                    return;
                }

                var prevProjectilePos = new Vec3d(this.prevX, this.prevY, this.prevZ);
                var hitVector = entityHitResult.getPos().subtract(prevProjectilePos).normalize().multiply(this.getWidth() * 0.5F);
                var hitPosition = entityHitResult.getPos().subtract(hitVector);

                var performed = SpellHelper.projectileImpact(caster, this, target, this.getSpellEntry(), context.position(hitPosition));
                if (performed) {
                    chainReactionFrom(target);
                    if (ricochetFrom(target, caster)) {
                        return;
                    }
                    if (pierced(target)) {
                        return;
                    }
                    this.kill();
                }
            }
        }
    }

    // MARK: Perks
    protected Set<Integer> impactHistory = new HashSet<>();

    /**
     * Returns `true` if a new target is found to ricochet to
     */
    protected boolean ricochetFrom(Entity target, LivingEntity caster) {
        if (this.perks == null
                || this.perks.ricochet <= 0) {
            return false;
        }
        impactHistory.add(target.getId());

        // Find next target
        var box = this.getBoundingBox().expand(
                this.perks.ricochet_range,
                this.perks.ricochet_range,
                this.perks.ricochet_range);
        var spell = this.getSpellEntry().value();
        var intents = SpellHelper.impactIntents(spell);
        Predicate<Entity> intentMatches = (entity) -> {
            boolean intentAllows = false;
            for (var intent: intents) {
                intentAllows = intentAllows || EntityRelations.actionAllowed(SpellTarget.FocusMode.AREA, intent, caster, entity);
            }
            return intentAllows;
        };
        var otherTargets = this.getWorld().getOtherEntities(this, box, (entity) -> {
            return entity.isAttackable()
                    && entity instanceof LivingEntity // Avoid targeting unliving entities like other projectiles
                    && !impactHistory.contains(entity.getId())
                    && intentMatches.test(entity)
                    && !entity.getPos().equals(target.getPos());
        });
        if (otherTargets.isEmpty()) {
            this.setFollowedTarget(null);
            return false;
        }

        otherTargets.sort(Comparator.comparingDouble(o -> o.squaredDistanceTo(target)));

        // Set trajectory
        var newTarget = otherTargets.get(0);
        this.setPosition(target.getPos().add(0, target.getHeight() * 0.5F, 0));
        this.setFollowedTarget(newTarget);

        var distanceVector = (newTarget.getPos().add(0, newTarget.getHeight() / 2F, 0))
                .subtract(this.getPos().add(0, this.getHeight() / 2F, 0));
        var newVelocity = distanceVector.normalize().multiply(this.getVelocity().length());
        this.setVelocity(newVelocity);
        this.velocityDirty = true;

        this.perks.ricochet -= 1;
        if (this.perks.bounce_ricochet_sync) {
            this.perks.bounce -= 1;
        }
        return true;
    }

    /**
     * Returns `true` if projectile can continue to travel
     */
    private boolean pierced(Entity target) {
        if (this.perks == null
                || this.perks.pierce <= 0) {
            return false;
        }
        // Save
        impactHistory.add(target.getId());
        setFollowedTarget(null);
        this.perks.pierce -= 1;

        // Modify velocity by a tiny, non zero amount
        // to enforce velocity update on the client.
        // (Otherwise the projectile is going crazy on the client)
        var tiny = 0.01 * ((-1) * (this.perks.pierce % 2));
        this.setVelocity(this.getVelocity().multiply(1 + tiny));
        this.velocityDirty = true;

        return true;
    }

    private boolean bounceFrom(BlockHitResult blockHitResult) {
        if (this.perks == null
                || this.perks.bounce <= 0) {
            return false;
        }

        var previousPosition = getPos();
        var previousDirection = getVelocity();
        var impactPosition = blockHitResult.getPos();
        var impactSide = blockHitResult.getSide();
        var speed = getVelocity().length();

        Vec3d surfaceNormal = getSurfaceNormal(impactSide);
        Vec3d newDirection = calculateBounceVector(previousDirection, surfaceNormal);

        // Calculate the remaining distance the projectile should travel after bouncing
        double remainingDistance = previousDirection.length() - (impactPosition.subtract(previousPosition)).length();

        // Calculate the final position after the remaining distance
        Vec3d finalPosition = impactPosition.add(newDirection.normalize().multiply(remainingDistance));

        // Set the new position and velocity
        this.setPos(finalPosition.getX(), finalPosition.getY(), finalPosition.getZ());
        this.setVelocity(newDirection.multiply(speed));
        ProjectileUtil.setRotationFromVelocity(this, 0.2F);

        this.perks.bounce -= 1;
        if (this.perks.bounce_ricochet_sync) {
            this.perks.ricochet -= 1;
        }
        this.velocityDirty = true;
        this.skipTravel = true;
        return true;
    }

    public Vec3d calculateBounceVector(Vec3d previousDirection, Vec3d normal) {
        // Calculate the reflection of the incident vector with respect to the surface normal
        return previousDirection.subtract(normal.multiply(2.0 * previousDirection.dotProduct(normal)));
    }

    public Vec3d getSurfaceNormal(Direction blockSide) {
        return switch (blockSide) {
            case DOWN -> new Vec3d(0, -1, 0);
            case UP -> new Vec3d(0, 1, 0);
            case NORTH -> new Vec3d(0, 0, -1);
            case SOUTH -> new Vec3d(0, 0, 1);
            case WEST -> new Vec3d(-1, 0, 0);
            case EAST -> new Vec3d(1, 0, 0);
        };
    }
    
    private void chainReactionFrom(Entity target) {
        if (this.perks == null
                || this.perks.chain_reaction_size <= 0
                || this.perks.chain_reaction_triggers <= 0
                || impactHistory.contains(target)) {
            return;
        }
        if (getWorld().isClient) {
            return;
        }
        var position = this.getPos();
        var spawnCount = this.perks.chain_reaction_size;
        var launchVector = new Vec3d(1, 0, 0).multiply(this.getVelocity().length());
        var launchAngle = 360 / spawnCount;
        var launchAngleOffset = random.nextFloat() * launchAngle;

        this.impactHistory.add(target.getId());
        this.perks.chain_reaction_triggers -= 1;
        this.perks.chain_reaction_size += this.perks.chain_reaction_increment;

        for (int i = 0; i < spawnCount; i++) {
            var projectile = new SpellProjectile(getWorld(), (LivingEntity)this.getOwner(),
                    position.getX(), position.getY(), position.getZ(),
                    this.getBehaviour(), null, context, this.perks.copy());

            var angle = launchAngle * i + launchAngleOffset;
            projectile.setVelocity(launchVector.rotateY((float) Math.toRadians(angle)));
            projectile.range = this.range;
            ProjectileUtil.setRotationFromVelocity(projectile, 0.2F);
            projectile.impactHistory = new HashSet<>(this.impactHistory);
            getWorld().spawnEntity(projectile);
        }
    }

    // MARK: Helper

    public SpellHelper.ImpactContext getImpactContext() {
        return context;
    }

    public ItemStack getItemStackModel() {
        return itemStackModel;
    }

    // MARK: FlyingSpellEntity

    public Spell.ProjectileModel renderData() {
        var data = projectileData();
        if (data != null && data.client_data != null) {
            return data.client_data.model;
        }
        return null;
    }

    @Override
    public ItemStack getStack() {
        var data = projectileData();
        if (data != null && data.client_data != null && data.client_data.model != null) {
            return Registries.ITEM.get(Identifier.of(data.client_data.model.model_id)).getDefaultStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (bounceFrom(blockHitResult)) {
            return;
        }

        if (this.getOwner() != null
                && this.getOwner() instanceof LivingEntity caster) {
            var hitPosition = blockHitResult.getPos();
            var performed = SpellHelper.projectileImpact(caster, this, null, this.getSpellEntry(), context.position(hitPosition));
        }
        this.kill();
    }

    private Gson gson = new Gson();


    // MARK: Stored data

    public void setBehaviour(Behaviour behaviour) {
        this.getDataTracker().set(TRACKER_BEHAVIOUR, behaviour.toString());
    }
    public Behaviour getBehaviour() {
        var string = this.getDataTracker().get(TRACKER_BEHAVIOUR);
        if (string == null || string.isEmpty()) {
            return Behaviour.FLY;
        }
        return Behaviour.valueOf(string);
    }

    private RegistryEntry<Spell> spellEntry;
    public void setSpell(RegistryEntry<Spell> entry) {
        this.spellEntry = entry;
        if (!getWorld().isClient) {
            this.getDataTracker().set(TRACKER_SPELL_ID, spellId().toString());
        }
    }
    @Nullable public RegistryEntry<Spell> getSpellEntry() {
        return spellEntry;
    }
    private Identifier spellId() {
        if (spellEntry != null) {
            return spellEntry.getKey().get().getValue();
        }
        return null;
    }


    private Entity followedTarget;
    private double distanceToFollow = 0;
    public void setFollowedTarget(Entity target) {
        followedTarget = target;
        if (target != null) {
            distanceToFollow = target.distanceTo(this);
        } else {
            distanceToFollow = 0;
        }
        var id = -1;
        if (!getWorld().isClient) {
            if (target != null) {
                id = target.getId();
            }
            this.getDataTracker().set(TRACKER_TARGET_ID, id);
        }
    }

    private ItemStack itemStackModel;
    public void setItemStackModel(ItemStack itemStack) {
        var modelId = Registries.ITEM.getId(itemStack.getItem());
        this.getDataTracker().set(TRACKER_ITEM_MODEL_ID, modelId.toString());
    }
    private void updateItemModel(String idString) {
        if (idString != null && !idString.isEmpty()) {
            var id = Identifier.of(this.getDataTracker().get(TRACKER_ITEM_MODEL_ID));
            itemStackModel = Registries.ITEM.get(id).getDefaultStack();
        }
    }

    // MARK: NBT (Persistence)

    private static String NBT_BEHAVIOUR = "Behaviour";
    private static String NBT_SPELL_ID = "Spell.ID";
    private static String NBT_PERKS = "Perks";
    private static String NBT_IMPACT_CONTEXT = "Impact.Context";
    private static String NBT_ITEM_MODEL_ID = "Item.Model.ID";

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString(NBT_BEHAVIOUR, this.getBehaviour().toString());

        if (this.spellId() != null) {
            nbt.putString(NBT_SPELL_ID, this.spellId().toString());
        }
        nbt.putString(NBT_IMPACT_CONTEXT, gson.toJson(this.context));
        nbt.putString(NBT_PERKS, gson.toJson(this.perks));

        var itemModelId = getDataTracker().get(TRACKER_ITEM_MODEL_ID);
        if (!itemModelId.isEmpty()) {
            nbt.putString(NBT_ITEM_MODEL_ID, itemModelId);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains(NBT_SPELL_ID, NbtElement.STRING_TYPE)) {
            try {
                var behaviour = Behaviour.valueOf(nbt.getString(NBT_BEHAVIOUR));
                this.setBehaviour(behaviour);

                var spellId = Identifier.of(nbt.getString(NBT_SPELL_ID));
                this.setSpell(SpellRegistry.from(this.getWorld()).getEntry(spellId).orElse(null));

                this.context = gson.fromJson(nbt.getString(NBT_IMPACT_CONTEXT), SpellHelper.ImpactContext.class);
                this.perks = gson.fromJson(nbt.getString(NBT_PERKS), Spell.ProjectileData.Perks.class);

                if (nbt.contains(NBT_ITEM_MODEL_ID, NbtElement.STRING_TYPE)) {
                    updateItemModel(nbt.getString(NBT_ITEM_MODEL_ID));
                }
            } catch (Exception e) {
                System.err.println("SpellProjectile - Failed to read spell data from NBT " + e.getMessage());
            }
        }
    }

    // MARK: DataTracker (client-server sync)

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(TRACKER_SPELL_ID, "");
        builder.add(TRACKER_BEHAVIOUR, Behaviour.FLY.toString());
        builder.add(TRACKER_TARGET_ID, 0);
        builder.add(TRACKER_ITEM_MODEL_ID, "");
    }

    private static final TrackedData<String> TRACKER_SPELL_ID;
    private static final TrackedData<String> TRACKER_BEHAVIOUR;
    private static final TrackedData<Integer> TRACKER_TARGET_ID;
    private static final TrackedData<String> TRACKER_ITEM_MODEL_ID;

    static {
        TRACKER_SPELL_ID = DataTracker.registerData(SpellProjectile.class, TrackedDataHandlerRegistry.STRING);
        TRACKER_BEHAVIOUR = DataTracker.registerData(SpellProjectile.class, TrackedDataHandlerRegistry.STRING);
        TRACKER_TARGET_ID = DataTracker.registerData(SpellProjectile.class, TrackedDataHandlerRegistry.INTEGER);
        TRACKER_ITEM_MODEL_ID = DataTracker.registerData(SpellProjectile.class, TrackedDataHandlerRegistry.STRING);
    }

    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (this.getWorld().isClient) {
            if (data.equals(TRACKER_SPELL_ID)) {
                var spellId = this.getDataTracker().get(TRACKER_SPELL_ID);
                var spellEntry = SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(spellId)).orElse(null);
                this.setSpell(spellEntry);
            }
            if (data.equals(TRACKER_ITEM_MODEL_ID)) {
                updateItemModel(this.getDataTracker().get(TRACKER_ITEM_MODEL_ID));
            }
            if (data.equals(TRACKER_TARGET_ID)) {
                var id = this.getDataTracker().get(TRACKER_TARGET_ID);
                var target = id > 0 ? this.getWorld().getEntityById(id) : null;
                this.setFollowedTarget(target);
            }
        }
    }
}
