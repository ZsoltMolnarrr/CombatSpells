package net.spell_engine.api.spell;

import net.minecraft.util.Rarity;
import net.spell_engine.api.render.LightEmission;
import net.spell_engine.api.spell.fx.ParticleBatch;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import org.jetbrains.annotations.Nullable;

public class Spell {
    // Structure
    public SpellSchool school;
    public float range = 50;
    public boolean range_melee = false;

    // An arbitrary group to group spells by
    // Spells with the same group override each other, prioritized by tier
    // For primary spells (such as hard bound spells of weapons, and first spells of spell books) the recommended group is "primary"
    @Nullable public String group;
    // The rank of the spell, used to determine which spell to use when multiple spells with the same `group` are available
    public int rank = 1;
    public Learn learn = new Learn();
    public static class Learn { public Learn() {}
        /// Whether the spell can be obtained from Spell Binding Table
        public boolean enabled = true;
        public int tier = 1;
        public int level_cost_per_tier = 3;
        public int level_requirement_per_tier = 10;
    }

    public Type type = Type.ACTIVE;
    public enum Type { ACTIVE, PASSIVE }

    public Active active;
    public static class Active {
        public Scroll scroll = new Scroll();
        public static class Scroll { public Scroll() {}
            public boolean generate = true;
            /// Cost of experience levels to apply the scroll
            public int apply_cost_base = 0;
            public int level_cost_per_tier = 1;
            public int level_requirement_per_tier = 0;
            @Nullable public Rarity custom_rarity = null;
        }

        public Cast cast = new Cast();
        public static class Cast { public Cast() { }
            public boolean haste_affected = true;
            public float duration = 0;
            public int channel_ticks = 0;
            public String animation;
            public boolean animation_pitch = true;
            public boolean animates_ranged_weapon = false;
            /// Default `0.2` matches the same as movement speed during vanilla item usage (such as bow)"
            public float movement_speed = 0.2F;
            public Sound start_sound;
            public Sound sound;
            public ParticleBatch[] particles = new ParticleBatch[]{};
        }
    }

    public Passive passive;
    public static class Passive {
        public Trigger trigger = new Trigger();
    }

    public Release release = new Release();
    public static class Release { public Release() { }
        public String animation;
        public ParticleBatch[] particles;
        public Sound sound;
    }

    public Target target = new Target();
    public static class Target {
        public Type type = Type.CASTER;
        public enum Type {
            NONE, CASTER, CURSOR, BEAM, AREA, FROM_TRIGGER
        }
        // The number of maximum targets, applied when greater than zero
        public int cap = 0;

        public Cursor cursor;
        public static class Cursor { public Cursor() { }
            /// Whether an entity must be targeted to cast the spell
            public boolean required = false;
            /// Whether the spell casting process keeps an entity that was targeted already
            public boolean sticky = false;
            /// Whether the spell casting process uses the caster as a fallback target
            public boolean use_caster_as_fallback = false;
        }

        public Beam beam;
        public static class Beam { public Beam() { }
            public enum Luminance { LOW, MEDIUM, HIGH }
            public Beam.Luminance luminance = Beam.Luminance.HIGH;
            public String texture_id = "textures/entity/beacon_beam.png";
            public long color_rgba = 0xFFFFFFFFL;
            public long inner_color_rgba = 0xFFFFFFFFL;
            public float width = 0.1F;
            public float flow = 1;
            public ParticleBatch[] block_hit_particles = new ParticleBatch[]{};
        }

        public Area area;
        public static class Area { public Area() { }
            public enum DropoffCurve { NONE, SQUARED }
            public DropoffCurve distance_dropoff = DropoffCurve.NONE;
            public float horizontal_range_multiplier = 1F;
            public float vertical_range_multiplier = 1F;
            public float angle_degrees = 0F;
            public boolean include_caster = false;
        }
    }

    public Delivery deliver = new Delivery();
    public static class Delivery {
        public Type type = Type.DIRECT;
        public enum Type {
            DIRECT, PROJECTILE, METEOR, CLOUD, SHOOT_ARROW, STASH_EFFECT, CUSTOM
        }

        public ShootProjectile projectile;
        public static class ShootProjectile {
            public boolean inherit_shooter_velocity = false;
            public static class DirectionOffset { public float yaw = 0; public float pitch = 0; }
            public ShootProjectile.DirectionOffset[] direction_offsets;
            public boolean direction_offsets_require_target = false;
            /// Turns the projectile immediately towards the target
            public boolean direct_towards_target = false;
            /// Launch properties of the spell projectile
            public LaunchProperties launch_properties = new LaunchProperties();
            /// The projectile to be launched
            public ProjectileData projectile;
        }

        public Meteor meteor;
        public static class Meteor { public Meteor() { }
            /// How high the falling projectile is launched from compared to the position of the target
            public float launch_height = 10;
            public int offset_requires_sequence = 1;
            public int divergence_requires_sequence = 1;
            public int follow_target_requires_sequence = -1;
            /// How far horizontally the falling projectile is launched from the target
            public float launch_radius = 0;
            /// Launch properties of the falling projectile
            public LaunchProperties launch_properties = new LaunchProperties();
            /// The projectile to be launched
            public ProjectileData projectile;
        }

        public ShootArrow shoot_arrow;
        public static class ShootArrow { public ShootArrow() { }
            public boolean consume_arrow = true;
            public float divergence = 5F;
            public boolean arrow_critical_strike = true;
            /// Launch properties of the arrow
            /// (vanilla default velocity for crossbows is 3.15)
            public LaunchProperties launch_properties = new LaunchProperties().velocity(3.15F);
        }

        public Cloud cloud;
        public Cloud[] clouds = new Cloud[]{};
        public static class Cloud { public Cloud() { }
            // Custom entity type id to spawn, must be a subclass of `SpellCloud`
            @Nullable public String entity_type_id;
            public AreaImpact volume = new AreaImpact();
            public float time_to_live_seconds = 0;

            /// The number of ticks between looking for targets and trying to apply impact
            public int impact_tick_interval = 5;
            public int delay_ticks = 0;
            public EntityPlacement placement = new EntityPlacement();
            @Nullable public Sound presence_sound;
            public Cloud.ClientData client_data = new Cloud.ClientData();
            public static class ClientData {
                public int light_level = 0;
                public ParticleBatch[] particles = new ParticleBatch[]{};
                public ProjectileModel model;
            }
            public Cloud.Spawn spawn = new Cloud.Spawn();
            public static class Spawn {
                public Sound sound;
                public ParticleBatch[] particles = new ParticleBatch[]{};
            }
        }

        public StashEffect stash_effect;
        public static class StashEffect {
            /// Spells with valid `stash_effect` get automatically linked
            /// to the status effect specified below.
            /// No java code required.

            /// ID of the status effect, that will stash this spell.
            public String id;
            /// Stacks to apply (-1)
            public int amplifier = 0;
            /// Duration of the status effect in seconds
            public float duration = 10;
            public boolean show_particles = false;

            /// Trigger of the status effect
            public Trigger trigger = new Trigger();
            /// Status effect stacks to consume upon triggering
            public int consume = 1;
            /// Determines what happens to the impacts of the spell when using this stash
            public ImpactMode impact_mode = ImpactMode.PERFORM;
            public enum ImpactMode {
                PERFORM,    /// Perform the impacts, on the target that is available at the time of triggering
                TRANSFER    /// Pass the impacts onto a projectile, that will be launched at the time of triggering
            }
        }

        public Custom custom;
        public static class Custom { public Custom() { }
            public String handler;
        }
    }

    public Impact[] impact;
    public static class Impact { public Impact() { }
        public Action action;
        /// Magic school of this specific impact, if null then spell school is used
        @Nullable public SpellSchool school;
        public TargetCondition[] target_conditions = new TargetCondition[]{};
        public static class TargetCondition {
            // If true = AND, if false = OR
            public boolean all_required = false;
            @Nullable public String entity_type;
            @Nullable public String has_effect;
            // @Nullable public String predicate ?

            public boolean allow_action = true;
            @Nullable public Modifier modifier;
        }
        public static class Modifier {
            // Combined as `ADD_MULTIPLIED_BASE` in `EntityAttributeModifier.Operation`
            public float power_multiplier = 0;
            // Combined as `ADD_VALUE` in `EntityAttributeModifier.Operation`
            public float critical_chance_bonus = 0;
            // Combined as `ADD_VALUE` in `EntityAttributeModifier.Operation`
            public float critical_damage_bonus = 0;
        }
        public static class Action { public Action() { }
            public Type type;
            public boolean apply_to_caster = false;
            public float min_power = 1;
            public enum Type {
                DAMAGE, HEAL, STATUS_EFFECT, FIRE, SPAWN, TELEPORT, CUSTOM
            }
            public Damage damage;
            public static class Damage { public Damage() { }
                public boolean bypass_iframes = true;
                public float spell_power_coefficient = 1;
                public float knockback = 1;
            }
            public Heal heal;
            public static class Heal { public Heal() { }
                public float spell_power_coefficient = 1;
            }
            public StatusEffect status_effect;
            public static class StatusEffect { public StatusEffect() { }
                public String effect_id;
                public float duration = 10;
                public int amplifier = 0;
                public float amplifier_power_multiplier = 0;
                public ApplyMode apply_mode = ApplyMode.SET;
                public enum ApplyMode { SET, ADD, REMOVE }
                @Nullable public ApplyLimit apply_limit;
                public static class ApplyLimit { public ApplyLimit() { }
                    public float health_base = 0;
                    public float spell_power_multiplier = 0;
                }
                public boolean show_particles = true;
            }

            public Fire fire;
            public static class Fire { public Fire() { }
                // Entity.java - Notice `% 20` - tick offset is used to avoid instant hits
                // if (this.fireTicks % 20 == 0 && !this.isInLava()) {
                //    this.damage(DamageSource.ON_FIRE, 1.0f);
                // }
                public int duration = 2;
                public int tick_offset = 10;
            }

            // Populate either `spawn` or `spawns` but not both
            public Spawn spawn;
            public Spawn[] spawns = new Spawn[]{};
            public static class Spawn {
                public String entity_type_id;
                public int time_to_live_seconds = 0;
                public int delay_ticks = 0;
                public EntityPlacement placement = new EntityPlacement();
            }

            public Teleport teleport;
            public static class Teleport { public Teleport() { }
                public enum Mode { FORWARD, BEHIND_TARGET }
                public Mode mode;
                public int required_clearance_block_y = 1;
                public SpellTarget.Intent intent = SpellTarget.Intent.HELPFUL;
                public Forward forward;
                public static class Forward { public Forward() { }
                    public float distance = 10;
                }
                public BehindTarget behind_target;
                public static class BehindTarget { public BehindTarget() { }
                    public float distance = 1.5F;
                }
                @Nullable public ParticleBatch[] depart_particles;
                @Nullable public ParticleBatch[] arrive_particles;
            }

            public Custom custom;
            public static class Custom { public Custom() { }
                public SpellTarget.Intent intent = SpellTarget.Intent.HELPFUL;
                public String handler;
            }
        }

        public ParticleBatch[] particles = new ParticleBatch[]{};
        public Sound sound;
    }
    /// Apply this impact to other entities nearby
    @Nullable public AreaImpact area_impact;

    @Nullable public ArrowPerks arrow_perks = null;
    public static class ArrowPerks { public ArrowPerks() { }
        public float damage_multiplier = 1F;
        public float velocity_multiplier = 1F;
        public boolean bypass_iframes = false;
        public int iframe_to_set = 0;
        public boolean skip_arrow_damage = false;
        public int pierce = 0;
        public float knockback = 1;
        @Nullable public ParticleBatch[] travel_particles;
        @Nullable public ProjectileModel override_render;
    }

    /// Applied to the caster, once the spell casting process finishes
    public Cost cost = new Cost();
    public static class Cost { public Cost() { }
        /// Exhaust to add
        public float exhaust = 0.1F;
        /// Durability of the spell host item to consume
        public int durability = 1;
        /// Status effect to remove
        /// (Useful for channeled spells)
        @Nullable public String effect_id;

        public Cooldown cooldown = new Cooldown();
        public static class Cooldown {
            /// Duration of the cooldown in seconds
            public float duration = 0;
            /// Whether the duration to be multiplied by channeling duration
            public boolean proportional = false;
            /// Whether the cooldown is affected by haste
            public boolean haste_affected = true;
            /// Whether item cooldown is imposed onto the hosting item of this spell
            public boolean hosting_item = true;
        }

        @Nullable public Item item;
        public static class Item {
            /// ID or Tag
            /// (When using tags, make sure to have a translation for tha tag)
            public String id;
            /// How many of the item is consumed
            public int amount = 1;
            /// When set to false, spell cast attempt will check availability,
            /// but upon successful cast will not be consumed
            /// (Useful for archery skills)
            public boolean consume = true;
        }
    }

    // MARK: Shared structures (used from multiple places in the spell structure)

    public static class Trigger {
        public enum Type {
            ARROW_SHOT, ARROW_IMPACT,
            MELEE_IMPACT,
            SPELL_IMPACT_ANY, SPELL_IMPACT_SPECIFIC,
            DAMAGE_TAKEN,
            ROLL  /// Only works when Combat Roll mod is installed
        }
        public Type type;
        /// Chance to trigger. 0 = 0%, 1 = 100%
        public float chance = 1;

        public enum TargetSelector { CASTER, AOE_SOURCE, TARGET }
        @Nullable public TargetSelector target_override;
        @Nullable public TargetSelector aoe_source_override;

        public SpellImpact spell_impact;
        public static class SpellImpact { public SpellImpact() { }
            // Spell school regex
            @Nullable public String school;
            // ID or tag to match the spell
            @Nullable public String id;
            // Impact type regex
            @Nullable public String impact_type;
            // Maybe add predicate, that can be registered in java, and resolved by this id
            // public String spell_predicate
        }
    }

    public static class AreaImpact { public AreaImpact() { }
        public float radius = 1F;
        public ExtraRadius extra_radius = new ExtraRadius();
        public static class ExtraRadius {
            public float power_coefficient = 0;
            public float power_cap = 0;
        }
        public Target.Area area = new Target.Area();
        public ParticleBatch[] particles = new ParticleBatch[]{};
        @Nullable
        public Sound sound;

        public float combinedRadius(SpellPower.Result power) {
            return radius + extra_radius.power_coefficient * (float) Math.min(extra_radius.power_cap, power.baseValue());
        }
    }

    public static class LaunchProperties { public LaunchProperties() { }
        /// Initial velocity of the projectile
        public float velocity = 1F;
        /// How many additional projectiles are spawned after launch
        public int extra_launch_count = 0;
        /// How many ticks after launch additional projectiles are spawned
        public int extra_launch_delay = 2;
        /// The sound to play on launch
        @Nullable public Sound sound;

        public LaunchProperties velocity(float value) {
            this.velocity = value;
            return this;
        }
        public LaunchProperties copy() {
            LaunchProperties copy = new LaunchProperties();
            copy.velocity = this.velocity;
            copy.extra_launch_count = this.extra_launch_count;
            copy.extra_launch_delay = this.extra_launch_delay;
            copy.sound = this.sound != null ? this.sound.copy() : null;
            return copy;
        }
    }

    public static class ProjectileData { public ProjectileData() { }
        public float divergence = 0;
        public float homing_angle = 1F;
        @Nullable public float[] homing_angles = null;
        public float homing_after_absolute_distance = 0;
        public float homing_after_relative_distance = 0;
        /// The frequency of playing the travel sound in ticks
        public int travel_sound_interval = 20;
        @Nullable public Sound travel_sound;

        public Perks perks = new Perks();
        public static class Perks { Perks() { }
            /// How many entities projectile can ricochet to
            public int ricochet = 0;
            /// How far ricochet can look for a target
            public float ricochet_range = 5;
            /// How many times projectile can bounce off a wall
            public int bounce = 0;
            /// Whether ricochet and bounce should be decremented together
            public boolean bounce_ricochet_sync = true;
            /// How many entities projectile can go through
            public int pierce = 0;
            /// How many additional projectiles are spawned on impact
            public int chain_reaction_size = 0;
            /// How many generation of chain reaction projectiles are spawned
            public int chain_reaction_triggers = 1;
            /// How many more projectiles are spawned from chain reaction of a spawned projectile
            public int chain_reaction_increment = -1;

            public Perks copy() {
                Perks copy = new Perks();
                copy.ricochet = this.ricochet;
                copy.ricochet_range = this.ricochet_range;
                copy.bounce = this.bounce;
                copy.bounce_ricochet_sync = this.bounce_ricochet_sync;
                copy.pierce = this.pierce;
                copy.chain_reaction_size = this.chain_reaction_size;
                copy.chain_reaction_triggers = this.chain_reaction_triggers;
                copy.chain_reaction_increment = this.chain_reaction_increment;
                return copy;
            }
        }

        public Client client_data;
        public static class Client { public Client() { }
            /// Ambient light level of the projectile, like players holding torches
            /// Requires `LambDynamicLights` to be installed
            /// Example values:
            /// 14 - torch
            /// 10 - soul torch
            public int light_level = 0;
            public ParticleBatch[] travel_particles;
            public ProjectileModel model;
        }
    }

    public static class ProjectileModel { public ProjectileModel() { }
        public boolean use_held_item = false;
        public String model_id;
        public LightEmission light_emission = LightEmission.GLOW;
        public float scale = 1F;
        public float rotate_degrees_per_tick = 2F;
        public float rotate_degrees_offset = 0;
        public Orientation orientation = Orientation.TOWARDS_MOTION;
        public enum Orientation {
            TOWARDS_CAMERA, TOWARDS_MOTION, ALONG_MOTION
        }
    }

    public static class EntityPlacement { public EntityPlacement() { }
        // If greater than 0, the entity will be placed at the caster's look direction, by this many blocks
        public boolean force_onto_ground = true;
        public float location_offset_by_look = 0;
        public float location_yaw_offset = 0;
        public boolean apply_yaw = false;
        public boolean apply_pitch = false;
        public float location_offset_x = 0;
        public float location_offset_y = 0;
        public float location_offset_z = 0;
    }
}