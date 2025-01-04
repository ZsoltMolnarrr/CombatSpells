package net.spell_engine.api.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;

public class SpellEngineItemTags {
    public static final TagKey<Item> SPELL_BOOK = TagKey.of(Registries.ITEM.getKey(), Identifier.of(SpellEngineMod.ID, "spell_book"));
}
