package net.spell_engine.api.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpellSlot(String id) {
    public static final Codec<SpellSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("id", "").forGetter(x -> x.id)
    ).apply(instance, SpellSlot::new));
}
