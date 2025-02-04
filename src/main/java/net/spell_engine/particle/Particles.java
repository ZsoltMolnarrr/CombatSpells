package net.spell_engine.particle;

import com.google.common.base.Suppliers;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.client.util.Color;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class Particles {
    private static class Helper extends SimpleParticleType {
        protected Helper(boolean alwaysShow) {
            super(alwaysShow);
        }
    }
    private static SimpleParticleType createSimple() {
        return new Helper(false);
    }

    public static class ParticleEntry {
        public final Identifier id;
        public final SimpleParticleType particleType = createSimple();
        public boolean usesCustomTexture = false;
        public ParticleEntry(String name) {
            this.id =  Identifier.of(SpellEngineMod.ID, name);
        }
        public ParticleEntry customTexture() {
            this.usesCustomTexture = true;
            return this;
        }
    }

    private static final ArrayList<ParticleEntry> all = new ArrayList<>();
    public static List<ParticleEntry> all() {
        return all;
    }

    private static ParticleEntry particle(String name) {
        var entry = new ParticleEntry(name);
        all.add(entry);
        return entry;
    }


    public static final List<MagicParticleFamily> MAGIC_FAMILIES = new ArrayList<>();
    public static MagicParticleFamily addMagicFamily(MagicParticleFamily family) {
        MAGIC_FAMILIES.add(family);
        return family;
    }
    public record MagicParticleFamily(String name, Color color) {
        public enum Shape { SPELL, IMPACT, SPARK, STRIPE }
        public enum Motion { FLOAT, ASCEND, DECELERATE, BURST }
        public List<Variant> variants() {
            var variants = new ArrayList<Variant>();
            for(var motion: Motion.values()) {
                for(var shape: Shape.values()) {
                    variants.add(new Variant(name, color, shape, motion));
                }
            }
            return variants;
        }
        public static final String prefix = "magic";
        public record Variant(String family, Color color, Shape shape, Motion motion, SimpleParticleType particleType) {
            public Variant(String name, Color color, Shape shape, Motion motion) {
                this(name, color, shape, motion, createSimple());
            }
            public Identifier id() {
                return Identifier.of(SpellEngineMod.ID, name());
            }
            public String name() {
                return String.format("%s_%s_%s_%s", prefix,
                        family.toLowerCase(Locale.ENGLISH),
                        shape.toString().toLowerCase(Locale.ENGLISH),
                        motion.toString().toLowerCase(Locale.ENGLISH));
            }
            public boolean requiresCustomTexture() {
                switch (shape) {
                    case IMPACT, SPARK, STRIPE -> {
                        return false;
                    }
                }
                return false;
            }
        }
    }
    public static final MagicParticleFamily ARCANE = addMagicFamily(new MagicParticleFamily("arcane", Color.from(SpellSchools.ARCANE.color)));
    public static final MagicParticleFamily HOLY = addMagicFamily(new MagicParticleFamily("holy", Color.HOLY));
    public static final MagicParticleFamily NATURE = addMagicFamily(new MagicParticleFamily("nature", Color.NATURE));
    public static final MagicParticleFamily FROST = addMagicFamily(new MagicParticleFamily("frost", Color.FROST));
    public static final MagicParticleFamily RAGE = addMagicFamily(new MagicParticleFamily("rage", Color.RAGE));
    public static final MagicParticleFamily WHITE = addMagicFamily(new MagicParticleFamily("white", Color.WHITE));
    public static final Supplier<List<MagicParticleFamily.Variant>> MAGIC_FAMILY_VARIANTS = Suppliers.memoize(() -> {
        var variants = new ArrayList<MagicParticleFamily.Variant>();
        for(var family: MAGIC_FAMILIES) {
            variants.addAll(family.variants());
        }
        return variants;
    });

    //    public static final ParticleEntry arcane_spark = new ParticleEntry("arcane_spark");
    public static final ParticleEntry arcane_spell = particle("arcane_spell");
    public static final ParticleEntry arcane_hit = particle("arcane_hit").customTexture();
    public static final ParticleEntry healing_ascend = particle("healing_ascend").customTexture();
    public static final ParticleEntry holy_ascend = particle("holy_ascend").customTexture();
    public static final ParticleEntry holy_hit = particle("holy_hit").customTexture();
    public static final ParticleEntry holy_spark = particle("holy_spark");
    public static final ParticleEntry holy_spark_mini = particle("holy_spark_mini");
    public static final ParticleEntry nature_spark_mini = particle("nature_spark_mini");
    public static final ParticleEntry nature_spark_mini_slowing = particle("nature_spark_mini_slowing");
    public static final ParticleEntry white_spark_mini = particle("white_spark_mini");
    public static final ParticleEntry holy_spell = particle("holy_spell");
    public static final ParticleEntry fire_explosion = particle("fire_explosion").customTexture();
    public static final ParticleEntry flame = particle("flame");
    public static final ParticleEntry flame_spark = particle("flame_spark").customTexture();
    public static final ParticleEntry flame_ground = particle("flame_ground").customTexture();
    public static final ParticleEntry flame_medium_a = particle("flame_medium_a").customTexture();
    public static final ParticleEntry flame_medium_b = particle("flame_medium_b").customTexture();
    public static final ParticleEntry frost_hit = particle("frost_hit").customTexture();
    public static final ParticleEntry frost_shard = particle("frost_shard").customTexture();
    public static final ParticleEntry snowflake = particle("snowflake");
    public static final ParticleEntry dripping_blood = particle("dripping_blood");
    public static final ParticleEntry roots = particle("roots").customTexture();
    public static final ParticleEntry electric_arc_A = particle("electric_arc_a").customTexture();
    public static final ParticleEntry electric_arc_B = particle("electric_arc_b").customTexture();
    public static final ParticleEntry smoke_medium = particle("smoke_medium").customTexture();
    public static final ParticleEntry weakness_smoke = particle("weakness_smoke").customTexture();
    public static final ParticleEntry buff_rage = particle("buff_rage").customTexture();
    public static final ParticleEntry sign_charge = particle("sign_charge").customTexture();

    public static void register() {
        for(var entry: all) {
            Registry.register(Registries.PARTICLE_TYPE, entry.id, entry.particleType);
        }
        for (var variant: MAGIC_FAMILY_VARIANTS.get()) {
            Registry.register(Registries.PARTICLE_TYPE, variant.id(), variant.particleType());
        }
    }
}