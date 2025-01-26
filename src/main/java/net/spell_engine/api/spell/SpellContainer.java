package net.spell_engine.api.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SpellContainer(ContentType content, boolean is_proxy, String pool, String slot, int max_spell_count, List<String> spell_ids) {
    public enum ContentType {
        MAGIC, ARCHERY;
        public static Codec<ContentType> CODEC = Codec.STRING.xmap(ContentType::valueOf, ContentType::name);
    }

    public static final Codec<SpellContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ContentType.CODEC.optionalFieldOf("content", ContentType.MAGIC).forGetter(x -> x.content),
            Codec.BOOL.optionalFieldOf("is_proxy", false).forGetter(x -> x.is_proxy),
            Codec.STRING.optionalFieldOf("pool", "").forGetter(x -> x.pool),
            Codec.STRING.optionalFieldOf("slot", "").forGetter(x -> x.slot),
            Codec.INT.optionalFieldOf("max_spell_count", 0).forGetter(x -> x.max_spell_count),
            Codec.STRING.listOf().optionalFieldOf("spell_ids", List.of()).forGetter(x -> x.spell_ids)
    ).apply(instance, SpellContainer::new));

    public static final SpellContainer EMPTY = new SpellContainer(ContentType.MAGIC, false, "", 0, List.of());

    // Canonical constructor with default values, to avoid null values
    public SpellContainer(ContentType content, boolean is_proxy, String pool, int max_spell_count, List<String> spell_ids) {
        this(content, is_proxy, pool, "", max_spell_count, spell_ids);
    }

    public SpellContainer(ContentType content, boolean is_proxy, String pool, String slot, int max_spell_count, List<String> spell_ids) {
        this.content = content != null ? content : ContentType.MAGIC;
        this.is_proxy = is_proxy;
        this.pool = pool != null ? pool : "";
        this.slot = slot != null ? slot : "";
        this.max_spell_count = max_spell_count;
        this.spell_ids = spell_ids != null ? spell_ids : List.of();
    }

    // MARK: Helpers

    public int cappedIndex(int selected) {
        if (spell_ids.isEmpty()) { return 0; }
        var remainder = selected % spell_ids.size();
        return (remainder >= 0) ? remainder : (remainder + spell_ids.size());
    }

    public String spellId(int selected) {
        if (spell_ids == null || spell_ids.isEmpty()) {
            return null;
        }
        var index = cappedIndex(selected);
        return spell_ids.get(index);
    }

    public boolean isValid() {
        if (is_proxy) {
            return true;
        }
        if (max_spell_count < 0) {
            return false;
        }
        return !spell_ids.isEmpty() || (pool != null && !pool.isEmpty());
    }

    public boolean contains(Identifier spellId) {
        return spell_ids.contains(spellId.toString());
    }

    public boolean isUsable() {
        return isValid() && !spell_ids.isEmpty();
    }

    public SpellContainer copy() {
        return new SpellContainer(content, is_proxy, pool, max_spell_count, new ArrayList<>(spell_ids));
    }

    public SpellContainer copyWith(List<String> spell_ids) {
        return new SpellContainer(content, is_proxy, pool, max_spell_count, spell_ids);
    }
}