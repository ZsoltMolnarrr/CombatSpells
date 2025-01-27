package net.spell_engine.internals.spell_stash;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.api.spell.Spell;

import java.util.List;

public interface SpellStash {
    record Entry(RegistryEntry<Spell> spell, Spell.Trigger trigger, int consume) { }
    void stashedSpell(Entry spell);
    List<Entry> getStashedSpells();

    static void configure(StatusEffect effect, RegistryEntry<Spell> spell, Spell.Trigger trigger, int consume) {
        ((SpellStash)effect).stashedSpell(new Entry(spell, trigger, consume));
    }
}