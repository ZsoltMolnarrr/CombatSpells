package net.spell_engine.internals.arrow;

import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.spell.Spell;
import org.jetbrains.annotations.Nullable;

public interface ArrowExtension {
    void applyArrowPerks(RegistryEntry<Spell> spellEntry);
    @Nullable RegistryEntry<Spell> getCarriedSpell();
    boolean isInGround_SpellEngine();
    void allowByPassingIFrames_SpellEngine(boolean allow);
}
