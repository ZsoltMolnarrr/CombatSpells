package net.spell_engine.api.spell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public class SpellRegistry_V2 {
    /**
     * Using vanilla name space on purpose!
     * So spell data file path looks like this:
     * `data/MOD/spell/SPELL.json`
     * instead of this:
     * `data/MOD/spell_engine/spell/SPELL.json`
     */
    public static final Identifier ID = Identifier.ofVanilla("spells");
    public static final RegistryKey<Registry<Spell>> KEY = RegistryKey.ofRegistry(ID);
    public static Registry<Spell> from(World world) {
        return world.getRegistryManager().get(KEY);
    }

    private static final Gson gson = new GsonBuilder().create();
    public static final Codec<Spell> CODEC = Codecs.exceptionCatching(Codecs.JSON_ELEMENT.xmap(
            json -> {
                return gson.fromJson(json, Spell.class);
            },
            spell -> {
                JsonElement jsonElement = gson.toJsonTree(spell);
                return jsonElement;
            }
    ));

    public static RegistryEntryList.Named<Spell> find(World world, Identifier tagId) {
        var manager = world.getRegistryManager();
        var lookup = manager.createRegistryLookup().getOrThrow(KEY); // RegistryEntryLookup<Spell>
        var tag = TagKey.of(KEY, tagId);
        return lookup.getOrThrow(tag);
    }


    public static List<RegistryEntry<Spell>> entries(World world, @Nullable Identifier id) {
        return find(world, id).stream().toList();
    }

    public static List<RegistryEntry<Spell>> entries(World world, @Nullable String pool) {
        if (pool == null || pool.isEmpty()) {
            return List.of();
        }
        var id = Identifier.of(pool);
        return entries(world, id);
    }

    public static Stream<RegistryEntry.Reference<Spell>> stream(World world) {
        var manager = world.getRegistryManager();
        var registry = manager.get(KEY);
        return registry.streamEntries();
    }
}