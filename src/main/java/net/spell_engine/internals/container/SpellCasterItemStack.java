package net.spell_engine.internals.container;

import net.spell_engine.api.spell.SpellContainer;
import org.jetbrains.annotations.Nullable;

@Deprecated
public interface SpellCasterItemStack {
    @Nullable
    SpellContainer getSpellContainer();
}
