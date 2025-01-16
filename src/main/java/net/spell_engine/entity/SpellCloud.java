package net.spell_engine.entity;

import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.particle.ParticleHelper;
import net.spell_engine.utils.SoundPlayerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SpellCloud extends Entity implements Ownable {
    public static EntityType<SpellCloud> ENTITY_TYPE;
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUuid;
    private int timeToLive;
    private Identifier spellId;
    private int dataIndex = 0;
    private SpellHelper.ImpactContext context;

    public SpellCloud(EntityType<? extends SpellCloud> entityType, World world) {
        super(entityType, world);
    }

    public SpellCloud(World world) {
        super(ENTITY_TYPE, world);
        this.noClip = true;
    }

    public void onCreatedFromSpell(Identifier spellId, Spell.Release.Target.Cloud cloudData, SpellHelper.ImpactContext context) {
        this.spellId = spellId;
        this.context = context;

        var spellEntry = getSpellEntry();
        if (spellEntry != null) {
            var spell = spellEntry.value();
            var index = -1;
            var dataList = List.of(spell.release.target.clouds);
            if (!dataList.isEmpty()) {
                index = dataList.indexOf(cloudData);
            }
            this.dataIndex = index;
        }
        this.getDataTracker().set(SPELL_ID_TRACKER, this.spellId.toString());
        this.getDataTracker().set(DATA_INDEX_TRACKER, this.dataIndex);
        this.getDataTracker().set(RADIUS_TRACKER, calculateRadius());

        this.timeToLive = (int) (cloudData.time_to_live_seconds * 20);
    }

    private float calculateRadius() {
        var cloudData = getCloudData();
        if (cloudData != null) {
            var radius = cloudData.volume.radius;
            if (context != null) {
                radius = cloudData.volume.combinedRadius(context.power());
            }
            return radius;
        } else {
            return 0F;
        }
    }

    public EntityDimensions getDimensions(EntityPose pose) {
        var cloudData = getCloudData();
        if (cloudData != null) {
            var radius = getDataTracker().get(RADIUS_TRACKER);
            var heightMultiplier = cloudData.volume.area.vertical_range_multiplier;
            return EntityDimensions.changing(radius * 2, radius * heightMultiplier);
        } else {
            return super.getDimensions(pose);
        }
    }

    // MARK: Owner

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        this.ownerUuid = owner == null ? null : owner.getUuid();
    }

    @Nullable
    @Override
    public Entity getOwner() {
        if (this.owner == null && this.ownerUuid != null && this.getWorld() instanceof ServerWorld) {
            Entity entity = ((ServerWorld)this.getWorld()).getEntity(this.ownerUuid);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity)entity;
            }
        }
        return this.owner;
    }

    // MARK: Sync

    private static final TrackedData<String> SPELL_ID_TRACKER  = DataTracker.registerData(SpellCloud.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> DATA_INDEX_TRACKER = DataTracker.registerData(SpellCloud.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> RADIUS_TRACKER = DataTracker.registerData(SpellCloud.class, TrackedDataHandlerRegistry.FLOAT);

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SPELL_ID_TRACKER, "");
        builder.add(DATA_INDEX_TRACKER, this.dataIndex);
        builder.add(RADIUS_TRACKER, 0F);
    }

    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        var rawSpellId = this.getDataTracker().get(SPELL_ID_TRACKER);
        if (rawSpellId != null && !rawSpellId.isEmpty()) {
            this.spellId = Identifier.of(rawSpellId);
        }
        this.dataIndex = this.getDataTracker().get(DATA_INDEX_TRACKER);
        this.calculateDimensions();
    }

    // MARK: Persistence

    private enum NBTKey {
        AGE("Age"),
        TIME_TO_LIVE("TTL"),
        SPELL_ID("SpellId"),
        DATA_INDEX("DataIndex")
        ;

        public final String key;
        NBTKey(String key) {
            this.key = key;
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.age = nbt.getInt(NBTKey.AGE.key);
        this.timeToLive = nbt.getInt(NBTKey.TIME_TO_LIVE.key);
        this.spellId = Identifier.of(nbt.getString(NBTKey.SPELL_ID.key));
        this.dataIndex = nbt.getInt(NBTKey.DATA_INDEX.key);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt(NBTKey.AGE.key, this.age);
        nbt.putInt(NBTKey.TIME_TO_LIVE.key, this.timeToLive);
        nbt.putString(NBTKey.SPELL_ID.key, this.spellId.toString());
        nbt.putInt(NBTKey.DATA_INDEX.key, this.dataIndex);
    }

    // MARK: Behavior

    @Override
    public boolean isSilent() {
        return false;
    }
    private boolean presenceSoundFired = false;

    public void tick() {
        super.tick();
        var cloudData = this.getCloudData();
        if (cloudData == null) {
            // this.discard();
            return;
        }
        var world = this.getWorld();
        if (world.isClient) {
            // Client side tick
            var clientData = cloudData.client_data;
            for (var particleBatch : clientData.particles) {
                ParticleHelper.play(world, this, particleBatch);
            }
            var presence_sound = cloudData.presence_sound;
            if (!presenceSoundFired && presence_sound != null) {
                var soundEvent = SoundEvent.of(Identifier.of(presence_sound.id()));
                ((SoundPlayerWorld)world).playSoundFromEntity(this, soundEvent, SoundCategory.PLAYERS,
                        presence_sound.volume(),
                        presence_sound.randomizedPitch());
                presenceSoundFired = true;
            }

        } else {
            // Server side tick
            if (this.age >= this.timeToLive) {
                this.discard();
                return;
            }
            if ((this.age % cloudData.impact_tick_interval) == 0) {
                // Impact tick due
                var area_impact = cloudData.volume;
                var owner = (LivingEntity) this.getOwner();
                var spellEntry = getSpellEntry();
                if (area_impact != null && owner != null && spellEntry != null) {
                    var spell = spellEntry.value();
                    var context = this.context;
                    if (context == null) {
                        context = new SpellHelper.ImpactContext();
                    }
                    SpellHelper.lookupAndPerformAreaImpact(area_impact, spellEntry, owner,null,
                            this, spell.impact, context.position(this.getPos()), true);
                }
            }
        }
    }

    @Nullable public Spell.Release.Target.Cloud getCloudData() {
        var spellEntry = getSpellEntry();
        if (spellEntry != null) {
            var spell = spellEntry.value();
            if (spell.release.target.clouds.length > 0) {
                return spell.release.target.clouds[dataIndex];
            } else {
                return spell.release.target.cloud;
            }
        }
        return null;
    }

    @Nullable public RegistryEntry<Spell> getSpellEntry() {
        return SpellRegistry.from(this.getWorld()).getEntry(this.spellId).orElse(null);
    }
}
