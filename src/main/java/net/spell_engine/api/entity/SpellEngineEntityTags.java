package net.spell_engine.api.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;

public class SpellEngineEntityTags {
    public static final TagKey<EntityType<?>> bosses = TagKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of(SpellEngineMod.ID, "bosses"));
    public static final TagKey<EntityType<?>> mechanical = TagKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of(SpellEngineMod.ID, "mechanical"));
}
