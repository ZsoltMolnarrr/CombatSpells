package net.spell_engine.fx;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.item.trinket.SpellBookItem;
import net.spell_engine.spellbinding.SpellBindingScreenHandler;

import java.util.ArrayList;
import java.util.List;

public class SpellEngineSounds {
    public record Entry(Identifier id, SoundEvent soundEvent) {
        public Entry(String name) {
            this(Identifier.of(SpellEngineMod.ID, name));
        }
        public Entry(Identifier id) {
            this(id, SoundEvent.of(id));
        }
        public Entry travelDistance(float distance) {
            return new Entry(id, SoundEvent.of(id, distance));
        }
    }
    public static final List<Entry> entries = new ArrayList<>();
    public static Entry add(Entry entry) {
        entries.add(entry);
        return entry;
    }

    // MARK: Generic spell sounds

    public static final Entry GENERIC_ARCANE_CASTING = add(new Entry("generic_arcane_casting"));
    public static final Entry GENERIC_ARCANE_RELEASE = add(new Entry("generic_arcane_release"));

    public static final Entry GENERIC_FIRE_CASTING = add(new Entry("generic_fire_casting"));
    public static final Entry GENERIC_FIRE_RELEASE = add(new Entry("generic_fire_release"));

    public static final Entry GENERIC_FROST_CASTING = add(new Entry("generic_frost_casting"));
    public static final Entry GENERIC_FROST_RELEASE = add(new Entry("generic_frost_release"));
    public static final Entry GENERIC_FROST_IMPACT = add(new Entry("generic_frost_impact"));

    public static final Entry GENERIC_HEALING_CASTING = add(new Entry("generic_healing_casting"));
    public static final Entry GENERIC_HEALING_RELEASE = add(new Entry("generic_healing_release"));
    public static final Entry GENERIC_HEALING_IMPACT_1 = add(new Entry("generic_healing_impact_1"));
    public static final Entry GENERIC_HEALING_IMPACT_2 = add(new Entry("generic_healing_impact_2"));

    public static final Entry GENERIC_LIGHTNING_CASTING = add(new Entry("generic_lightning_casting"));
    public static final Entry GENERIC_LIGHTNING_RELEASE = add(new Entry("generic_lightning_release"));

    public static final Entry GENERIC_SOUL_CASTING = add(new Entry("generic_soul_casting"));
    public static final Entry GENERIC_SOUL_RELEASE = add(new Entry("generic_soul_release"));
    public static final Entry GENERIC_WIND_CHARGING = add(new Entry("generic_wind_charging"));

    // MARK: Spell binding sounds

    public static final Entry BIND_SPELL = add(new Entry("bind_spell"));

    // MARK: Item sounds

    public static final Entry SPELLBOOK_EQUIP = add(new Entry("spellbook_equip"));

    public static void register() {
        for (var entry: entries) {
            Registry.register(Registries.SOUND_EVENT, entry.id(), entry.soundEvent());
        }
    }
}
