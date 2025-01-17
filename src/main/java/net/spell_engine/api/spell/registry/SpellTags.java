package net.spell_engine.api.spell.registry;

import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.Spell;

public class SpellTags {
    private static TagKey<Spell> tag(String name) {
        return TagKey.of(SpellRegistry.KEY, Identifier.of(SpellEngineMod.ID, name));
    }

    /**
     * Spells that can be found in loot chests applied onto spell scrolls.
     */
    public static final TagKey<Spell> TREASURE = tag("treasure");
}
