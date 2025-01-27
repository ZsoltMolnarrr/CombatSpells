package net.spell_engine.mixin.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.spell_engine.internals.spell_stash.SpellStash;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;

@Mixin(StatusEffect.class)
public class StatusEffectSpellStash implements SpellStash {
    private ArrayList<SpellStash.Entry> stashedSpells;

    @Override
    public void stashedSpell(SpellStash.Entry entry) {
        this.stashedSpells.add(entry);
    }

    @Override
    public List<Entry> getStashedSpells() {
        return this.stashedSpells;
    }
}
