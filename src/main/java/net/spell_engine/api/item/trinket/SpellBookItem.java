package net.spell_engine.api.item.trinket;

import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.Identifier;
import net.spell_engine.api.item.SpellEngineItemTags;

public interface SpellBookItem extends ItemConvertible {
    Identifier getPoolId();

    static boolean isSpellBook(Item item) {
        return item instanceof SpellBookItem || item.getRegistryEntry().isIn(SpellEngineItemTags.SPELL_BOOK);
    }
}
