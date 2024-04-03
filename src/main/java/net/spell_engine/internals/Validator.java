package net.spell_engine.internals;

import net.spell_engine.api.spell.Spell;

public class Validator {
    public static void validate(Spell spell) throws Exception {
        if (spell == null) {
            throw new Exception("Spell is null");
        }
        if (spell.school == null) {
            throw new Exception("Spell school is null");
        }
    }
}
