package net.spell_engine.internals.casting;

import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellInfo;
import net.spell_engine.internals.SpellCooldownManager;
import org.jetbrains.annotations.Nullable;

public interface SpellCasterEntity {
    SpellCooldownManager getCooldownManager();

    void setSpellCastProcess(@Nullable SpellCast.Process process);
    @Nullable SpellCast.Process getSpellCastProcess();

    Spell getCurrentSpell(); // Used by Better Combat compatibility
    float getCurrentCastingSpeed();

    // Used for Archery
    void setTemporaryActiveSpell(@Nullable SpellInfo spellInfo);
    @Nullable SpellInfo getTemporaryActiveSpell();

    boolean isBeaming();
    @Nullable
    Spell.Release.Target.Beam getBeam();

    default boolean isCastingSpell() {
        return getSpellCastProcess() != null;
    }
}
