package net.spell_engine.api.tags;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;

public class SpellEngineItemTags {
    /**
     * Items those are considered spell books.
     * Allows equipping in the spell book slot.
     */
    public static final TagKey<Item> SPELL_BOOK = TagKey.of(Registries.ITEM.getKey(), Identifier.of(SpellEngineMod.ID, "spell_books"));

    /**
     * Items those are considered non-craftable spell books.
     * Spell Binding Table will not offer these for creation.
     */
    public static final TagKey<Item> NON_CRAFTABLE_SPELL_BOOK = TagKey.of(Registries.ITEM.getKey(), Identifier.of(SpellEngineMod.ID, "non_craftable_spell_books"));

    /**
     * Items that can be merged (placed) into spell books, to add new spells.
     * (Example: Spell Scroll)
     */
    public static final TagKey<Item> SPELL_BOOK_MERGEABLE = TagKey.of(Registries.ITEM.getKey(), Identifier.of(SpellEngineMod.ID, "spell_book_mergeable"));
}
