package net.spell_engine.api.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.util.Identifier;

public interface SpellStash {
    record Entry(Identifier id, int consumed) { }
    void setStashedSpell(Entry spell);
    Entry getStashedSpell();

    static void configure(StatusEffect effect, Identifier spellId, int consumed) {
        ((SpellStash)effect).setStashedSpell(new Entry(spellId, consumed));
    }
}