package net.spell_engine.internals.casting;

import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellCooldownManager;
import org.jetbrains.annotations.Nullable;

public interface SpellCasterEntity {
    SpellCooldownManager getCooldownManager();

    void setChannelTickIndex(int channelTickIndex);
    int getChannelTickIndex();

    void setSpellCastProcess(@Nullable SpellCast.Process process);
    @Nullable SpellCast.Process getSpellCastProcess();

    Spell getCurrentSpell(); // Used by Better Combat compatibility
    float getCurrentCastingSpeed();

    // Used for Archery
    void setTemporaryActiveSpell(@Nullable RegistryEntry<Spell> spellEntry);
    @Nullable RegistryEntry<Spell> getTemporaryActiveSpell();

    boolean isBeaming();
    @Nullable
    Spell.Target.Beam getBeam();

    default boolean isCastingSpell() {
        return getSpellCastProcess() != null;
    }
}
