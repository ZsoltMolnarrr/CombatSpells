package net.spell_engine.api.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.utils.PatternMatching;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;

public class SpellEntityPredicates {
    public record Input(Entity entity, Entity other, @Nullable String param) { }
    public record Entry(Identifier id, Predicate<Input> predicate) { }

    private static final Map<String, Entry> entries = Map.of();
    public static Entry register(Identifier id, Predicate<Input> predicate) {
        var entry = new Entry(id, predicate);
        entries.put(id.toString(), entry);
        return entry;
    }
    public static Entry get(Identifier id) {
        return entries.get(id.toString());
    }
    public static Entry get(String idString) {
        return entries.get(idString);
    }

    public static Entry TYPE_MATCHES = register(Identifier.of(SpellEngineMod.ID, "type_matches"), args -> {
        if (args.param == null) {
            return true;
        }
        return PatternMatching.matches(args.entity.getType().getRegistryEntry(), RegistryKeys.ENTITY_TYPE, args.param);
    });

    public static Entry HAS_EFFECT = register(Identifier.of(SpellEngineMod.ID, "has_effect"), args -> {
        if (args.param == null) {
            return true;
        }
        if (args.entity instanceof LivingEntity livingEntity) {
            var id = Identifier.of(args.param);
            var effect = Registries.STATUS_EFFECT.getEntry(id);
            if (effect.isEmpty()) {
                return false;
            }
            return livingEntity.hasStatusEffect(effect.get());
        }
        return false;
    });

    public static Entry HAS_BAD_EFFECT = register(Identifier.of(SpellEngineMod.ID, "has_bad_effect"), args -> {
        if (args.entity instanceof LivingEntity livingEntity) {
            for (var instance: livingEntity.getStatusEffects()) {
                var effect = instance.getEffectType().value();
                if (!effect.isBeneficial()) {
                    return true;
                }
            }
        }
        return false;
    });

    public static Entry HAS_GOOD_EFFECT = register(Identifier.of(SpellEngineMod.ID, "has_good_effect"), args -> {
        if (args.entity instanceof LivingEntity livingEntity) {
            for (var instance : livingEntity.getStatusEffects()) {
                var effect = instance.getEffectType().value();
                if (effect.isBeneficial()) {
                    return true;
                }
            }
        }
        return false;
    });

    public static Entry IS_ON_FIRE = register(Identifier.of(SpellEngineMod.ID, "is_on_fire"), args -> {
        return args.entity.isOnFire();
    });
}
