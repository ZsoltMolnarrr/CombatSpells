package net.spell_engine.internals.target;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.TargetHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class SpellTarget {
    public enum Intent {
        HELPFUL, HARMFUL
    }
    public enum FocusMode {
        DIRECT, AREA
    }

    public record SearchResult(List<Entity> entities, @Nullable Vec3d location) {
        public static SearchResult empty() {
            return new SearchResult(List.of(), null);
        }
        public static SearchResult of(List<Entity> entities) {
            return new SearchResult(entities, null);
        }
        public static SearchResult of(Entity entity) {
            return new SearchResult(List.of(entity), null);
        }
        public static SearchResult of(Vec3d location) {
            return new SearchResult(List.of(), location);
        }
    }

    public static SearchResult findTargets(PlayerEntity caster, Spell currentSpell, SearchResult previous) {
        List<Entity> targets = List.of();
        var previousTargets = previous.entities;
        Vec3d location = null;
        if (currentSpell == null || currentSpell.impact == null) {
            return new SearchResult(targets, location);
        }
        boolean fallbackToPreviousTargets = false;
        var focusMode = SpellHelper.focusMode(currentSpell);
        var targetType = currentSpell.target.type;
        var range = SpellHelper.getRange(caster, currentSpell) * caster.getScale();

        Predicate<Entity> selectionPredicate = (target) -> {
            var deliveryIntent = SpellHelper.deliveryIntent(currentSpell);
            boolean intentAllows = deliveryIntent.isPresent()
                    ? EntityRelations.actionAllowed(focusMode, deliveryIntent.get(), caster, target)
                    : false;
            for (var impact: currentSpell.impact) {
                var intent = SpellHelper.impactIntent(impact.action);
                var newValue = impact.action.apply_to_caster
                        ? target == caster
                        : EntityRelations.actionAllowed(focusMode, intent, caster, target);
                intentAllows = intentAllows || newValue;
            }
            return !SpellEngineClient.config.filterInvalidTargets || intentAllows;
        };

        switch (targetType) {
            case NONE -> {
            }
            case CASTER -> {
                targets = List.of(caster);
            }
            case CURSOR -> {
                fallbackToPreviousTargets = currentSpell.target.cursor.sticky;
                var target = TargetHelper.targetFromRaycast(caster, range, selectionPredicate);
                if (target != null) {
                    targets = List.of(target);
                } else {
                    targets = List.of();
                }
            }
            case BEAM -> {
                targets = TargetHelper.targetsFromRaycast(caster, range, selectionPredicate);
            }
            case AREA -> {
                targets = TargetHelper.targetsFromArea(caster, range, currentSpell.target.area, selectionPredicate);
                var area = currentSpell.target.area;
                if (area != null && area.include_caster) {
                    targets.add(caster);
                }
            }
        }

        if (fallbackToPreviousTargets && targets.isEmpty()) {
            targets = previousTargets.stream()
                    .filter(entity -> {
                        return TargetHelper.isInLineOfSight(caster, entity) && !entity.isRemoved();
                    })
                    .toList();
        }

        var cursor = currentSpell.target.cursor;
        if (cursor != null) {
            if (cursor.use_caster_as_fallback && targets.isEmpty()) {
                targets = List.of(caster);
            }
            /// If no targets are found, use cursor location for meteor style spells
            if (!cursor.required && targets.isEmpty()) {
                location = TargetHelper.locationFromRayCast(caster, range);
            }
        }

        return new SearchResult(targets, location);
    }
}
