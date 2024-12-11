package net.spell_engine.mixin.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.spell_engine.api.effect.SpellStash;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StatusEffect.class)
public class StatusEffectSpellStash implements SpellStash {
    private SpellStash.Entry stashedSpell;

    @Override
    public void setStashedSpell(SpellStash.Entry spell) {
        this.stashedSpell = spell;
    }

    @Override
    public SpellStash.Entry getStashedSpell() {
        return this.stashedSpell;
    }
}
